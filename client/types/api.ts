

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

// Device entity
export interface Device {
  id?: string;
  name: string;
  type: string;
  status?: 'normal' | 'abnormal' | 'offline';
  lastHeartbeat?: string; // ISO timestamp
  temperature?: number;
  cpuUsage?: number;
  memoryUsage?: number;
  lastError?: string;
}