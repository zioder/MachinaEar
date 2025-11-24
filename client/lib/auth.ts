"use client";

import type {
  LoginCredentials,
  RegisterCredentials,
  TokenPair,
  User,
  LoginResult,
  TwoFactorSetup,
} from "@/types/auth";
import { API_ENDPOINTS } from "@/lib/constants";

/**
 * AuthService handles authentication with httpOnly cookies
 * Tokens are automatically sent with requests via cookies
 * No manual token management needed - more secure against XSS
 */
export class AuthService {
  static async register(credentials: RegisterCredentials): Promise<TokenPair> {
    const response = await fetch(API_ENDPOINTS.REGISTER, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      credentials: "include", // Important: include cookies
      body: JSON.stringify({
        email: credentials.email,
        username: credentials.username,
        password: credentials.password,
      }),
    });

    if (!response.ok) {
      let error = await response.text();
      // Try to parse JSON error response
      try {
        const errorJson = JSON.parse(error);
        error = errorJson.message || errorJson.error || error;
      } catch (e) {
      // If not JSON, use the text as-is
      }
      throw new Error(error || "Registration failed");
    }

    const tokens: TokenPair = await response.json();
    // Tokens are now in httpOnly cookies - no need to store in localStorage
    return tokens;
  }

  static async login(credentials: LoginCredentials): Promise<LoginResult> {
    const response = await fetch(API_ENDPOINTS.LOGIN, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      credentials: "include", // Important: include cookies
      body: JSON.stringify(credentials),
    });

    if (!response.ok) {
      const error = await response.text();
      throw new Error(error || "Login failed");
    }

    const result: LoginResult = await response.json();
    // Tokens are now in httpOnly cookies - backend sets them automatically
    return result;
  }

  static async refreshToken(): Promise<boolean> {
    try {
      const response = await fetch(API_ENDPOINTS.REFRESH, {
        method: "POST",
        headers: {
          "Content-Type": "application/x-www-form-urlencoded",
        },
        body: new URLSearchParams({
          grant_type: "refresh_token",
        }),
        credentials: "include", // Refresh token sent automatically via cookie
      });

      if (!response.ok) {
        return false;
      }

      // Tokens are refreshed in cookies automatically
      return true;
    } catch (error) {
      return false;
    }
  }

  static async logout(): Promise<void> {
    try {
      // Call backend to clear cookies and session
      await fetch(API_ENDPOINTS.LOGOUT, {
        method: "POST",
        credentials: "include",
      });
    } catch (error) {
      console.error("Logout error:", error);
    }
  }

  /**
   * Gets current user by making an API call with the access token from cookie
   */
  static async getCurrentUser(): Promise<User | null> {
    try {
      const response = await fetch(API_ENDPOINTS.ME, {
        method: "GET",
        credentials: "include", // Token sent automatically via cookie
      });

      if (!response.ok) {
        // Token might be expired, try to refresh
        const refreshed = await this.refreshToken();
        if (refreshed) {
          // Retry with refreshed token
          return this.getCurrentUser();
        }
        return null;
      }

      const user: User = await response.json();
      return user;
    } catch (error) {
      console.error("Error getting current user:", error);
      return null;
    }
  }

  static async isAuthenticated(): Promise<boolean> {
    const user = await this.getCurrentUser();
    return user !== null;
  }

  // 2FA Management Methods
  // All methods now use httpOnly cookies for authentication
  static async setup2FA(email: string): Promise<TwoFactorSetup> {
    const response = await fetch(API_ENDPOINTS.SETUP_2FA, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      credentials: "include", // Token sent automatically via cookie
      body: JSON.stringify({ email }),
    });

    if (!response.ok) {
      const error = await response.text();
      throw new Error(error || "Failed to setup 2FA");
    }

    return await response.json();
  }

  static async enable2FA(
    email: string,
    secret: string,
    verificationCode: number,
    recoveryCodes: string[]
  ): Promise<void> {
    const response = await fetch(API_ENDPOINTS.ENABLE_2FA, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      credentials: "include", // Token sent automatically via cookie
      body: JSON.stringify({ email, secret, verificationCode, recoveryCodes }),
    });

    if (!response.ok) {
      const error = await response.text();
      throw new Error(error || "Failed to enable 2FA");
    }
  }

  static async disable2FA(email: string, password: string): Promise<void> {
    const response = await fetch(API_ENDPOINTS.DISABLE_2FA, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      credentials: "include", // Token sent automatically via cookie
      body: JSON.stringify({ email, password }),
    });

    if (!response.ok) {
      const error = await response.text();
      throw new Error(error || "Failed to disable 2FA");
    }
  }

  static async regenerateRecoveryCodes(email: string, password: string): Promise<string[]> {
    const response = await fetch(API_ENDPOINTS.REGENERATE_CODES, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      credentials: "include", // Token sent automatically via cookie
      body: JSON.stringify({ email, password }),
    });

    if (!response.ok) {
      const error = await response.text();
      throw new Error(error || "Failed to regenerate recovery codes");
    }

    const result = await response.json();
    return result.recoveryCodes;
  }
}
