#!/usr/bin/env python3
"""
MachinaEar Raspberry Pi Agent - VMA309 GPIO Version
For use with VMA309 digital sound sensor module
"""

import os
import sys
import time
import uuid
import sqlite3
import signal
import threading
from pathlib import Path
from datetime import datetime
from dataclasses import dataclass
from typing import Optional

import requests

# GPIO for VMA309 sensor
try:
    import RPi.GPIO as GPIO
    HAS_GPIO = True
except ImportError:
    print("[WARN] RPi.GPIO not available. Running in mock mode.")
    HAS_GPIO = False


# =============================================================================
# Configuration
# =============================================================================

@dataclass
class Config:
    # Backend API
    api_base_url: str = "https://iam.machinaear.me/iam"
    
    # Device info
    device_name: str = "MachinaEar-Pi"
    
    # VMA309 GPIO settings
    sound_sensor_pin: int = 4  # GPIO pin for VMA309 D0
    
    # Detection settings
    sound_window: float = 5.0  # Time window to count sounds (seconds)
    anomaly_threshold: int = 5  # Number of sounds in window to trigger anomaly
    
    # Paths
    data_dir: Path = Path.home() / ".machinaear"
    db_path: Path = Path.home() / ".machinaear" / "events.db"
    
    # Sync settings
    sync_interval: float = 30.0  # seconds
    heartbeat_interval: float = 60.0  # seconds
    status_update_interval: float = 10.0  # seconds


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
            sound_count INTEGER NOT NULL,
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


def save_event(timestamp: str, sound_count: int, is_anomaly: bool):
    """Save an event to local database."""
    conn = sqlite3.connect(str(config.db_path))
    cursor = conn.cursor()
    cursor.execute(
        'INSERT INTO events (timestamp, sound_count, is_anomaly) VALUES (?, ?, ?)',
        (timestamp, sound_count, int(is_anomaly))
    )
    conn.commit()
    conn.close()


def get_unsynced_events():
    """Get events that haven't been synced to backend."""
    conn = sqlite3.connect(str(config.db_path))
    cursor = conn.cursor()
    cursor.execute('SELECT id, timestamp, sound_count, is_anomaly FROM events WHERE synced = 0')
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
    try:
        conn = sqlite3.connect(str(config.db_path))
        cursor = conn.cursor()
        cursor.execute('SELECT value FROM device_config WHERE key = ?', (key,))
        row = cursor.fetchone()
        conn.close()
        return row[0] if row else None
    except:
        return None


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
    
    def send_status_update(self, device_id: str, status: str, sound_count: int = 0):
        """Send device status update to backend."""
        try:
            # Convert sound_count to a normalized score (0-1)
            anomaly_score = min(1.0, sound_count / 10.0)
            
            resp = requests.patch(
                f"{self.base_url}/devices/{device_id}/status",
                json={
                    "status": status,
                    "anomalyScore": anomaly_score
                },
                headers={"Authorization": f"Bearer {self.device_token}"} if self.device_token else {},
                timeout=10
            )
            return resp.status_code == 200
        except Exception as e:
            print(f"[ERROR] Status update failed: {e}")
        return False


# =============================================================================
# VMA309 Sound Sensor
# =============================================================================

class VMA309Sensor:
    def __init__(self, pin: int = 4):
        self.pin = pin
        self.sound_events = []  # List of timestamps when sound was detected
        self.lock = threading.Lock()
        
        if HAS_GPIO:
            GPIO.setmode(GPIO.BCM)
            GPIO.setup(self.pin, GPIO.IN, pull_up_down=GPIO.PUD_DOWN)
            GPIO.add_event_detect(
                self.pin, 
                GPIO.RISING, 
                callback=self._on_sound_detected, 
                bouncetime=200
            )
            print(f"[INFO] VMA309 sensor initialized on GPIO {self.pin}")
        else:
            print("[INFO] Running in mock mode (no GPIO)")
    
    def _on_sound_detected(self, channel):
        """Callback when sound is detected."""
        with self.lock:
            self.sound_events.append(time.time())
            print("Sound detected!")
    
    def get_sound_count(self, window_seconds: float = 5.0) -> int:
        """Get number of sounds in the last N seconds."""
        cutoff = time.time() - window_seconds
        
        with self.lock:
            # Remove old events
            self.sound_events = [t for t in self.sound_events if t > cutoff]
            return len(self.sound_events)
    
    def cleanup(self):
        """Clean up GPIO."""
        if HAS_GPIO:
            GPIO.cleanup()


# =============================================================================
# Pairing Code Generator
# =============================================================================

def generate_pairing_code() -> str:
    """Generate a 6-character pairing code."""
    import random
    import string
    code = ''.join(random.choices(string.ascii_uppercase + string.digits, k=6))
    set_device_config('pairing_code', code)
    return code


# =============================================================================
# Main Agent
# =============================================================================

class MachinaEarAgent:
    def __init__(self):
        self.running = False
        self.api_client = APIClient(config.api_base_url)
        self.sensor = VMA309Sensor(config.sound_sensor_pin)
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
        
        print("")
        print("=" * 50)
        print("  PAIRING CODE: " + self.pairing_code)
        print("=" * 50)
        print(f"  MAC: {mac}")
        print(f"  Hostname: {hostname}")
        print("=" * 50)
        print("")
        print("  Go to https://machinaear.me/devices")
        print("  Click 'Pair Device' and enter the code above")
        print("")
        
        # Register with backend
        result = self.api_client.register_pending(self.pairing_code, mac, hostname)
        if result:
            print("[INFO] Device registered. Waiting for pairing...")
        else:
            print("[WARN] Could not register with backend. Will retry...")
        
        return self.pairing_code
    
    def poll_pairing(self) -> bool:
        """Poll backend to check if pairing is complete."""
        if not self.pairing_code:
            return False
        
        result = self.api_client.check_pairing(self.pairing_code)
        if result and result.get('isPaired'):
            print("[INFO] Pairing complete!")
            self.save_state(result.get('id'), result.get('deviceToken', ''))
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
        
        for event_id, timestamp, sound_count, is_anomaly in events:
            status = "abnormal" if is_anomaly else "normal"
            if self.api_client.send_status_update(self.device_id, status, sound_count):
                synced_ids.append(event_id)
        
        mark_events_synced(synced_ids)
        if synced_ids:
            print(f"[INFO] Synced {len(synced_ids)} events")
    
    def run(self):
        """Main agent loop."""
        print("")
        print("=" * 60)
        print("  MachinaEar Raspberry Pi Agent (VMA309)")
        print("=" * 60)
        print("")
        
        # Check internet connectivity
        has_internet = check_internet_connection()
        
        if not has_internet:
            print("[WARN] No internet connection detected.")
            print("[INFO] Running in offline mode. Events will be buffered.")
        else:
            print("[INFO] Internet connection OK")
        
        # Start pairing if not already paired
        if not self.is_paired:
            self.start_pairing()
            
            # Poll for pairing completion
            while not self.is_paired and self.running:
                if check_internet_connection():
                    if self.poll_pairing():
                        break
                time.sleep(5)
        
        if not self.running:
            return
        
        # Main detection loop
        last_sync = time.time()
        last_heartbeat = time.time()
        last_status_update = time.time()
        
        print("")
        print("[INFO] Starting sound monitoring...")
        print(f"[INFO] Anomaly threshold: {config.anomaly_threshold} sounds in {config.sound_window}s")
        print("")
        
        while self.running:
            try:
                # Get sound count in the window
                sound_count = self.sensor.get_sound_count(config.sound_window)
                is_anomaly = sound_count >= config.anomaly_threshold
                timestamp = datetime.now().isoformat()
                
                # Only log significant events
                if sound_count > 0:
                    status = "[!] ANOMALY" if is_anomaly else "[OK] Normal"
                    print(f"[{timestamp}] {status} - {sound_count} sounds detected")
                    
                    # Save event locally
                    save_event(timestamp, sound_count, is_anomaly)
                
                # Periodic status update (even if normal)
                if time.time() - last_status_update > config.status_update_interval:
                    if self.is_paired and check_internet_connection():
                        status_str = "abnormal" if is_anomaly else "normal"
                        self.api_client.send_status_update(self.device_id, status_str, sound_count)
                    last_status_update = time.time()
                
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
                
                # Small delay
                time.sleep(1)
                
            except KeyboardInterrupt:
                break
            except Exception as e:
                print(f"[ERROR] {e}")
                time.sleep(1)
        
        print("")
        print("[INFO] Agent stopped.")
        self.sensor.cleanup()
    
    def stop(self):
        """Stop the agent."""
        self.running = False


def main():
    agent = MachinaEarAgent()
    
    # Handle Ctrl+C gracefully
    def signal_handler(sig, frame):
        print("")
        print("[INFO] Shutting down...")
        agent.stop()
        sys.exit(0)
    
    signal.signal(signal.SIGINT, signal_handler)
    
    # Run the agent
    agent.running = True
    agent.run()


if __name__ == "__main__":
    main()
