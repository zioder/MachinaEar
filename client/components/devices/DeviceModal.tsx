import { useState, useEffect } from 'react';
import { Device } from '@/types/api';

interface DeviceModalProps {
    isOpen: boolean;
    onClose: () => void;
    onSave: (name: string, type: string, id?: string) => Promise<void>;
    initialData?: Device | null;
}

export default function DeviceModal({ isOpen, onClose, onSave, initialData }: DeviceModalProps) {
    const [name, setName] = useState('');
    const [type, setType] = useState('Mobile');
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');

    const isEditMode = !!initialData;

    // Initialize form when modal opens or initialData changes
    useEffect(() => {
        if (isOpen) {
            if (initialData) {
                setName(initialData.name || '');
                setType(initialData.type || 'Mobile');
            } else {
                setName('');
                setType('Mobile');
            }
            setError('');
        }
    }, [isOpen, initialData]);

    if (!isOpen) return null;

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setLoading(true);
        setError('');
        try {
            await onSave(name, type, initialData?.id);
            setName('');
            setType('Mobile');
            onClose();
        } catch (err: any) {
            setError(err.message || `Failed to ${isEditMode ? 'update' : 'add'} device`);
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
            <div className="bg-white dark:bg-gray-800 p-6 rounded-lg shadow-xl w-full max-w-md">
                <h2 className="text-xl font-bold mb-4 text-gray-900 dark:text-white">
                    {isEditMode ? 'Edit Device' : 'Add New Device'}
                </h2>

                {error && (
                    <div className="mb-4 p-2 bg-red-100 text-red-700 rounded text-sm dark:bg-red-900 dark:text-red-200">
                        {error}
                    </div>
                )}

                <form onSubmit={handleSubmit}>
                    <div className="mb-4">
                        <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                            Device Name
                        </label>
                        <input
                            type="text"
                            value={name}
                            onChange={(e) => setName(e.target.value)}
                            className="w-full p-2 border rounded dark:bg-gray-700 dark:border-gray-600 dark:text-white"
                            placeholder="e.g., My iPhone"
                            required
                        />
                    </div>

                    <div className="mb-6">
                        <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                            Device Type
                        </label>
                        <select
                            value={type}
                            onChange={(e) => setType(e.target.value)}
                            className="w-full p-2 border rounded dark:bg-gray-700 dark:border-gray-600 dark:text-white"
                        >
                            <option value="Mobile">Mobile</option>
                            <option value="Desktop">Desktop</option>
                            <option value="Tablet">Tablet</option>
                            <option value="IoT">IoT</option>
                            <option value="Other">Other</option>
                        </select>
                    </div>

                    <div className="flex justify-end space-x-3">
                        <button
                            type="button"
                            onClick={onClose}
                            className="px-4 py-2 text-gray-600 hover:text-gray-800 dark:text-gray-400 dark:hover:text-white"
                            disabled={loading}
                        >
                            Cancel
                        </button>
                        <button
                            type="submit"
                            className="px-4 py-2 bg-indigo-600 text-white rounded hover:bg-indigo-700 disabled:opacity-50"
                            disabled={loading}
                        >
                            {loading ? (isEditMode ? 'Updating...' : 'Adding...') : (isEditMode ? 'Update Device' : 'Add Device')}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}

