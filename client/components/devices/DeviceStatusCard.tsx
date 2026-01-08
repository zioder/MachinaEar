import { Device } from '@/types/api';
import {
  DevicePhoneMobileIcon,
  ComputerDesktopIcon,
  ServerIcon,
  SignalIcon,
  ExclamationTriangleIcon,
  CheckCircleIcon,
  XCircleIcon,
} from '@heroicons/react/24/outline';

interface DeviceStatusCardProps {
  device: Device;
  onClick?: () => void;
}

export default function DeviceStatusCard({ device, onClick }: DeviceStatusCardProps) {
  const getDeviceIcon = (type: string) => {
    switch (type.toLowerCase()) {
      case 'mobile':
        return <DevicePhoneMobileIcon className="h-8 w-8" />;
      case 'desktop':
        return <ComputerDesktopIcon className="h-8 w-8" />;
      case 'iot':
        return <ServerIcon className="h-8 w-8" />;
      default:
        return <ComputerDesktopIcon className="h-8 w-8" />;
    }
  };

  const getStatusConfig = (status?: string) => {
    switch (status) {
      case 'normal':
        return {
          icon: <CheckCircleIcon className="h-5 w-5" />,
          text: 'Normal',
          bgColor: 'bg-green-50 dark:bg-green-900/20',
          iconColor: 'text-green-600 dark:text-green-400',
          borderColor: 'border-green-200 dark:border-green-800',
          dotColor: 'bg-green-500',
        };
      case 'abnormal':
        return {
          icon: <ExclamationTriangleIcon className="h-5 w-5" />,
          text: 'Abnormal',
          bgColor: 'bg-yellow-50 dark:bg-yellow-900/20',
          iconColor: 'text-yellow-600 dark:text-yellow-400',
          borderColor: 'border-yellow-200 dark:border-yellow-800',
          dotColor: 'bg-yellow-500',
        };
      case 'offline':
        return {
          icon: <XCircleIcon className="h-5 w-5" />,
          text: 'Offline',
          bgColor: 'bg-red-50 dark:bg-red-900/20',
          iconColor: 'text-red-600 dark:text-red-400',
          borderColor: 'border-red-200 dark:border-red-800',
          dotColor: 'bg-red-500',
        };
      default:
        return {
          icon: <SignalIcon className="h-5 w-5" />,
          text: 'Unknown',
          bgColor: 'bg-gray-50 dark:bg-gray-800',
          iconColor: 'text-gray-600 dark:text-gray-400',
          borderColor: 'border-gray-200 dark:border-gray-700',
          dotColor: 'bg-gray-500',
        };
    }
  };

  // Check if device is offline (no heartbeat in 30 seconds)
  const isOffline = () => {
    if (!device.lastHeartbeat) return true;
    const lastSeen = new Date(device.lastHeartbeat);
    const now = new Date();
    const diffMs = now.getTime() - lastSeen.getTime();
    return diffMs > 30000; // 30 seconds
  };

  const effectiveStatus = isOffline() ? 'offline' : device.status;
  const statusConfig = getStatusConfig(effectiveStatus);

  const formatLastSeen = (lastHeartbeat?: string) => {
    if (!lastHeartbeat) return 'Never';
    const lastSeen = new Date(lastHeartbeat);
    const now = new Date();
    const diffMs = now.getTime() - lastSeen.getTime();
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMs / 3600000);
    const diffDays = Math.floor(diffMs / 86400000);

    if (diffMins < 1) return 'Just now';
    if (diffMins < 60) return `${diffMins}m ago`;
    if (diffHours < 24) return `${diffHours}h ago`;
    return `${diffDays}d ago`;
  };

  return (
    <div
      onClick={onClick}
      className={`${statusConfig.bgColor} ${statusConfig.borderColor} border-2 rounded-xl p-6 transition-all duration-200 ${
        onClick ? 'cursor-pointer hover:shadow-lg hover:scale-[1.02]' : ''
      }`}
    >
      {/* Header with device icon and status */}
      <div className="flex items-start justify-between mb-4">
        <div className="flex items-center space-x-3">
          <div className={`p-3 rounded-full ${statusConfig.iconColor} bg-white dark:bg-gray-800 shadow-sm`}>
            {getDeviceIcon(device.type)}
          </div>
          <div>
            <h3 className="text-lg font-bold text-gray-900 dark:text-white">
              {device.name}
            </h3>
            <p className="text-sm text-gray-600 dark:text-gray-400 capitalize">
              {device.type}
            </p>
          </div>
        </div>
        
        {/* Status indicator */}
        <div className="flex items-center space-x-2">
          <span className={`flex h-3 w-3 relative`}>
            <span className={`animate-ping absolute inline-flex h-full w-full rounded-full ${statusConfig.dotColor} opacity-75`}></span>
            <span className={`relative inline-flex rounded-full h-3 w-3 ${statusConfig.dotColor}`}></span>
          </span>
        </div>
      </div>

      {/* Status badge */}
      <div className="mb-4">
        <div className={`inline-flex items-center space-x-2 px-3 py-1 rounded-full ${statusConfig.iconColor} bg-white dark:bg-gray-800 shadow-sm`}>
          {statusConfig.icon}
          <span className="text-sm font-semibold">{statusConfig.text}</span>
        </div>
      </div>

      {/* Last seen */}
      <div className="flex items-center justify-between text-sm">
        <span className="text-gray-600 dark:text-gray-400">Last seen</span>
        <span className="font-medium text-gray-900 dark:text-white">
          {formatLastSeen(device.lastHeartbeat)}
        </span>
      </div>

      {/* Error message if any */}
      {device.lastError && (
        <div className="mt-3 p-2 bg-red-100 dark:bg-red-900/30 border border-red-200 dark:border-red-800 rounded-lg">
          <p className="text-xs text-red-800 dark:text-red-300 font-medium">
            {device.lastError}
          </p>
        </div>
      )}
    </div>
  );
}

