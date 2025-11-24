/**
 * PKCE (Proof Key for Code Exchange) Utilities
 * Implements RFC 7636 for OAuth 2.0
 */

/**
 * Generates a cryptographically secure random string for code_verifier
 * @param length Length of the verifier (43-128 characters)
 * @returns Base64URL-encoded random string
 */
export function generateCodeVerifier(length: number = 128): string {
  const array = new Uint8Array(length);
  crypto.getRandomValues(array);
  return base64URLEncode(array);
}

/**
 * Generates code_challenge from code_verifier using SHA-256
 * @param verifier The code verifier
 * @returns Base64URL-encoded SHA-256 hash of verifier
 */
export async function generateCodeChallenge(verifier: string): Promise<string> {
  const encoder = new TextEncoder();
  const data = encoder.encode(verifier);
  const hash = await crypto.subtle.digest('SHA-256', data);
  return base64URLEncode(new Uint8Array(hash));
}

/**
 * Generates a random state parameter for CSRF protection
 * @returns Random state string
 */
export function generateState(): string {
  const array = new Uint8Array(32);
  crypto.getRandomValues(array);
  return base64URLEncode(array);
}

/**
 * Encodes a Uint8Array to Base64URL format (RFC 4648)
 * @param buffer The buffer to encode
 * @returns Base64URL-encoded string
 */
function base64URLEncode(buffer: Uint8Array): string {
  const base64 = btoa(String.fromCharCode(...buffer));
  return base64
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=/g, '');
}

/**
 * Stores PKCE parameters in sessionStorage for the OAuth flow
 */
export function storePKCEParams(verifier: string, state: string): void {
  if (typeof window !== 'undefined') {
    sessionStorage.setItem('pkce_code_verifier', verifier);
    sessionStorage.setItem('pkce_state', state);
  }
}

/**
 * Retrieves stored PKCE parameters from sessionStorage
 */
export function getPKCEParams(): { verifier: string | null; state: string | null } {
  if (typeof window !== 'undefined') {
    return {
      verifier: sessionStorage.getItem('pkce_code_verifier'),
      state: sessionStorage.getItem('pkce_state'),
    };
  }
  return { verifier: null, state: null };
}

/**
 * Clears PKCE parameters from sessionStorage
 */
export function clearPKCEParams(): void {
  if (typeof window !== 'undefined') {
    sessionStorage.removeItem('pkce_code_verifier');
    sessionStorage.removeItem('pkce_state');
  }
}
