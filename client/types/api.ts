/**
 * API response types for type safety
 */

// Generic API error response
export interface ApiError {
  error: string;
  message?: string;
  statusCode?: number;
}

// Generic API success response
export interface ApiSuccess<T = any> {
  data: T;
  message?: string;
}

// OAuth token response
export interface TokenResponse {
  access_token?: string;  // May not be in response (httpOnly cookie)
  refresh_token?: string; // May not be in response (httpOnly cookie)
  token_type?: string;
  expires_in?: number;
}

// OAuth error response
export interface OAuthError {
  error: string;
  error_description?: string;
  error_uri?: string;
}
