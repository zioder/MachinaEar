'use client';

import { useState, useEffect } from 'react';
import { useAuth } from '@/hooks';
import { ApiClient } from '@/lib/api-client';
import { Device } from '@/types/api';
import { API_URL } from '@/lib/constants';
import DeviceList from '@/components/devices/DeviceList';
import PairingModal from '@/components/devices/PairingModal';
import { LinkIcon } from '@heroicons/react/24/solid';

export default function DevicesPage() {
    const { user } = useAuth();
    const [devices, setDevices] = useState<Device[]>([]);
    const [loading, setLoading] = useState(true);
    const [isPairingModalOpen, setIsPairingModalOpen] = useState(false);

    // Initialize API client
    // In a real app, this might be provided via context or hook
    const api = new ApiClient(API_URL);

    const fetchDevices = async () => {
        try {
            const data = await api.getDevices();
            setDevices(data);
        } catch (error) {
            console.error('Failed to fetch devices:', error);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        if (user) {
            fetchDevices();
        }
    }, [user]);

    const handleDeleteDevice = async (id: string) => {
        if (confirm('Are you sure you want to delete this device?')) {
            try {
                await api.deleteDevice(id);
                await fetchDevices();
            } catch (error) {
                console.error('Failed to delete device:', error);
                alert('Failed to delete device');
            }
        }
    };

    const handlePairDevice = () => {
        if (devices.length >= 5) {
            alert('Maximum number of devices (5) reached.');
            return;
        }
        setIsPairingModalOpen(true);
    };

    const handlePairingComplete = () => {
        fetchDevices();
    };

    return (
        <div className="container mx-auto px-4 py-8">
            <div className="flex justify-between items-center mb-6">
                <h1 className="text-2xl font-bold text-gray-900 dark:text-white">My Devices</h1>
                <div className="text-sm text-gray-500 dark:text-gray-400">
                    {devices.length} / 5 Devices
                </div>
            </div>

            {loading ? (
                <div className="flex justify-center py-10">Loading...</div>
            ) : (
                <DeviceList
                    devices={devices}
                    onDelete={handleDeleteDevice}
                />
            )}

            {/* Floating Action Button - Add/Pair Device */}
            <div className="fixed bottom-8 right-8 z-40">
                <button
                    onClick={handlePairDevice}
                    disabled={devices.length >= 5}
                    className={`p-4 bg-gradient-to-r from-purple-600 to-indigo-600 text-white rounded-full shadow-lg transition-all focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-purple-500 ${devices.length >= 5
                            ? 'opacity-50 cursor-not-allowed'
                            : 'hover:from-purple-500 hover:to-indigo-500 hover:scale-105'
                        }`}
                    aria-label="Add Device"
                    title={devices.length >= 5 ? 'Maximum devices reached' : 'Add New Device'}
                >
                    <LinkIcon className="h-6 w-6" />
                </button>
            </div>

            <PairingModal
                isOpen={isPairingModalOpen}
                onClose={() => setIsPairingModalOpen(false)}
                onPaired={handlePairingComplete}
            />
        </div>
    );
}

