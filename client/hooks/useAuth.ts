import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { AuthService } from '@/lib/auth';
import type { User, LoginCredentials, RegisterCredentials } from '@/types/auth';

export function useAuth() {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const router = useRouter();

  // Load current user on mount
  useEffect(() => {
    const loadUser = async () => {
      try {
        const currentUser = await AuthService.getCurrentUser();
        setUser(currentUser);
      } catch (err) {
        console.error('Error loading user:', err);
        // Don't set error state for network errors during initial load
        // This allows the app to function in offline mode
        setUser(null);
      } finally {
        setLoading(false);
      }
    };

    loadUser();
  }, []);

  const login = async (credentials: LoginCredentials, returnTo?: string) => {
    setLoading(true);
    setError(null);

    try {
      const result = await AuthService.login(credentials);

      // Check if 2FA is required
      if (result.twoFactorEnabled && !result.authenticated) {
        setLoading(false);
        return { requires2FA: true, success: false };
      }

      // Login successful - update user state
      const currentUser = await AuthService.getCurrentUser();
      setUser(currentUser);

      // Redirect to returnTo or home
      if (returnTo) {
        window.location.href = `https://localhost:8443/iam-0.1.0/iam${returnTo}`;
      } else {
        router.push('/home');
      }

      return { requires2FA: false, success: true };
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Login failed';
      setError(errorMessage);
      return { requires2FA: false, success: false, error: errorMessage };
    } finally {
      setLoading(false);
    }
  };

  const register = async (credentials: RegisterCredentials) => {
    setLoading(true);
    setError(null);

    try {
      await AuthService.register(credentials);
      
      // Get updated user
      const currentUser = await AuthService.getCurrentUser();
      setUser(currentUser);
      
      router.push('/home');
      return { success: true };
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Registration failed';
      setError(errorMessage);
      return { success: false, error: errorMessage };
    } finally {
      setLoading(false);
    }
  };

  const logout = async () => {
    setLoading(true);
    try {
      await AuthService.logout();
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
      const currentUser = await AuthService.getCurrentUser();
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
    error,
    login,
    register,
    logout,
    refreshUser,
    isAuthenticated: !!user,
  };
}
