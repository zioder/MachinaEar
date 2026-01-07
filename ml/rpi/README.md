# MachinaEar Raspberry Pi Setup Guide

This guide explains how to set up and pair a Raspberry Pi with your MachinaEar account for real-time audio anomaly detection.

## Quick Start (Fresh Raspberry Pi)

**One-command setup** - Run this on a fresh Raspberry Pi OS:

```bash
# Download and run the installer
curl -sSL https://raw.githubusercontent.com/YOUR_REPO/main/ml/rpi/install.sh | bash
```

Or step-by-step:

```bash
# 1. Download the installer
wget https://raw.githubusercontent.com/YOUR_REPO/main/ml/rpi/install.sh

# 2. Make it executable
chmod +x install.sh

# 3. Run it
./install.sh
```

After installation:
1. Connect a USB microphone
2. Run `~/machinaear/start.sh`
3. Note the **6-character pairing code** shown
4. Open the MachinaEar web app → **Devices** → **Add Device**
5. Enter the code and give your device a name (e.g., "Workshop Sensor")

---

## Hardware Requirements

- Raspberry Pi 4 (2GB+ RAM recommended)
- USB Microphone or USB Sound Card with 3.5mm mic input
- MicroSD card (16GB+)
- Power supply (5V 3A)
- WiFi or Ethernet connectivity

## Software Setup (Manual)

If you prefer manual setup or the installer doesn't work, follow these steps:

### 1. Prepare the Raspberry Pi

Copy and paste this entire block into your terminal:

```bash
# Update system and install dependencies
sudo apt update && sudo apt upgrade -y
sudo apt install -y python3-pip python3-venv libportaudio2 libsndfile1 alsa-utils git

# Create project directory
mkdir -p ~/machinaear
cd ~/machinaear

# Create virtual environment
python3 -m venv venv
source venv/bin/activate
```

### 2. Copy MachinaEar Files

**Option A: Using Git**
```bash
cd ~/machinaear
git clone https://github.com/YOUR_REPO/MachinaEar.git temp
mv temp/ml ./ml
rm -rf temp
```

**Option B: Manual Copy (from your computer)**
```bash
# On your computer, copy the ml folder to the Pi:
scp -r ml/ pi@raspberrypi.local:~/machinaear/
```

### 3. Install Python Dependencies

```bash
cd ~/machinaear
source venv/bin/activate
pip install -r ml/requirements-rpi.txt
```

Or install directly:
```bash
pip install numpy sounddevice scipy requests flask flask-cors
pip install torch torchaudio --index-url https://download.pytorch.org/whl/cpu
```

### 3. Test the Microphone

```bash
# List audio devices
python3 -c "import sounddevice as sd; print(sd.query_devices())"

# Test recording (5 seconds)
python3 -c "
import sounddevice as sd
import numpy as np
print('Recording...')
audio = sd.rec(int(5 * 16000), samplerate=16000, channels=1)
sd.wait()
print(f'Recorded {len(audio)} samples')
print(f'Max amplitude: {np.max(np.abs(audio)):.4f}')
"
```

## Running the Agent

### Quick Start (Development)

```bash
cd ~/machinaear/ml
source ../venv/bin/activate
python rpi_agent.py
```

The agent will:
1. Display a **6-character pairing code** in the terminal
2. Register with the MachinaEar backend
3. Wait for you to complete pairing via the web app

### Pairing Process

1. **On your Raspberry Pi**: Run `python rpi_agent.py` and note the pairing code
2. **On the Web App**: 
   - Go to **Devices** page
   - Click the **Link icon** to pair a new device
   - Enter the 6-character code and give your device a name
   - Click **Pair Device**
3. **On your Raspberry Pi**: The agent will confirm pairing and start monitoring

## WiFi Provisioning (Fresh Setup)

If the Pi doesn't have WiFi configured:

1. The agent will detect no internet connection
2. It will start in **Access Point mode** (WiFi name: `MachinaEar-Setup`)
3. Connect to this network from your phone
4. Open `http://192.168.4.1` in your browser
5. Enter your WiFi credentials
6. The Pi will connect to your network and continue the pairing process

## Running as a Service

To run the agent automatically on boot:

```bash
# Create systemd service
sudo tee /etc/systemd/system/machinaear.service > /dev/null << 'EOF'
[Unit]
Description=MachinaEar Audio Monitoring Agent
After=network-online.target
Wants=network-online.target

[Service]
Type=simple
User=pi
WorkingDirectory=/home/pi/machinaear/ml
ExecStart=/home/pi/machinaear/venv/bin/python /home/pi/machinaear/ml/rpi_agent.py
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

# Enable and start the service
sudo systemctl daemon-reload
sudo systemctl enable machinaear
sudo systemctl start machinaear

# Check status
sudo systemctl status machinaear

# View logs
journalctl -u machinaear -f
```

## Configuration

Edit the configuration in `rpi_agent.py`:

```python
@dataclass
class Config:
    # Backend API URL
    api_base_url: str = "https://iam.machinaear.me/iam"
    
    # Audio settings
    sample_rate: int = 16000      # Hz
    chunk_duration: float = 1.0   # seconds
    
    # ML settings
    anomaly_threshold: float = 0.1  # MSE threshold
    
    # Sync intervals
    sync_interval: float = 30.0     # seconds
    heartbeat_interval: float = 60.0  # seconds
```

## Troubleshooting

### No audio devices found
```bash
# Check if microphone is connected
arecord -l

# Install/reinstall audio drivers
sudo apt install -y alsa-utils pulseaudio
```

### Agent can't connect to backend
- Check internet connectivity: `ping google.com`
- Verify API URL in config
- Check firewall settings

### High CPU usage
- Reduce `sample_rate` to 8000
- Increase `chunk_duration` to 2.0
- Use a lighter model

## Offline Mode

The agent works offline:
- Anomaly detection runs locally
- Events are saved to SQLite database (`~/.machinaear/events.db`)
- Events sync automatically when internet is restored
