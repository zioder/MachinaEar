import React, { useState } from 'react';
import { Input } from '@/components/ui/Input';
import { Button } from '@/components/ui/Button';

interface VerificationFormProps {
  onVerify: (code: string) => Promise<void>;
  loading: boolean;
}

export function VerificationForm({ onVerify, loading }: VerificationFormProps) {
  const [verificationCode, setVerificationCode] = useState('');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    await onVerify(verificationCode);
  };

  return (
    <form onSubmit={handleSubmit}>
      <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">
        Step 2: Verify Code
      </h3>
      <p className="text-gray-700 dark:text-gray-300 mb-4">
        Enter the 6-digit code from your authenticator app to complete setup:
      </p>
      <div className="flex items-center gap-4">
        <input
          type="text"
          value={verificationCode}
          onChange={(e) => setVerificationCode(e.target.value.replace(/\D/g, ''))}
          maxLength={6}
          pattern="[0-9]{6}"
          required
          placeholder="000000"
          className="w-32 px-3 py-2 border border-gray-300 dark:border-gray-700 rounded-md text-center text-lg font-mono focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
        />
        <Button
          type="submit"
          disabled={loading || verificationCode.length !== 6}
          loading={loading}
        >
          Verify & Enable
        </Button>
      </div>
    </form>
  );
}
