/**
 * OAuth 2.0 Authorization Code Flow with PKCE
 */

import { generateCodeVerifier, generateCodeChallenge, generateState, storePKCEParams, getPKCEParams, clearPKCEParams } from './pkce';
import { API_ENDPOINTS, OAUTH_CLIENT_ID, OAUTH_REDIRECT_URI } from './constants';

// Note: Tokens are now stored in httpOnly cookies
// These interfaces are kept for backend response compatibility
export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
}

/**
 * Initiates the OAuth 2.0 authorization code flow with PKCE
 * Redirects user to the authorization endpoint
 * @param mode - Optional mode to pass to IAM ('login' or 'register')
 */
export async function initiateOAuthFlow(mode?: 'login' | 'register'): Promise<void> {
  // Generate PKCE parameters
  const codeVerifier = generateCodeVerifier();
  const codeChallenge = await generateCodeChallenge(codeVerifier);
  const state = generateState();

  // Store for later use in callback
  storePKCEParams(codeVerifier, state);

  // Build authorization URL
  const params = new URLSearchParams({
    response_type: 'code',
    client_id: OAUTH_CLIENT_ID,
    redirect_uri: OAUTH_REDIRECT_URI,
    code_challenge: codeChallenge,
    code_challenge_method: 'S256',
    state: state,
  });

  // Add mode parameter if specified
  if (mode) {
    params.set('mode', mode);
  }

  const authorizationUrl = `${API_ENDPOINTS.AUTHORIZE}?${params.toString()}`;

  // Redirect to authorization endpoint
  window.location.href = authorizationUrl;
}

/**
 * Initiates Google OAuth flow
 * Redirects to backend Google OAuth endpoint which then redirects to Google
 * The backend stores state and original OAuth params to resume flow after Google callback
 */
export async function initiateGoogleOAuthFlow(): Promise<void> {
  // Generate PKCE parameters (for eventual OAuth code exchange)
  const codeVerifier = generateCodeVerifier();
  const codeChallenge = await generateCodeChallenge(codeVerifier);
  const state = generateState();

  // Store for later use
  storePKCEParams(codeVerifier, state);

  // Build URL to backend Google OAuth initiator
  // Pass original OAuth params so backend can resume authorization flow after Google auth
  const params = new URLSearchParams({
    client_id: OAUTH_CLIENT_ID,
    redirect_uri: OAUTH_REDIRECT_URI,
    code_challenge: codeChallenge,
    code_challenge_method: 'S256',
    state: state,
  });

  const googleOAuthUrl = `${API_ENDPOINTS.GOOGLE_OAUTH_LOGIN}?${params.toString()}`;

  // Redirect to backend (which will redirect to Google)
  window.location.href = googleOAuthUrl;
}

/**
 * Exchanges authorization code for tokens
 * Called from the callback page after redirect
 * Note: Tokens are set as httpOnly cookies by the backend
 */
export async function exchangeCodeForTokens(code: string, receivedState: string): Promise<void> {
  // Retrieve stored PKCE parameters
  const { verifier, state } = getPKCEParams();

  if (!verifier) {
    throw new Error('Code verifier not found. Please initiate login again.');
  }

  // Validate state parameter (CSRF protection)
  if (state !== receivedState) {
    clearPKCEParams();
    throw new Error('Invalid state parameter. Possible CSRF attack.');
  }

  // Exchange authorization code for tokens
  const body = new URLSearchParams({
    grant_type: 'authorization_code',
    code: code,
    client_id: OAUTH_CLIENT_ID,
    redirect_uri: OAUTH_REDIRECT_URI,
    code_verifier: verifier,
  });

  const response = await fetch(API_ENDPOINTS.TOKEN, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
    body: body.toString(),
    credentials: 'include', // Important: include cookies
  });

  // Clear PKCE parameters after use
  clearPKCEParams();

  if (!response.ok) {
    const errorData = await response.json().catch(() => ({}));
    throw new Error(errorData.error_description || errorData.error || 'Token exchange failed');
  }

  // Tokens are now in httpOnly cookies - no need to save to localStorage
  // Just verify the response was successful
  await response.json(); // Consume the response body
}

/**
 * Refreshes the access token using the refresh token stored in httpOnly cookie
 * The backend automatically reads the refresh token from the cookie
 */
export async function refreshAccessToken(): Promise<boolean> {
  try {
    const response = await fetch(API_ENDPOINTS.TOKEN, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
      },
      body: new URLSearchParams({
        grant_type: 'refresh_token',
      }).toString(),
      credentials: 'include', // Browser automatically sends httpOnly cookies
    });

    if (!response.ok) {
      return false;
    }

    // New tokens are set as httpOnly cookies by backend
    await response.json(); // Consume response
    return true;
  } catch (error) {
    console.error('Token refresh failed:', error);
    return false;
  }
}

/**
 * Logs out the user by calling backend to clear httpOnly cookies
 */
export async function logout(): Promise<void> {
  try {
    await fetch(API_ENDPOINTS.LOGOUT, {
      method: 'POST',
      credentials: 'include',
    });
  } catch (error) {
    console.error('Logout error:', error);
  }

  // Clear any client-side PKCE state
  clearPKCEParams();
}
