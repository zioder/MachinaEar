#!/usr/bin/env python3
"""
MachinaEar Local Testing Script
This script allows you to test the full pairing and inference flow locally
without needing a Raspberry Pi or actual WiFi provisioning.

Usage:
    1. Start your backend server (or use the production one)
    2. Run: python local_test.py
    3. Open the frontend and use the Pairing Modal to pair the device
    4. The script will then start simulating audio detection
"""

import os
import sys
import argparse
import time
import json
import threading
from pathlib import Path

# Add ml directory to path
sys.path.insert(0, str(Path(__file__).parent))

from rpi_agent import (
    MachinaEarAgent, 
    Config, 
    config, 
    check_internet_connection,
    init_database,
    get_device_config,
    set_device_config
)


def print_banner():
    """Print a nice banner."""
    print("\n" + "="*60)
    print(r"""
    __  __            _     _             ___              
   |  \/  | __ _  ___| |__ (_)_ __   __ _| __| __ _  _ _   
   | |\/| |/ _` |/ __| '_ \| | '_ \ / _` | _| / _` || '_|  
   |_|  |_|\__,_|\___|_| |_|_|_| |_|\__,_|___|\__,_||_|    
   
   LOCAL TESTING MODE
    """)
    print("="*60 + "\n")


def run_local_test(args):
    """Run the local test."""
    
    # Override config for local testing
    if args.api_url:
        config.api_base_url = args.api_url
        print(f"[CONFIG] Using API URL: {config.api_base_url}")
    
    if args.mock_audio:
        print("[CONFIG] Mock audio mode enabled (no microphone required)")
    
    # Initialize database
    init_database()
    
    # Reset pairing if requested
    if args.reset:
        print("[CONFIG] Resetting device pairing state...")
        set_device_config('device_id', '')
        set_device_config('device_token', '')
        set_device_config('pairing_code', '')
    
    # Check connectivity
    print(f"\n[TEST] Checking internet connectivity...")
    if check_internet_connection():
        print("[TEST] [OK] Internet connection available")
    else:
        print("[TEST] [WARN] No internet connection")
        if not args.offline:
            print("[TEST] Use --offline to test in offline mode")
            return
    
    # Create and run agent
    agent = MachinaEarAgent()
    
    if not agent.is_paired:
        print("\n" + "="*60)
        print("  DEVICE NOT PAIRED")
        print("="*60)
        print("\n  To pair this device:")
        print("  1. Go to the MachinaEar web app")
        print("  2. Navigate to Devices page")
        print("  3. Click 'Pair Device' button")
        print("  4. Enter the pairing code shown below")
        print("\n" + "="*60)
        
        # Start pairing
        pairing_code = agent.start_pairing()
        
        print(f"\n  [*] PAIRING CODE: {pairing_code}")
        print("\n" + "="*60)
        print("\n  Waiting for pairing to complete...")
        print("  (Press Ctrl+C to cancel)\n")
        
        # Poll for pairing completion
        agent.running = True
        while agent.running and not agent.is_paired:
            if agent.poll_pairing():
                print("\n[SUCCESS] Device paired successfully!")
                print(f"[SUCCESS] Device ID: {agent.device_id}")
                break
            time.sleep(3)
            print(".", end="", flush=True)
        
        if not agent.is_paired:
            print("\n[ERROR] Pairing was not completed")
            return
    else:
        print(f"\n[INFO] Device already paired: {agent.device_id}")
    
    # Now run the detection loop
    print("\n" + "="*60)
    print("  STARTING AUDIO MONITORING")
    print("="*60)
    print("\n  The agent will now:")
    print("  - Record audio (or simulate if --mock-audio)")
    print("  - Run anomaly detection using the ML model")
    print("  - Send results to the backend")
    print("  - Buffer results if offline")
    print("\n  Press Ctrl+C to stop\n")
    
    if args.duration:
        print(f"[INFO] Will run for {args.duration} seconds")
        
        def stop_after_duration():
            time.sleep(args.duration)
            print("\n[INFO] Duration reached, stopping...")
            agent.stop()
        
        timer = threading.Thread(target=stop_after_duration, daemon=True)
        timer.start()
    
    try:
        agent.run()
    except KeyboardInterrupt:
        agent.stop()
        print("\n[INFO] Stopped by user")


def run_mock_backend_test():
    """Run a simple mock backend for testing without a real server."""
    try:
        from flask import Flask, request, jsonify, redirect, make_response
        from flask_cors import CORS
    except ImportError:
        print("[ERROR] Flask and flask-cors required for mock backend")
        print("        Run: pip install flask flask-cors")
        return
    
    app = Flask(__name__)
    CORS(app, supports_credentials=True)
    
    # In-memory storage
    pending_devices = {}
    paired_devices = {}
    user_devices = {}  # Devices owned by user
    
    # Mock user
    mock_user = {
        'id': 'mock-user-123',
        'email': 'test@machinaear.me',
        'firstName': 'Test',
        'lastName': 'User',
        'roles': ['USER']
    }
    
    # ===== AUTH ENDPOINTS =====
    @app.route('/iam/auth/authorize', methods=['GET'])
    def authorize():
        # Simulate OAuth flow - redirect back with code
        redirect_uri = request.args.get('redirect_uri', 'http://localhost:3001/auth/callback')
        state = request.args.get('state', '')
        # Return a mock authorization code
        return redirect(f"{redirect_uri}?code=mock-auth-code-12345&state={state}")
    
    @app.route('/iam/auth/token', methods=['POST'])
    def token():
        # Return mock tokens
        response = make_response(jsonify({
            'access_token': 'mock-access-token-xyz',
            'refresh_token': 'mock-refresh-token-abc',
            'token_type': 'Bearer',
            'expires_in': 3600
        }))
        # Set session cookie
        response.set_cookie('session', 'mock-session-id', httponly=True, samesite='Lax')
        return response
    
    @app.route('/iam/auth/me', methods=['GET'])
    def me():
        return jsonify(mock_user)
    
    @app.route('/iam/auth/logout', methods=['POST'])
    def logout():
        response = make_response(jsonify({'success': True}))
        response.delete_cookie('session')
        return response
    
    # ===== DEVICES ENDPOINTS =====
    @app.route('/iam/devices', methods=['GET'])
    def get_devices():
        # Return user's paired devices
        devices = list(user_devices.values())
        return jsonify(devices)
    
    @app.route('/iam/devices', methods=['POST'])
    def add_device():
        data = request.json
        device_id = f"manual-device-{len(user_devices)+1}"
        device = {
            'id': device_id,
            'name': data.get('name', 'New Device'),
            'type': data.get('type', 'IoT'),
            'status': 'normal',
            'isPaired': True,
            'cpuUsage': 0,
            'memoryUsage': 0,
            'temperature': 0
        }
        user_devices[device_id] = device
        print(f"[MOCK] Device added: {device['name']}")
        return jsonify(device)
    
    @app.route('/iam/device-registration/register-pending', methods=['POST'])
    def register_pending():
        data = request.json
        code = data.get('pairingCode')
        pending_devices[code] = {
            'pairingCode': code,
            'mac': data.get('mac'),
            'hostname': data.get('hostname'),
            'isPaired': False,
            'id': f'mock-device-{code}',
            'name': data.get('hostname')
        }
        print(f"[MOCK] Device registered with code: {code}")
        return jsonify(pending_devices[code]), 202
    
    @app.route('/iam/device-registration/check-pairing/<code>', methods=['GET'])
    def check_pairing(code):
        if code in paired_devices:
            return jsonify(paired_devices[code])
        if code in pending_devices:
            return jsonify({'status': 'pending'}), 202
        return jsonify({'error': 'Not found'}), 404
    
    @app.route('/iam/devices/available', methods=['GET'])
    def get_available():
        return jsonify(list(pending_devices.values()))
    
    @app.route('/iam/devices/pair', methods=['POST'])
    def pair_device():
        data = request.json
        code = data.get('pairingCode')
        if code in pending_devices:
            device = pending_devices.pop(code)
            device['isPaired'] = True
            device['name'] = data.get('name', device['name'])
            device['type'] = 'Raspberry Pi'
            device['deviceToken'] = f'mock-token-{code}'
            device['status'] = 'normal'
            device['cpuUsage'] = 0
            device['memoryUsage'] = 0
            device['temperature'] = 0
            paired_devices[code] = device
            user_devices[device['id']] = device  # Add to user's devices
            print(f"[MOCK] Device paired: {device['name']}")
            return jsonify(device)
        return jsonify({'error': 'Device not found'}), 404
    
    @app.route('/iam/devices/<device_id>', methods=['DELETE'])
    def delete_device(device_id):
        if device_id in user_devices:
            del user_devices[device_id]
            print(f"[MOCK] Device deleted: {device_id}")
            return '', 204
        return jsonify({'error': 'Device not found'}), 404
    
    @app.route('/iam/devices/<device_id>', methods=['PUT'])
    def update_device(device_id):
        data = request.json
        if device_id in user_devices:
            user_devices[device_id]['name'] = data.get('name', user_devices[device_id]['name'])
            user_devices[device_id]['type'] = data.get('type', user_devices[device_id]['type'])
            print(f"[MOCK] Device updated: {device_id}")
            return jsonify(user_devices[device_id])
        return jsonify({'error': 'Device not found'}), 404
    
    @app.route('/iam/devices/<device_id>/status', methods=['PATCH'])
    def update_status(device_id):
        data = request.json
        status = data.get('status', 'normal')
        anomaly_score = data.get('anomalyScore', 0)
        
        # Update device status in user_devices
        if device_id in user_devices:
            user_devices[device_id]['status'] = status
            user_devices[device_id]['anomalyScore'] = anomaly_score
            user_devices[device_id]['cpuUsage'] = data.get('cpuUsage', 0)
            user_devices[device_id]['memoryUsage'] = data.get('memoryUsage', 0)
            user_devices[device_id]['temperature'] = data.get('temperature', 0)
            user_devices[device_id]['lastHeartbeat'] = time.strftime('%Y-%m-%dT%H:%M:%SZ')
        
        # Also check paired_devices
        for code, dev in paired_devices.items():
            if dev['id'] == device_id:
                dev['status'] = status
                dev['anomalyScore'] = anomaly_score
        
        status_icon = "[!]" if status == "abnormal" else "[OK]"
        print(f"[MOCK] {status_icon} Status update for {device_id}: {status} (score: {anomaly_score})")
        return jsonify({'status': 'ok'})
    
    print("\n" + "="*60)
    print("  MOCK BACKEND SERVER")
    print("="*60)
    print("\n  Running at: http://localhost:8080")
    print("  API base:   http://localhost:8080/iam")
    print("\n  Use this with: python local_test.py --api-url http://localhost:8080/iam")
    print("\n  Press Ctrl+C to stop\n")
    
    app.run(host='0.0.0.0', port=8080, debug=False)


def main():
    parser = argparse.ArgumentParser(
        description='MachinaEar Local Testing Script',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  # Test with production backend
  python local_test.py
  
  # Test with local backend
  python local_test.py --api-url http://localhost:8080/iam
  
  # Test in mock audio mode (no microphone needed)
  python local_test.py --mock-audio
  
  # Run the mock backend server
  python local_test.py --mock-backend
  
  # Reset pairing and start fresh
  python local_test.py --reset
  
  # Run for a specific duration
  python local_test.py --duration 60
        """
    )
    
    parser.add_argument('--api-url', type=str, 
                        help='Backend API URL (default: https://iam.machinaear.me/iam)')
    parser.add_argument('--mock-audio', action='store_true',
                        help='Use mock audio instead of real microphone')
    parser.add_argument('--mock-backend', action='store_true',
                        help='Run a mock backend server for testing')
    parser.add_argument('--offline', action='store_true',
                        help='Allow running in offline mode')
    parser.add_argument('--reset', action='store_true',
                        help='Reset device pairing state before testing')
    parser.add_argument('--duration', type=int,
                        help='Run for a specific number of seconds')
    
    args = parser.parse_args()
    
    print_banner()
    
    if args.mock_backend:
        run_mock_backend_test()
    else:
        run_local_test(args)


if __name__ == '__main__':
    main()
