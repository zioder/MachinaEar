import React from 'react';
import Image from 'next/image';

interface QRCodeSetupProps {
  qrCodeImage: string;
  secret: string;
}

export function QRCodeSetup({ qrCodeImage, secret }: QRCodeSetupProps) {
  return (
    <div>
      <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">
        Step 1: Scan QR Code
      </h3>
      <p className="text-gray-700 dark:text-gray-300 mb-4">
        Scan this QR code with your authenticator app (It's highly recommended to use Authy):
      </p>
      <div className="bg-white p-4 rounded-lg inline-block">
        <img
          src={`data:image/png;base64,${qrCodeImage}`}
          alt="2FA QR Code"
          className="w-64 h-64"
        />
      </div>
      <p className="text-sm text-gray-500 dark:text-gray-400 mt-2">
        Or manually enter this secret: <code className="font-mono bg-gray-100 dark:bg-gray-800 px-2 py-1 rounded">{secret}</code>
      </p>
    </div>
  );
}
