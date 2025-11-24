'use client';

import { useState, useEffect } from 'react';
import { useAuth } from '@/hooks';
import { ApiClient } from '@/lib/api-client';
import { Device } from '@/types/api';
import { API_URL } from '@/lib/constants';
import DeviceList from '@/components/devices/DeviceList';
import DeviceModal from '@/components/devices/DeviceModal';
import { PlusIcon } from '@heroicons/react/24/solid';

export default function DevicesPage() {
    const { user } = useAuth();
    const [devices, setDevices] = useState<Device[]>([]);
    const [loading, setLoading] = useState(true);
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [editingDevice, setEditingDevice] = useState<Device | null>(null);

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

    const handleSaveDevice = async (name: string, type: string, id?: string) => {
        if (id) {
            // Update existing device
            await api.updateDevice(id, name, type);
        } else {
            // Add new device
            if (devices.length >= 5) {
                throw new Error('Maximum number of devices (5) reached.');
            }
            await api.addDevice(name, type);
        }
        await fetchDevices();
        setEditingDevice(null);
    };

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

    const handleEditDevice = (device: Device) => {
        setEditingDevice(device);
        setIsModalOpen(true);
    };

    const handleAddDevice = () => {
        if (devices.length >= 5) {
            alert('Maximum number of devices (5) reached.');
            return;
        }
        setEditingDevice(null);
        setIsModalOpen(true);
    };

    const handleCloseModal = () => {
        setIsModalOpen(false);
        setEditingDevice(null);
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
                    onEdit={handleEditDevice}
                />
            )}

            {/* Floating Action Button */}
            <button
                onClick={handleAddDevice}
                disabled={devices.length >= 5}
                className={`fixed bottom-8 right-8 p-4 bg-indigo-600 text-white rounded-full shadow-lg transition-colors focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 z-40 ${
                    devices.length >= 5
                        ? 'opacity-50 cursor-not-allowed'
                        : 'hover:bg-indigo-700'
                }`}
                aria-label="Add Device"
                title={devices.length >= 5 ? 'Maximum number of devices (5) reached.' : 'Add Device'}
            >
                <PlusIcon className="h-6 w-6" />
            </button>

            <DeviceModal
                isOpen={isModalOpen}
                onClose={handleCloseModal}
                onSave={handleSaveDevice}
                initialData={editingDevice}
            />
        </div>
    );
}
