import { useState } from 'react';
import { AuthService } from '@/lib/auth';
import type { TwoFactorSetup } from '@/types/auth';

export function use2FA() {
  const [setup, setSetup] = useState<TwoFactorSetup | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const initiate2FASetup = async (email: string) => {
    setLoading(true);
    setError(null);

    try {
      const setupData = await AuthService.setup2FA(email);
      setSetup(setupData);
      return { success: true, data: setupData };
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to setup 2FA';
      setError(errorMessage);
      return { success: false, error: errorMessage };
    } finally {
      setLoading(false);
    }
  };

  const enable2FA = async (
    email: string,
    secret: string,
    verificationCode: number,
    recoveryCodes: string[]
  ) => {
    setLoading(true);
    setError(null);

    try {
      await AuthService.enable2FA(email, secret, verificationCode, recoveryCodes);
      return { success: true };
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to enable 2FA';
      setError(errorMessage);
      return { success: false, error: errorMessage };
    } finally {
      setLoading(false);
    }
  };

  const disable2FA = async (email: string, password: string) => {
    setLoading(true);
    setError(null);

    try {
      await AuthService.disable2FA(email, password);
      setSetup(null);
      return { success: true };
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to disable 2FA';
      setError(errorMessage);
      return { success: false, error: errorMessage };
    } finally {
      setLoading(false);
    }
  };

  const regenerateRecoveryCodes = async (email: string, password: string) => {
    setLoading(true);
    setError(null);

    try {
      const newCodes = await AuthService.regenerateRecoveryCodes(email, password);
      if (setup) {
        setSetup({ ...setup, recoveryCodes: newCodes });
      }
      return { success: true, codes: newCodes };
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to regenerate recovery codes';
      setError(errorMessage);
      return { success: false, error: errorMessage };
    } finally {
      setLoading(false);
    }
  };

  return {
    setup,
    loading,
    error,
    initiate2FASetup,
    enable2FA,
    disable2FA,
    regenerateRecoveryCodes,
  };
}
