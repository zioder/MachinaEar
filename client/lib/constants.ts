/**
 * API and application constants
 * Centralizes all configuration values
 */

// Backend API base URL
// Next.js will replace process.env.NEXT_PUBLIC_API_URL at build time
// If not set, use local development URL as fallback
export const API_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080/iam/iam";

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

  // 2FA
  SETUP_2FA: `${API_URL}/auth/2fa/setup`,
  ENABLE_2FA: `${API_URL}/auth/2fa/enable`,
  DISABLE_2FA: `${API_URL}/auth/2fa/disable`,
  REGENERATE_CODES: `${API_URL}/auth/2fa/regenerate-codes`,
} as const;

// Session storage keys
export const STORAGE_KEYS = {
  CODE_VERIFIER: "pkce_code_verifier",
  STATE: "pkce_state",
} as const;
