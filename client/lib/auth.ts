"use client";

import { jwtDecode } from "jwt-decode";
import type {
  LoginCredentials,
  RegisterCredentials,
  TokenPair,
  DecodedToken,
  User,
  LoginResult,
  TwoFactorSetup,
} from "@/types/auth";

const API_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080/iam-0.1.0/iam";

export class AuthService {
  private static ACCESS_TOKEN_KEY = "access_token";
  private static REFRESH_TOKEN_KEY = "refresh_token";

  static async register(credentials: RegisterCredentials): Promise<TokenPair> {
    const response = await fetch(`${API_URL}/auth/register`, {
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
    this.saveTokens(tokens);
    return tokens;
  }

  static async login(credentials: LoginCredentials): Promise<LoginResult> {
    const response = await fetch(`${API_URL}/auth/login`, {
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

    // Save tokens if authentication was successful
    if (result.authenticated && result.tokens) {
      this.saveTokens(result.tokens);
    }

    return result;
  }

  static async refreshToken(): Promise<TokenPair | null> {
    const refreshToken = this.getRefreshToken();
    if (!refreshToken) {
      return null;
    }

    try {
      const response = await fetch(`${API_URL}/auth/token`, {
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
        return null;
      }

      const tokens: TokenPair = await response.json();
      this.saveTokens(tokens);
      return tokens;
    } catch (error) {
      this.clearTokens();
      return null;
    }
  }

  static logout(): void {
    this.clearTokens();
  }

  static saveTokens(tokens: TokenPair): void {
    if (typeof window !== "undefined") {
      localStorage.setItem(this.ACCESS_TOKEN_KEY, tokens.accessToken);
      localStorage.setItem(this.REFRESH_TOKEN_KEY, tokens.refreshToken);
    }
  }

  static getAccessToken(): string | null {
    if (typeof window !== "undefined") {
      return localStorage.getItem(this.ACCESS_TOKEN_KEY);
    }
    return null;
  }

  static getRefreshToken(): string | null {
    if (typeof window !== "undefined") {
      return localStorage.getItem(this.REFRESH_TOKEN_KEY);
    }
    return null;
  }

  static clearTokens(): void {
    if (typeof window !== "undefined") {
      localStorage.removeItem(this.ACCESS_TOKEN_KEY);
      localStorage.removeItem(this.REFRESH_TOKEN_KEY);
    }
  }

  static getCurrentUser(): User | null {
    const token = this.getAccessToken();
    if (!token) {
      return null;
    }

    try {
      const decoded = jwtDecode<DecodedToken>(token);
      console.log("Decoded token:", decoded);

      // Check if token is expired
      if (decoded.exp * 1000 < Date.now()) {
        return null;
      }

      const user = {
        email: decoded.sub,
        username: decoded.username || "",
        roles: decoded.roles || [],
      };
      console.log("User object created:", user);
      return user;
    } catch (error) {
      console.error("Error decoding token:", error);
      return null;
    }
  }

  static isAuthenticated(): boolean {
    return this.getCurrentUser() !== null;
  }

  // 2FA Management Methods
  static async setup2FA(email: string): Promise<TwoFactorSetup> {
    const accessToken = this.getAccessToken();
    const response = await fetch(`${API_URL}/auth/2fa/setup`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${accessToken}`,
      },
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
    const accessToken = this.getAccessToken();
    const response = await fetch(`${API_URL}/auth/2fa/enable`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${accessToken}`,
      },
      body: JSON.stringify({ email, secret, verificationCode, recoveryCodes }),
    });

    if (!response.ok) {
      const error = await response.text();
      throw new Error(error || "Failed to enable 2FA");
    }
  }

  static async disable2FA(email: string, password: string): Promise<void> {
    const accessToken = this.getAccessToken();
    const response = await fetch(`${API_URL}/auth/2fa/disable`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${accessToken}`,
      },
      body: JSON.stringify({ email, password }),
    });

    if (!response.ok) {
      const error = await response.text();
      throw new Error(error || "Failed to disable 2FA");
    }
  }

  static async regenerateRecoveryCodes(email: string, password: string): Promise<string[]> {
    const accessToken = this.getAccessToken();
    const response = await fetch(`${API_URL}/auth/2fa/regenerate-codes`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${accessToken}`,
      },
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
