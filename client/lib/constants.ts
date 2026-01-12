/**
 * API and application constants
 * Centralizes all configuration values
 */

// Backend API base URL
// Prefer explicit env; otherwise default to local dev path that matches the packaged context /iam-0.1.0/iam
export const API_URL = "https://iam.machinaear.me/iam";

// Frontend base URL
export const FRONTEND_URL =
  typeof window !== "undefined"
    ? window.location.origin
    : process.env.NEXT_PUBLIC_FRONTEND_URL || "https://localhost:3000";

// OAuth configuration
export const OAUTH_CLIENT_ID =
  process.env.NEXT_PUBLIC_OAUTH_CLIENT_ID || "machina-ear-web";

export const OAUTH_REDIRECT_URI = `${FRONTEND_URL}/auth/callback`;

// API endpoints
export const API_ENDPOINTS = {
  // Authentication
  REGISTER: `${API_URL}/auth/register`,
  LOGIN: `${API_URL}/auth/login`,
  LOGOUT: `${API_URL}/auth/logout`,
  ME: `${API_URL}/auth/me`,
  REFRESH: `${API_URL}/auth/token`, // For refresh token grant

  // OAuth
  AUTHORIZE: `${API_URL}/auth/authorize`,
  TOKEN: `${API_URL}/auth/token`,

  // Google OAuth
  GOOGLE_OAUTH_LOGIN: `${API_URL}/auth/google/login`,
  GOOGLE_OAUTH_CALLBACK: `${API_URL}/auth/google/callback`,

  // 2FA
  SETUP_2FA: `${API_URL}/auth/2fa/setup`,
  ENABLE_2FA: `${API_URL}/auth/2fa/enable`,
  DISABLE_2FA: `${API_URL}/auth/2fa/disable`,
  REGENERATE_CODES: `${API_URL}/auth/2fa/regenerate-codes`,

  // Email & Password
  VERIFY_EMAIL: `${API_URL}/auth/verify-email`,
  FORGOT_PASSWORD: `${API_URL}/auth/forgot-password`,
  RESET_PASSWORD: `${API_URL}/auth/reset-password`,
} as const;

// Session storage keys
export const STORAGE_KEYS = {
  CODE_VERIFIER: "pkce_code_verifier",
  STATE: "pkce_state",
} as const;
