'use client';

import { useState, useEffect } from 'react';
import { Device } from '@/types/api';
import { ApiClient } from '@/lib/api-client';
import { API_URL } from '@/lib/constants';

interface PairingModalProps {
    isOpen: boolean;
    onClose: () => void;
    onPaired: () => void;
}

export default function PairingModal({ isOpen, onClose, onPaired }: PairingModalProps) {
    const [step, setStep] = useState<'instructions' | 'code' | 'loading' | 'success'>('instructions');
    const [pairingCode, setPairingCode] = useState('');
    const [deviceName, setDeviceName] = useState('');
    const [availableDevices, setAvailableDevices] = useState<Device[]>([]);
    const [selectedDevice, setSelectedDevice] = useState<Device | null>(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');

    const api = new ApiClient(API_URL);

    // Reset state when modal opens/closes
    useEffect(() => {
        if (isOpen) {
            setStep('instructions');
            setPairingCode('');
            setDeviceName('');
            setSelectedDevice(null);
            setError('');
            fetchAvailableDevices();
        }
    }, [isOpen]);

    const fetchAvailableDevices = async () => {
        try {
            const devices = await api.getAvailableDevices();
            setAvailableDevices(devices);
        } catch (err) {
            console.error('Failed to fetch available devices:', err);
        }
    };

    const handleEnterCode = () => {
        setStep('code');
    };

    const handlePair = async () => {
        if (!pairingCode.trim()) {
            setError('Please enter a pairing code');
            return;
        }
        if (!deviceName.trim()) {
            setError('Please enter a name for your device');
            return;
        }

        setLoading(true);
        setError('');

        try {
            await api.pairDevice(pairingCode.toUpperCase(), deviceName);
            setStep('success');
            setTimeout(() => {
                onPaired();
                onClose();
            }, 2000);
        } catch (err: any) {
            setError(err.message || 'Failed to pair device. Please check the code and try again.');
        } finally {
            setLoading(false);
        }
    };

    const handleSelectDevice = (device: Device) => {
        setSelectedDevice(device);
        setPairingCode(device.pairingCode || '');
        setDeviceName(''); // Keep empty for manual entry
        setStep('code');
    };

    if (!isOpen) return null;

    return (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm flex items-center justify-center z-50 p-4">
            <div className="bg-gradient-to-br from-gray-900 to-gray-800 p-6 rounded-2xl shadow-2xl w-full max-w-md border border-gray-700">

                {/* Header */}
                <div className="flex items-center justify-between mb-6">
                    <h2 className="text-xl font-bold text-white flex items-center gap-2">
                        <span className="text-2xl">🔗</span>
                        Pair New Device
                    </h2>
                    <button
                        onClick={onClose}
                        className="text-gray-400 hover:text-white transition-colors"
                    >
                        <svg className="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                        </svg>
                    </button>
                </div>

                {error && (
                    <div className="mb-4 p-3 bg-red-500/20 border border-red-500/50 text-red-300 rounded-lg text-sm">
                        {error}
                    </div>
                )}

                {/* Step: Instructions */}
                {step === 'instructions' && (
                    <div className="space-y-6">
                        <div className="bg-indigo-500/10 border border-indigo-500/30 rounded-xl p-4">
                            <h3 className="font-semibold text-indigo-300 mb-3">Setup Instructions</h3>
                            <ol className="space-y-3 text-sm text-gray-300">
                                <li className="flex gap-3">
                                    <span className="flex-shrink-0 w-6 h-6 rounded-full bg-indigo-500/30 flex items-center justify-center text-indigo-300 font-bold text-xs">1</span>
                                    <span>Power on your Raspberry Pi with MachinaEar installed</span>
                                </li>
                                <li className="flex gap-3">
                                    <span className="flex-shrink-0 w-6 h-6 rounded-full bg-indigo-500/30 flex items-center justify-center text-indigo-300 font-bold text-xs">2</span>
                                    <span>If no WiFi is configured, connect to <strong className="text-white">MachinaEar-Setup</strong> network</span>
                                </li>
                                <li className="flex gap-3">
                                    <span className="flex-shrink-0 w-6 h-6 rounded-full bg-indigo-500/30 flex items-center justify-center text-indigo-300 font-bold text-xs">3</span>
                                    <span>Note the <strong className="text-white">6-character pairing code</strong> shown on screen or console</span>
                                </li>
                            </ol>
                        </div>

                        {/* Available devices */}
                        {availableDevices.length > 0 && (
                            <div>
                                <h4 className="text-sm font-medium text-gray-400 mb-2">Devices waiting to be paired:</h4>
                                <div className="space-y-2">
                                    {availableDevices.map((device) => (
                                        <button
                                            key={device.id}
                                            onClick={() => handleSelectDevice(device)}
                                            className="w-full p-3 bg-gray-700/50 hover:bg-gray-700 rounded-lg text-left transition-colors border border-gray-600 hover:border-indigo-500"
                                        >
                                            <div className="flex items-center justify-between">
                                                <div>
                                                    <p className="text-white font-medium">{device.name || device.mac}</p>
                                                    <p className="text-gray-400 text-sm">Code: {device.pairingCode}</p>
                                                </div>
                                                <svg className="w-5 h-5 text-indigo-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
                                                </svg>
                                            </div>
                                        </button>
                                    ))}
                                </div>
                            </div>
                        )}

                        <button
                            onClick={handleEnterCode}
                            className="w-full py-3 bg-gradient-to-r from-indigo-600 to-purple-600 hover:from-indigo-500 hover:to-purple-500 text-white font-semibold rounded-xl transition-all shadow-lg shadow-indigo-500/25"
                        >
                            Enter Pairing Code
                        </button>
                    </div>
                )}

                {/* Step: Enter Code */}
                {step === 'code' && (
                    <div className="space-y-4">
                        <div>
                            <label className="block text-sm font-medium text-gray-300 mb-2">
                                Pairing Code
                            </label>
                            <input
                                type="text"
                                value={pairingCode}
                                onChange={(e) => setPairingCode(e.target.value.toUpperCase())}
                                className="w-full p-3 bg-gray-700/50 border border-gray-600 rounded-xl text-white text-center text-2xl font-mono tracking-widest focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20 outline-none"
                                placeholder="ABC123"
                                maxLength={6}
                                autoFocus
                            />
                        </div>

                        <div>
                            <label className="block text-sm font-medium text-gray-300 mb-2">
                                Device Name
                            </label>
                            <input
                                type="text"
                                value={deviceName}
                                onChange={(e) => setDeviceName(e.target.value)}
                                className="w-full p-3 bg-gray-700/50 border border-gray-600 rounded-xl text-white focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20 outline-none"
                                placeholder="Living Room Pi"
                            />
                        </div>

                        <div className="flex gap-3 pt-2">
                            <button
                                onClick={() => setStep('instructions')}
                                className="flex-1 py-3 bg-gray-700 hover:bg-gray-600 text-white font-medium rounded-xl transition-colors"
                            >
                                Back
                            </button>
                            <button
                                onClick={handlePair}
                                disabled={loading || !pairingCode || !deviceName}
                                className="flex-1 py-3 bg-gradient-to-r from-indigo-600 to-purple-600 hover:from-indigo-500 hover:to-purple-500 text-white font-semibold rounded-xl transition-all disabled:opacity-50 disabled:cursor-not-allowed"
                            >
                                {loading ? (
                                    <span className="flex items-center justify-center gap-2">
                                        <svg className="animate-spin h-5 w-5" viewBox="0 0 24 24">
                                            <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" fill="none" />
                                            <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
                                        </svg>
                                        Pairing...
                                    </span>
                                ) : 'Pair Device'}
                            </button>
                        </div>
                    </div>
                )}

                {/* Step: Success */}
                {step === 'success' && (
                    <div className="text-center py-8 space-y-4">
                        <div className="w-16 h-16 bg-green-500/20 rounded-full flex items-center justify-center mx-auto">
                            <svg className="w-8 h-8 text-green-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                            </svg>
                        </div>
                        <h3 className="text-xl font-bold text-white">Device Paired!</h3>
                        <p className="text-gray-400">Your device is now connected and monitoring.</p>
                    </div>
                )}
            </div>
        </div>
    );
}
