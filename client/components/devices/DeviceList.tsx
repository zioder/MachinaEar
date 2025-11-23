import { Device } from '@/types/api';
import { TrashIcon, PencilIcon, DevicePhoneMobileIcon, ComputerDesktopIcon, ServerIcon } from '@heroicons/react/24/outline';

interface DeviceListProps {
    devices: Device[];
    onDelete: (id: string) => void;
    onEdit: (device: Device) => void;
}

export default function DeviceList({ devices, onDelete, onEdit }: DeviceListProps) {
    const getIcon = (type: string) => {
        switch (type.toLowerCase()) {
            case 'mobile': return <DevicePhoneMobileIcon className="h-6 w-6" />;
            case 'desktop': return <ComputerDesktopIcon className="h-6 w-6" />;
            case 'iot': return <ServerIcon className="h-6 w-6" />;
            default: return <ComputerDesktopIcon className="h-6 w-6" />;
        }
    };

    if (devices.length === 0) {
        return (
            <div className="text-center py-10 text-gray-500 dark:text-gray-400">
                No devices found. Add a device to get started.
            </div>
        );
    }

    return (
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
            {devices.map((device) => (
                <div key={device.id} className="bg-white dark:bg-gray-800 p-4 rounded-lg shadow border border-gray-200 dark:border-gray-700 flex items-center justify-between">
                    <div className="flex items-center space-x-3">
                        <div className="p-2 bg-indigo-100 dark:bg-indigo-900 rounded-full text-indigo-600 dark:text-indigo-300">
                            {getIcon(device.type)}
                        </div>
                        <div>
                            <h3 className="font-medium text-gray-900 dark:text-white">{device.name}</h3>
                            <p className="text-sm text-gray-500 dark:text-gray-400">{device.type}</p>
                        </div>
                    </div>
                    <div className="flex space-x-2">
                        <button
                            onClick={() => onEdit(device)}
                            className="p-1 text-gray-400 hover:text-indigo-600 dark:hover:text-indigo-400 transition-colors"
                            title="Edit Device"
                        >
                            <PencilIcon className="h-5 w-5" />
                        </button>
                        <button
                            onClick={() => device.id && onDelete(device.id)}
                            className="p-1 text-gray-400 hover:text-red-600 dark:hover:text-red-400 transition-colors"
                            title="Delete Device"
                        >
                            <TrashIcon className="h-5 w-5" />
                        </button>
                    </div>
                </div>
            ))}
        </div>
    );
}
