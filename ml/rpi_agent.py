#!/usr/bin/env python3
"""
MachinaEar Raspberry Pi Agent
This script runs on the Raspberry Pi to:
1. Handle WiFi provisioning (AP mode if no network)
2. Pair with a user account via pairing code
3. Record audio and run anomaly detection locally
4. Sync results to the backend (with offline buffering)
"""

import os
import sys
import json
import time
import uuid
import sqlite3
import signal
import threading
import subprocess
from pathlib import Path
from datetime import datetime
from dataclasses import dataclass
from typing import Optional, Callable

import numpy as np
import requests

# Audio processing
try:
    import sounddevice as sd
except ImportError:
    print("sounddevice not installed. Run: pip install sounddevice")
    sd = None

try:
    import librosa
except ImportError:
    print("librosa not installed. Run: pip install librosa")
    librosa = None

# ML model
try:
    import torch
    from models.cnn_autoencoder import CNNAutoEncoder
except Exception as e:
    print(f"PyTorch not available ({e}). Model inference will use mock mode.")
    torch = None
    CNNAutoEncoder = None

# Flask for provisioning portal
try:
    from flask import Flask, request, render_template_string, redirect
except ImportError:
    Flask = None


# =============================================================================
# Configuration
# =============================================================================

@dataclass
class Config:
    # Backend API
    api_base_url: str = "https://iam.machinaear.me/iam"
    
    # Device info
    device_name: str = "MachinaEar-Pi"
    
    # Audio settings
    sample_rate: int = 16000
    chunk_duration: float = 1.0  # seconds
    
    # ML settings
    n_mels: int = 128
    n_fft: int = 1024
    hop_length: int = 512
    anomaly_threshold: float = 0.1  # MSE threshold for anomaly
    
    # Paths
    data_dir: Path = Path.home() / ".machinaear"
    db_path: Path = Path.home() / ".machinaear" / "events.db"
    config_path: Path = Path.home() / ".machinaear" / "config.json"
    model_path: Optional[Path] = None  # Set to trained model checkpoint
    
    # WiFi AP settings
    ap_ssid: str = "MachinaEar-Setup"
    ap_password: str = "machinaear123"
    
    # Sync settings
    sync_interval: float = 30.0  # seconds
    heartbeat_interval: float = 60.0  # seconds


config = Config()


# =============================================================================
# Database for offline storage
# =============================================================================

def init_database():
    """Initialize SQLite database for offline event storage."""
    config.data_dir.mkdir(parents=True, exist_ok=True)
    conn = sqlite3.connect(str(config.db_path))
    cursor = conn.cursor()
    
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS events (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            timestamp TEXT NOT NULL,
            anomaly_score REAL NOT NULL,
            is_anomaly INTEGER NOT NULL,
            synced INTEGER DEFAULT 0,
            created_at TEXT DEFAULT CURRENT_TIMESTAMP
        )
    ''')
    
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS device_config (
            key TEXT PRIMARY KEY,
            value TEXT
        )
    ''')
    
    conn.commit()
    conn.close()


def save_event(timestamp: str, anomaly_score: float, is_anomaly: bool):
    """Save an event to local database."""
    conn = sqlite3.connect(str(config.db_path))
    cursor = conn.cursor()
    cursor.execute(
        'INSERT INTO events (timestamp, anomaly_score, is_anomaly) VALUES (?, ?, ?)',
        (timestamp, anomaly_score, int(is_anomaly))
    )
    conn.commit()
    conn.close()


def get_unsynced_events():
    """Get events that haven't been synced to backend."""
    conn = sqlite3.connect(str(config.db_path))
    cursor = conn.cursor()
    cursor.execute('SELECT id, timestamp, anomaly_score, is_anomaly FROM events WHERE synced = 0')
    events = cursor.fetchall()
    conn.close()
    return events


def mark_events_synced(event_ids: list):
    """Mark events as synced."""
    if not event_ids:
        return
    conn = sqlite3.connect(str(config.db_path))
    cursor = conn.cursor()
    cursor.executemany('UPDATE events SET synced = 1 WHERE id = ?', [(id,) for id in event_ids])
    conn.commit()
    conn.close()


def get_device_config(key: str) -> Optional[str]:
    """Get a config value from database."""
    conn = sqlite3.connect(str(config.db_path))
    cursor = conn.cursor()
    cursor.execute('SELECT value FROM device_config WHERE key = ?', (key,))
    row = cursor.fetchone()
    conn.close()
    return row[0] if row else None


def set_device_config(key: str, value: str):
    """Set a config value in database."""
    conn = sqlite3.connect(str(config.db_path))
    cursor = conn.cursor()
    cursor.execute('INSERT OR REPLACE INTO device_config (key, value) VALUES (?, ?)', (key, value))
    conn.commit()
    conn.close()


# =============================================================================
# Network utilities
# =============================================================================

def get_mac_address() -> str:
    """Get the device's MAC address."""
    try:
        # Try to get from network interface
        import netifaces
        for iface in netifaces.interfaces():
            addrs = netifaces.ifaddresses(iface)
            if netifaces.AF_LINK in addrs:
                mac = addrs[netifaces.AF_LINK][0].get('addr')
                if mac and mac != '00:00:00:00:00:00':
                    return mac.upper()
    except ImportError:
        pass
    
    # Fallback: generate a pseudo-MAC from machine ID
    machine_id = uuid.getnode()
    return ':'.join(f'{(machine_id >> i) & 0xff:02x}' for i in range(0, 48, 8)).upper()


def check_internet_connection() -> bool:
    """Check if we have internet connectivity."""
    try:
        requests.get("https://www.google.com", timeout=5)
        return True
    except:
        return False


def get_hostname() -> str:
    """Get the device hostname."""
    import socket
    return socket.gethostname()


# =============================================================================
# WiFi Provisioning (AP Mode)
# =============================================================================

def start_ap_mode():
    """Start WiFi Access Point for provisioning (Linux/Raspberry Pi only)."""
    if sys.platform != 'linux':
        print("[MOCK] Would start AP mode on Linux. Running in mock mode.")
        return False
    
    try:
        # This requires hostapd and dnsmasq installed
        # sudo apt install hostapd dnsmasq
        print(f"Starting AP mode: {config.ap_ssid}")
        
        # Create hostapd config
        hostapd_conf = f"""
interface=wlan0
driver=nl80211
ssid={config.ap_ssid}
hw_mode=g
channel=7
wmm_enabled=0
macaddr_acl=0
auth_algs=1
ignore_broadcast_ssid=0
wpa=2
wpa_passphrase={config.ap_password}
wpa_key_mgmt=WPA-PSK
rsn_pairwise=CCMP
"""
        # In production, write this to /etc/hostapd/hostapd.conf and start the service
        print("[INFO] AP mode configuration ready")
        return True
    except Exception as e:
        print(f"[ERROR] Failed to start AP mode: {e}")
        return False


def run_provisioning_server():
    """Run a simple web server for WiFi provisioning."""
    if Flask is None:
        print("[ERROR] Flask not installed. Cannot run provisioning server.")
        return
    
    app = Flask(__name__)
    
    PROVISION_PAGE = '''
    <!DOCTYPE html>
    <html>
    <head>
        <title>MachinaEar Setup</title>
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <style>
            body { font-family: Arial, sans-serif; max-width: 400px; margin: 50px auto; padding: 20px; background: #1a1a2e; color: white; }
            h1 { color: #6c63ff; }
            input { width: 100%; padding: 12px; margin: 10px 0; border: none; border-radius: 8px; }
            button { width: 100%; padding: 15px; background: #6c63ff; color: white; border: none; border-radius: 8px; cursor: pointer; font-size: 16px; }
            button:hover { background: #5a52d5; }
            .pairing-code { background: #2d2d44; padding: 20px; border-radius: 10px; text-align: center; margin-top: 20px; }
            .pairing-code h2 { color: #6c63ff; font-size: 32px; letter-spacing: 5px; }
        </style>
    </head>
    <body>
        <h1>🎧 MachinaEar Setup</h1>
        <form method="POST" action="/configure">
            <label>WiFi Network Name (SSID)</label>
            <input type="text" name="ssid" required>
            <label>WiFi Password</label>
            <input type="password" name="password" required>
            <button type="submit">Connect</button>
        </form>
        <div class="pairing-code">
            <p>Your Pairing Code:</p>
            <h2>{{ pairing_code }}</h2>
            <p>Enter this code in the MachinaEar app to pair this device.</p>
        </div>
    </body>
    </html>
    '''
    
    @app.route('/')
    def index():
        pairing_code = get_device_config('pairing_code') or generate_pairing_code()
        return render_template_string(PROVISION_PAGE, pairing_code=pairing_code)
    
    @app.route('/configure', methods=['POST'])
    def configure():
        ssid = request.form.get('ssid')
        password = request.form.get('password')
        
        if ssid and password:
            # Save WiFi config
            set_device_config('wifi_ssid', ssid)
            set_device_config('wifi_password', password)
            
            # On real Pi, would configure wpa_supplicant here
            print(f"[INFO] WiFi configured: {ssid}")
            
            return "<h1>✅ WiFi configured! Device will restart...</h1>"
        
        return redirect('/')
    
    # Run on port 80 (or 8080 if not root)
    try:
        is_root = os.geteuid() == 0
    except AttributeError:
        # Windows doesn't have geteuid
        is_root = False
    port = 80 if is_root else 8080
    print(f"[INFO] Provisioning server running at http://192.168.4.1:{port}")
    app.run(host='0.0.0.0', port=port, debug=False)


# =============================================================================
# Backend API Client
# =============================================================================

class APIClient:
    def __init__(self, base_url: str):
        self.base_url = base_url.rstrip('/')
        self.device_token = None
        self.device_id = None
    
    def register_pending(self, pairing_code: str, mac: str, hostname: str) -> dict:
        """Register device for pairing."""
        try:
            resp = requests.post(
                f"{self.base_url}/device-registration/register-pending",
                json={"pairingCode": pairing_code, "mac": mac, "hostname": hostname},
                timeout=10
            )
            if resp.status_code in (200, 202):
                return resp.json()
            print(f"[WARN] Registration failed: {resp.status_code} - {resp.text}")
        except Exception as e:
            print(f"[ERROR] Registration request failed: {e}")
        return {}
    
    def check_pairing(self, pairing_code: str) -> Optional[dict]:
        """Check if pairing is complete."""
        try:
            resp = requests.get(
                f"{self.base_url}/device-registration/check-pairing/{pairing_code}",
                timeout=10
            )
            if resp.status_code == 200:
                data = resp.json()
                if data.get('isPaired'):
                    return data
            return None
        except Exception as e:
            print(f"[ERROR] Check pairing failed: {e}")
        return None
    
    def send_status_update(self, device_id: str, status: str, anomaly_score: float, 
                           cpu_usage: float = 0, memory_usage: float = 0, temperature: float = 0):
        """Send device status update to backend."""
        if not self.device_token:
            return False
        
        try:
            resp = requests.patch(
                f"{self.base_url}/devices/{device_id}/status",
                json={
                    "status": status,
                    "anomalyScore": anomaly_score,
                    "cpuUsage": cpu_usage,
                    "memoryUsage": memory_usage,
                    "temperature": temperature
                },
                headers={"Authorization": f"Bearer {self.device_token}"},
                timeout=10
            )
            return resp.status_code == 200
        except Exception as e:
            print(f"[ERROR] Status update failed: {e}")
        return False


# =============================================================================
# Audio Processing & ML Inference
# =============================================================================

class AnomalyDetector:
    def __init__(self, model_path: Optional[Path] = None):
        self.model = None
        self.device = torch.device('cpu') if torch else None
        
        if torch and CNNAutoEncoder:
            self.model = CNNAutoEncoder(z_dim=40)
            if model_path and model_path.exists():
                checkpoint = torch.load(model_path, map_location=self.device)
                if 'state_dict' in checkpoint:
                    self.model.load_state_dict(checkpoint['state_dict'])
                else:
                    self.model.load_state_dict(checkpoint)
                print(f"[INFO] Loaded model from {model_path}")
            else:
                print("[WARN] No model checkpoint found. Using random weights (for testing).")
            self.model.eval()
    
    def preprocess_audio(self, audio: np.ndarray, sr: int) -> Optional[np.ndarray]:
        """Convert audio to mel spectrogram."""
        if librosa is None:
            return None
        
        try:
            mel_spec = librosa.feature.melspectrogram(
                y=audio,
                sr=sr,
                n_fft=config.n_fft,
                hop_length=config.hop_length,
                n_mels=config.n_mels,
                power=2.0
            )
            log_mel = 20.0 / 2.0 * np.log10(mel_spec + sys.float_info.epsilon)
            
            # Normalize to 0-1
            log_mel = (log_mel - log_mel.min()) / (log_mel.max() - log_mel.min() + 1e-8)
            
            # Create square patches (n_mels x n_mels)
            n_frames = log_mel.shape[1]
            if n_frames < config.n_mels:
                # Pad if too short
                log_mel = np.pad(log_mel, ((0, 0), (0, config.n_mels - n_frames)))
            
            # Take center crop
            start = (log_mel.shape[1] - config.n_mels) // 2
            patch = log_mel[:, start:start + config.n_mels]
            
            return patch.astype(np.float32)
        except Exception as e:
            print(f"[ERROR] Preprocessing failed: {e}")
            return None
    
    def detect(self, audio: np.ndarray, sr: int) -> tuple[float, bool]:
        """
        Run anomaly detection on audio.
        Returns: (anomaly_score, is_anomaly)
        """
        if self.model is None or torch is None:
            # Return random score for testing
            score = np.random.random() * 0.2
            return score, score > config.anomaly_threshold
        
        mel_patch = self.preprocess_audio(audio, sr)
        if mel_patch is None:
            return 0.0, False
        
        try:
            with torch.no_grad():
                # Add batch and channel dimensions: (1, 1, n_mels, n_mels)
                x = torch.tensor(mel_patch).unsqueeze(0).unsqueeze(0)
                reconstructed = self.model(x)
                
                # Calculate MSE
                mse = torch.mean((x - reconstructed) ** 2).item()
                is_anomaly = mse > config.anomaly_threshold
                
                return mse, is_anomaly
        except Exception as e:
            print(f"[ERROR] Inference failed: {e}")
            return 0.0, False


# =============================================================================
# Main Agent
# =============================================================================

def generate_pairing_code() -> str:
    """Generate a 6-character pairing code."""
    import random
    import string
    code = ''.join(random.choices(string.ascii_uppercase + string.digits, k=6))
    set_device_config('pairing_code', code)
    return code


class MachinaEarAgent:
    def __init__(self):
        self.running = False
        self.api_client = APIClient(config.api_base_url)
        self.detector = AnomalyDetector(config.model_path)
        self.pairing_code = None
        self.device_id = None
        self.is_paired = False
        
        # Initialize database
        init_database()
        
        # Load saved state
        self.load_state()
    
    def load_state(self):
        """Load saved device state from database."""
        self.device_id = get_device_config('device_id')
        token = get_device_config('device_token')
        if token:
            self.api_client.device_token = token
            self.api_client.device_id = self.device_id
            self.is_paired = True
            print(f"[INFO] Loaded paired device: {self.device_id}")
    
    def save_state(self, device_id: str, token: str):
        """Save device state to database."""
        set_device_config('device_id', device_id)
        set_device_config('device_token', token)
        self.device_id = device_id
        self.api_client.device_token = token
        self.api_client.device_id = device_id
        self.is_paired = True
    
    def start_pairing(self):
        """Start the pairing process."""
        self.pairing_code = get_device_config('pairing_code') or generate_pairing_code()
        mac = get_mac_address()
        hostname = get_hostname()
        
        print(f"\n{'='*50}")
        print(f"  PAIRING CODE: {self.pairing_code}")
        print(f"{'='*50}")
        print(f"  MAC: {mac}")
        print(f"  Hostname: {hostname}")
        print(f"{'='*50}\n")
        
        # Register with backend
        result = self.api_client.register_pending(self.pairing_code, mac, hostname)
        if result:
            print("[INFO] Device registered for pairing. Waiting for user to complete pairing...")
        
        return self.pairing_code
    
    def poll_pairing(self) -> bool:
        """Poll backend to check if pairing is complete."""
        if not self.pairing_code:
            return False
        
        result = self.api_client.check_pairing(self.pairing_code)
        if result and result.get('isPaired'):
            print("[INFO] Pairing complete!")
            self.save_state(result.get('id'), result.get('deviceToken'))
            return True
        
        return False
    
    def sync_events(self):
        """Sync offline events to backend."""
        if not self.is_paired:
            return
        
        events = get_unsynced_events()
        if not events:
            return
        
        print(f"[INFO] Syncing {len(events)} events...")
        synced_ids = []
        
        for event_id, timestamp, score, is_anomaly in events:
            status = "abnormal" if is_anomaly else "normal"
            if self.api_client.send_status_update(self.device_id, status, score):
                synced_ids.append(event_id)
        
        mark_events_synced(synced_ids)
        print(f"[INFO] Synced {len(synced_ids)} events")
    
    def record_and_analyze(self):
        """Record audio and run anomaly detection."""
        if sd is None:
            # Mock mode for testing
            score = np.random.random() * 0.2
            is_anomaly = score > config.anomaly_threshold
            return score, is_anomaly
        
        try:
            # Record audio
            duration = config.chunk_duration
            sr = config.sample_rate
            audio = sd.rec(int(duration * sr), samplerate=sr, channels=1, dtype='float32')
            sd.wait()
            audio = audio.flatten()
            
            # Run detection
            score, is_anomaly = self.detector.detect(audio, sr)
            return score, is_anomaly
        except Exception as e:
            print(f"[ERROR] Recording failed: {e}")
            return 0.0, False
    
    def run(self):
        """Main agent loop."""
        print("\n" + "="*60)
        print("  MachinaEar Raspberry Pi Agent")
        print("="*60 + "\n")
        
        # Check internet connectivity
        has_internet = check_internet_connection()
        
        if not has_internet:
            print("[WARN] No internet connection detected.")
            print("[INFO] Please configure WiFi or run in offline mode.")
            
            # In production, would start AP mode here
            # start_ap_mode()
            # run_provisioning_server()
            
            # For now, just continue in offline mode
            print("[INFO] Running in offline mode. Events will be buffered locally.")
        
        # Start pairing if not already paired
        if not self.is_paired:
            self.start_pairing()
            
            # Poll for pairing completion
            print("[INFO] Waiting for pairing...")
            while not self.is_paired and self.running:
                if self.poll_pairing():
                    break
                time.sleep(5)
        
        # Main detection loop
        self.running = True
        last_sync = time.time()
        last_heartbeat = time.time()
        
        print("\n[INFO] Starting audio monitoring...")
        
        while self.running:
            try:
                # Record and analyze
                score, is_anomaly = self.record_and_analyze()
                timestamp = datetime.now().isoformat()
                
                # Log result
                status = "[!] ANOMALY" if is_anomaly else "[OK] Normal"
                print(f"[{timestamp}] {status} (score: {score:.4f})")
                
                # Save event locally
                save_event(timestamp, score, is_anomaly)
                
                # Try to send real-time update if connected
                if self.is_paired and check_internet_connection():
                    status_str = "abnormal" if is_anomaly else "normal"
                    self.api_client.send_status_update(self.device_id, status_str, score)
                
                # Periodic sync of offline events
                if time.time() - last_sync > config.sync_interval:
                    if check_internet_connection():
                        self.sync_events()
                    last_sync = time.time()
                
                # Heartbeat
                if time.time() - last_heartbeat > config.heartbeat_interval:
                    if self.is_paired and check_internet_connection():
                        self.api_client.send_status_update(self.device_id, "normal", 0)
                    last_heartbeat = time.time()
                
                # Small delay between recordings
                time.sleep(0.5)
                
            except KeyboardInterrupt:
                break
            except Exception as e:
                print(f"[ERROR] {e}")
                time.sleep(1)
        
        print("\n[INFO] Agent stopped.")
    
    def stop(self):
        """Stop the agent."""
        self.running = False


def main():
    agent = MachinaEarAgent()
    
    # Handle Ctrl+C gracefully
    def signal_handler(sig, frame):
        print("\n[INFO] Shutting down...")
        agent.stop()
        sys.exit(0)
    
    signal.signal(signal.SIGINT, signal_handler)
    
    # Run the agent
    agent.running = True
    agent.run()


if __name__ == "__main__":
    main()
