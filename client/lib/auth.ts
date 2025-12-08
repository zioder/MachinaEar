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
 * AuthService handles authentication with localStorage tokens
 * Tokens are stored in localStorage and sent in Authorization header
 * This approach works with cross-domain APIs (machinaear.me <-> iam.machinaear.me)
 */
export class AuthService {
  private static readonly ACCESS_TOKEN_KEY = "access_token";
  private static readonly REFRESH_TOKEN_KEY = "refresh_token";

  private static getAccessToken(): string | null {
    if (typeof window === "undefined") return null;
    return localStorage.getItem(this.ACCESS_TOKEN_KEY);
  }

  private static getRefreshToken(): string | null {
    if (typeof window === "undefined") return null;
    return localStorage.getItem(this.REFRESH_TOKEN_KEY);
  }

  private static setTokens(accessToken: string, refreshToken: string): void {
    if (typeof window === "undefined") return;
    localStorage.setItem(this.ACCESS_TOKEN_KEY, accessToken);
    localStorage.setItem(this.REFRESH_TOKEN_KEY, refreshToken);
  }

  private static clearTokens(): void {
    if (typeof window === "undefined") return;
    localStorage.removeItem(this.ACCESS_TOKEN_KEY);
    localStorage.removeItem(this.REFRESH_TOKEN_KEY);
  }

  private static getAuthHeaders(): HeadersInit {
    const token = this.getAccessToken();
    return token
      ? {
        "Content-Type": "application/json",
        Authorization: `Bearer ${token}`,
      }
      : {
        "Content-Type": "application/json",
      };
  }

  static async register(credentials: RegisterCredentials): Promise<TokenPair> {
    const response = await fetch(API_ENDPOINTS.REGISTER, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        email: credentials.email,
        username: credentials.username,
        password: credentials.password,
      }),
    });

    if (!response.ok) {
      let error = await response.text();
      try {
        const errorJson = JSON.parse(error);
        error = errorJson.message || errorJson.error || error;
      } catch (e) {
        // If not JSON, use the text as-is
      }
      throw new Error(error || "Registration failed");
    }

    const tokens: TokenPair = await response.json();
    // Store tokens in localStorage
    this.setTokens(tokens.accessToken, tokens.refreshToken);
    return tokens;
  }

  static async login(credentials: LoginCredentials): Promise<LoginResult> {
    const response = await fetch(API_ENDPOINTS.LOGIN, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(credentials),
    });

    if (!response.ok) {
      const error = await response.text();
      throw new Error(error || "Login failed");
    }

    const result: LoginResult = await response.json();
    // Store tokens in localStorage if authentication succeeded
    if (result.authenticated && result.tokens) {
      this.setTokens(result.tokens.accessToken, result.tokens.refreshToken);
    }
    return result;
  }

  static async refreshToken(): Promise<boolean> {
    try {
      const refreshToken = this.getRefreshToken();
      if (!refreshToken) return false;

      const response = await fetch(API_ENDPOINTS.REFRESH, {
        method: "POST",
        headers: {
          "Content-Type": "application/x-www-form-urlencoded",
        },
        body: new URLSearchParams({
          grant_type: "refresh_token",
          refresh_token: refreshToken,
        }),
      });

      if (!response.ok) {
        this.clearTokens();
        return false;
      }

      const tokens: TokenPair = await response.json();
      this.setTokens(tokens.accessToken, tokens.refreshToken);
      return true;
    } catch (error) {
      this.clearTokens();
      return false;
    }
  }

  static async logout(): Promise<void> {
    try {
      // Call backend to invalidate session
      await fetch(API_ENDPOINTS.LOGOUT, {
        method: "POST",
        headers: this.getAuthHeaders(),
      });
    } catch (error) {
      console.error("Logout error:", error);
    } finally {
      // Always clear local tokens
      this.clearTokens();
    }
  }

  /**
   * Gets current user by making an API call with the access token
   */
  static async getCurrentUser(): Promise<User | null> {
    try {
      const token = this.getAccessToken();
      if (!token) return null;

      const response = await fetch(API_ENDPOINTS.ME, {
        method: "GET",
        headers: this.getAuthHeaders(),
      });

      if (!response.ok) {
        // Token might be expired, try to refresh
        const refreshed = await this.refreshToken();
        if (refreshed) {
          // Retry with refreshed token
          return this.getCurrentUser();
        }
        this.clearTokens();
        return null;
      }

      const user: User = await response.json();
      return user;
    } catch (error) {
      console.error("Error getting current user:", error);
      this.clearTokens();
      return null;
    }
  }

  static async isAuthenticated(): Promise<boolean> {
    const user = await this.getCurrentUser();
    return user !== null;
  }

  // 2FA Management Methods
  static async setup2FA(email: string): Promise<TwoFactorSetup> {
    const response = await fetch(API_ENDPOINTS.SETUP_2FA, {
      method: "POST",
      headers: this.getAuthHeaders(),
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
      headers: this.getAuthHeaders(),
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
      headers: this.getAuthHeaders(),
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
      headers: this.getAuthHeaders(),
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
