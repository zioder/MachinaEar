import { Device } from '@/types/api';
import { TrashIcon, DevicePhoneMobileIcon, ComputerDesktopIcon, ServerIcon, CheckCircleIcon, ExclamationTriangleIcon, XCircleIcon } from '@heroicons/react/24/outline';
import { getDeviceEffectiveStatus } from '@/lib/device-status';

interface DeviceListProps {
    devices: Device[];
    onDelete: (id: string) => void;
}

export default function DeviceList({ devices, onDelete }: DeviceListProps) {
    const getIcon = (type: string) => {
        switch (type?.toLowerCase()) {
            case 'mobile': return <DevicePhoneMobileIcon className="h-6 w-6" />;
            case 'desktop': return <ComputerDesktopIcon className="h-6 w-6" />;
            case 'iot': return <ServerIcon className="h-6 w-6" />;
            default: return <ServerIcon className="h-6 w-6" />;
        }
    };

    const getStatus = (device: Device) => {
        return getDeviceEffectiveStatus(device);
    };

    const getStatusBadge = (device: Device) => {
        const status = getStatus(device);
        switch (status) {
            case 'normal':
                return <span className="flex items-center text-green-600 dark:text-green-400"><CheckCircleIcon className="h-4 w-4 mr-1" />Normal</span>;
            case 'abnormal':
                return <span className="flex items-center text-yellow-600 dark:text-yellow-400"><ExclamationTriangleIcon className="h-4 w-4 mr-1" />Abnormal</span>;
            case 'offline':
                return <span className="flex items-center text-red-600 dark:text-red-400"><XCircleIcon className="h-4 w-4 mr-1" />Offline</span>;
            default:
                return <span className="text-gray-500">Unknown</span>;
        }
    };

    const formatLastSeen = (lastHeartbeat?: string) => {
        if (!lastHeartbeat) return 'Never';
        const lastSeen = new Date(lastHeartbeat);
        const now = new Date();
        const diffMs = now.getTime() - lastSeen.getTime();
        const diffSecs = Math.floor(diffMs / 1000);
        const diffMins = Math.floor(diffMs / 60000);
        if (diffSecs < 60) return `${diffSecs}s ago`;
        if (diffMins < 60) return `${diffMins}m ago`;
        return `${Math.floor(diffMs / 3600000)}h ago`;
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
                <div key={device.id} className="bg-white dark:bg-gray-800 p-4 rounded-lg shadow border border-gray-200 dark:border-gray-700">
                    <div className="flex items-center justify-between mb-3">
                        <div className="flex items-center space-x-3">
                            <div className="p-2 bg-indigo-100 dark:bg-indigo-900 rounded-full text-indigo-600 dark:text-indigo-300">
                                {getIcon(device.type)}
                            </div>
                            <div>
                                <h3 className="font-medium text-gray-900 dark:text-white">{device.name}</h3>
                                <p className="text-xs text-gray-500 dark:text-gray-400">{device.type}</p>
                            </div>
                        </div>
                        <button
                            onClick={() => device.id && onDelete(device.id)}
                            className="p-1 text-gray-400 hover:text-red-600 dark:hover:text-red-400 transition-colors"
                            title="Delete Device"
                        >
                            <TrashIcon className="h-5 w-5" />
                        </button>
                    </div>
                    <div className="flex items-center justify-between text-sm">
                        {getStatusBadge(device)}
                        <span className="text-gray-500 dark:text-gray-400">
                            {formatLastSeen(device.lastHeartbeat)}
                        </span>
                    </div>
                </div>
            ))}
        </div>
    );
}
