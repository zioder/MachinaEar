/**
 * User API functions using OAuth 2.1 authenticated requests
 * All requests use httpOnly cookies for authentication
 */

import { API_ENDPOINTS } from './constants';
import type { User, TwoFactorSetup } from '@/types/auth';
import { refreshAccessToken } from './oauth';

/**
 * Gets the current authenticated user
 * Uses httpOnly cookie for authentication
 */
export async function getCurrentUser(): Promise<User | null> {
  try {
    const response = await fetch(API_ENDPOINTS.ME, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      },
      credentials: 'include', // Send httpOnly cookies
    });

    if (!response.ok) {
      // Token might be expired, try to refresh
      if (response.status === 401) {
        const refreshed = await refreshAccessToken();
        if (refreshed) {
          // Retry with refreshed token
          return getCurrentUser();
        }
      }
      return null;
    }

    const user: User = await response.json();
    return user;
  } catch (error) {
    console.error('Error getting current user:', error);
    return null;
  }
}

/**
 * Checks if user is authenticated by attempting to get current user
 */
export async function isAuthenticated(): Promise<boolean> {
  const user = await getCurrentUser();
  return user !== null;
}

// 2FA Management Functions

export async function setup2FA(email: string): Promise<TwoFactorSetup> {
  const response = await fetch(API_ENDPOINTS.SETUP_2FA, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ email }),
    credentials: 'include',
  });

  if (!response.ok) {
    const error = await response.text();
    throw new Error(error || 'Failed to setup 2FA');
  }

  return await response.json();
}

export async function enable2FA(
  email: string,
  secret: string,
  verificationCode: number,
  recoveryCodes: string[]
): Promise<void> {
  const response = await fetch(API_ENDPOINTS.ENABLE_2FA, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ email, secret, verificationCode, recoveryCodes }),
    credentials: 'include',
  });

  if (!response.ok) {
    const error = await response.text();
    throw new Error(error || 'Failed to enable 2FA');
  }
}

export async function disable2FA(email: string, password: string): Promise<void> {
  const response = await fetch(API_ENDPOINTS.DISABLE_2FA, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ email, password }),
    credentials: 'include',
  });

  if (!response.ok) {
    const error = await response.text();
    throw new Error(error || 'Failed to disable 2FA');
  }
}

export async function regenerateRecoveryCodes(email: string, password: string): Promise<string[]> {
  const response = await fetch(API_ENDPOINTS.REGENERATE_CODES, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ email, password }),
    credentials: 'include',
  });

  if (!response.ok) {
    const error = await response.text();
    throw new Error(error || 'Failed to regenerate recovery codes');
  }

  const result = await response.json();
  return result.recoveryCodes;
}
