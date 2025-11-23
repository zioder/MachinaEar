import React, { useState } from 'react';
import { Input } from '@/components/ui/Input';
import { Button } from '@/components/ui/Button';

interface TwoFactorManagementProps {
  onRegenerateCodes: (password: string) => Promise<void>;
  onDisable2FA: (password: string) => Promise<void>;
  loading: boolean;
}

export function TwoFactorManagement({
  onRegenerateCodes,
  onDisable2FA,
  loading,
}: TwoFactorManagementProps) {
  const [regenPassword, setRegenPassword] = useState('');
  const [disablePassword, setDisablePassword] = useState('');

  const handleRegenerateCodes = async (e: React.FormEvent) => {
    e.preventDefault();
    await onRegenerateCodes(regenPassword);
    setRegenPassword('');
  };

  const handleDisable2FA = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!confirm('Are you sure you want to disable two-factor authentication?')) {
      return;
    }
    
    await onDisable2FA(disablePassword);
    setDisablePassword('');
  };

  return (
    <div className="border-t border-gray-200 dark:border-gray-700 pt-6 mt-6">
      <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">
        Manage Two-Factor Authentication
      </h3>

      <div className="space-y-6">
        {/* Regenerate Recovery Codes */}
        <form onSubmit={handleRegenerateCodes} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
              Regenerate Recovery Codes
            </label>
            <p className="text-sm text-gray-500 dark:text-gray-400 mb-2">
              Generate new recovery codes (this will invalidate old codes)
            </p>
            <div className="flex items-center gap-4">
              <Input
                type="password"
                value={regenPassword}
                onChange={(e) => setRegenPassword(e.target.value)}
                placeholder="Enter your password"
                required
                className="flex-1 max-w-xs"
              />
              <Button type="submit" disabled={loading}>
                Regenerate
              </Button>
            </div>
          </div>
        </form>

        {/* Disable 2FA */}
        <form onSubmit={handleDisable2FA} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
              Disable Two-Factor Authentication
            </label>
            <p className="text-sm text-gray-500 dark:text-gray-400 mb-2">
              Turn off 2FA for your account
            </p>
            <div className="flex items-center gap-4">
              <Input
                type="password"
                value={disablePassword}
                onChange={(e) => setDisablePassword(e.target.value)}
                placeholder="Enter your password"
                required
                className="flex-1 max-w-xs"
              />
              <Button type="submit" variant="danger" disabled={loading}>
                Disable 2FA
              </Button>
            </div>
          </div>
        </form>
      </div>
    </div>
  );
}
