import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { getCurrentUser } from '@/lib/user';
import { logout as oauthLogout } from '@/lib/oauth';
import type { User } from '@/types/auth';

/**
 * Authentication hook for OAuth 2.1
 * Manages user state and authentication status
 *
 * Note: For login, use initiateOAuthFlow() from @/lib/oauth
 * Login and registration are handled by the IAM server
 */
export function useAuth() {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);
  const router = useRouter();

  // Load current user on mount
  useEffect(() => {
    const loadUser = async () => {
      try {
        // Temporarily disable user loading to avoid auth errors
        // const currentUser = await getCurrentUser();
        // setUser(currentUser);
        setUser(null);
      } catch (err) {
        console.error('Error loading user:', err);
        setUser(null);
      } finally {
        setLoading(false);
      }
    };

    loadUser();
  }, []);

  const logout = async () => {
    setLoading(true);
    try {
      await oauthLogout();
      setUser(null);
      router.push('/');
    } catch (err) {
      console.error('Logout error:', err);
    } finally {
      setLoading(false);
    }
  };

  const refreshUser = async () => {
    try {
      const currentUser = await getCurrentUser();
      setUser(currentUser);
      return currentUser;
    } catch (err) {
      console.error('Error refreshing user:', err);
      return null;
    }
  };

  return {
    user,
    loading,
    logout,
    refreshUser,
    isAuthenticated: !!user,
  };
}
