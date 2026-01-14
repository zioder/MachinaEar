#!/usr/bin/env python3
"""MachinaEar VMA309 Agent - Simple Polling Version"""
import time, requests, sqlite3, uuid, signal, sys
from pathlib import Path
import RPi.GPIO as GPIO

API_URL = "https://iam.machinaear.me/iam"
SENSOR_PIN = 4
SOUND_WINDOW = 5.0
ANOMALY_THRESHOLD = 3

# Check for --reset flag to force unpair
FORCE_RESET = '--reset' in sys.argv or '-r' in sys.argv

# Setup database
Path.home().joinpath(".machinaear").mkdir(exist_ok=True)
DB_PATH = str(Path.home() / ".machinaear" / "events.db")
conn = sqlite3.connect(DB_PATH)
conn.execute('CREATE TABLE IF NOT EXISTS device_config (key TEXT PRIMARY KEY, value TEXT)')
conn.commit()

def get_cfg(k): 
    r = conn.execute('SELECT value FROM device_config WHERE key=?', (k,)).fetchone()
    return r[0] if r else None

def set_cfg(k, v): 
    conn.execute('INSERT OR REPLACE INTO device_config VALUES (?,?)', (k,v))
    conn.commit()

def clear_pairing():
    """Clear all pairing data to force re-pairing."""
    conn.execute("DELETE FROM device_config WHERE key IN ('device_id', 'device_token', 'pairing_code')")
    conn.commit()
    print("[RESET] Cleared pairing data - starting fresh!")

# If --reset flag is passed, clear pairing data
if FORCE_RESET:
    clear_pairing()

# GPIO setup - simple polling mode (no edge detection)
GPIO.setmode(GPIO.BCM)
GPIO.setup(SENSOR_PIN, GPIO.IN)

def gen_code():
    import random, string
    return ''.join(random.choices(string.ascii_uppercase + string.digits, k=6))

def cleanup(sig=None, frame=None):
    print("\nCleaning up...")
    GPIO.cleanup()
    conn.close()
    sys.exit(0)

signal.signal(signal.SIGINT, cleanup)
signal.signal(signal.SIGTERM, cleanup)

print("=" * 50)
print("  MachinaEar VMA309 Agent")
print("=" * 50)

device_id = get_cfg('device_id')
device_token = get_cfg('device_token')
is_paired = device_id is not None and device_token is not None

if not is_paired:
    code = get_cfg('pairing_code') or gen_code()
    set_cfg('pairing_code', code)
    mac = ':'.join(f'{(uuid.getnode() >> i) & 0xff:02x}' for i in range(0,48,8)).upper()
    
    print(f"\n  PAIRING CODE: {code}\n")
    print(f"  Go to: https://machinaear.me/devices")
    print(f"  Click Pair Device and enter the code\n")
    
    try:
        resp = requests.post(f"{API_URL}/device-registration/register-pending",
            json={"pairingCode": code, "mac": mac, "hostname": "raspberrypi"}, timeout=10)
        print(f"Registered with backend (status: {resp.status_code})")
        print("Waiting for pairing...")
    except Exception as e:
        print(f"Could not register: {e}")
        print("Waiting for pairing anyway...")
    
    while True:
        try:
            r = requests.get(f"{API_URL}/device-registration/check-pairing/{code}", timeout=10)
            if r.status_code == 200:
                data = r.json()
                if data.get('isPaired'):
                    device_id = data.get('id')
                    device_token = data.get('deviceToken')
                    set_cfg('device_id', device_id)
                    set_cfg('device_token', device_token)
                    print(f"Paired! Device ID: {device_id}")
                    break
        except Exception as e:
            print(f"Checking... ({e})")
        time.sleep(5)

print("\nMonitoring sounds...")
print(f"Threshold: {ANOMALY_THRESHOLD} sounds in {SOUND_WINDOW}s triggers anomaly")
print("-" * 50)

sound_times = []
last_state = 0
last_status_update = 0

while True:
    try:
        current = GPIO.input(SENSOR_PIN)
        
        # Detect rising edge (LOW to HIGH)
        if current == 1 and last_state == 0:
            sound_times.append(time.time())
            print(f"[{time.strftime('%H:%M:%S')}] Sound detected!")
        last_state = current
        
        # Count sounds in window
        cutoff = time.time() - SOUND_WINDOW
        sound_times = [t for t in sound_times if t > cutoff]
        count = len(sound_times)
        
        # Check for anomaly
        is_anomaly = count >= ANOMALY_THRESHOLD
        
        # Send status update every 10 seconds or on anomaly
        if time.time() - last_status_update > 10 or (is_anomaly and time.time() - last_status_update > 2):
            status = "abnormal" if is_anomaly else "normal"
            score = min(1.0, count / 10.0)
            
            if is_anomaly:
                print(f"[{time.strftime('%H:%M:%S')}] ANOMALY: {count} sounds!")
            
            try:
                resp = requests.post(f"{API_URL}/device-registration/status",
                    json={"status": status, "anomalyScore": score},
                    headers={"X-Device-Token": device_token}, timeout=5)
                if resp.status_code != 200:
                    print(f"[{time.strftime('%H:%M:%S')}] Status update failed: {resp.status_code}")
            except Exception as e:
                print(f"[{time.strftime('%H:%M:%S')}] Status update error: {e}")
            
            last_status_update = time.time()
        
        time.sleep(0.05)  # 50ms polling
        
    except Exception as e:
        print(f"Error: {e}")
        time.sleep(1)
