import React from 'react';
import { Alert } from '@/components/ui/Alert';
import { Button } from '@/components/ui/Button';

interface RecoveryCodesDisplayProps {
  recoveryCodes: string[];
  onDownload: () => void;
}

export function RecoveryCodesDisplay({ recoveryCodes, onDownload }: RecoveryCodesDisplayProps) {
  return (
    <div>
      <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">
        Recovery Codes
      </h3>
      <Alert variant="warning">
        <p className="font-semibold mb-2">Save these recovery codes in a safe place!</p>
        <p>
          Each code can only be used once. You'll need them if you lose access to your authenticator app.
        </p>
      </Alert>
      
      <div className="bg-gray-100 dark:bg-gray-700 rounded-md p-4 my-4">
        <div className="grid grid-cols-2 gap-2 font-mono text-sm">
          {recoveryCodes.map((code, index) => (
            <div key={index} className="text-gray-900 dark:text-white">
              {index + 1}. {code}
            </div>
          ))}
        </div>
      </div>
      
      <Button variant="secondary" onClick={onDownload}>
        Download Recovery Codes
      </Button>
    </div>
  );
}
