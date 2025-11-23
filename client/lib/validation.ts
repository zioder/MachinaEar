/**
 * Form validation utilities
 */

interface ValidationResult {
  valid: boolean;
  error?: string;
}

/**
 * Validate email format
 */
export function validateEmail(email: string): ValidationResult {
  if (!email) {
    return { valid: false, error: 'Email is required' };
  }

  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  if (!emailRegex.test(email)) {
    return { valid: false, error: 'Invalid email format' };
  }

  return { valid: true };
}

/**
 * Validate password strength
 */
export function validatePassword(password: string): ValidationResult {
  if (!password) {
    return { valid: false, error: 'Password is required' };
  }

  if (password.length < 12) {
    return { valid: false, error: 'Password must be at least 12 characters long' };
  }

  const hasLower = /[a-z]/.test(password);
  const hasUpper = /[A-Z]/.test(password);
  const hasDigit = /[0-9]/.test(password);
  const hasSpecial = /[^a-zA-Z0-9]/.test(password);
  const varietyCount = [hasLower, hasUpper, hasDigit, hasSpecial].filter(Boolean).length;

  if (varietyCount < 3) {
    return {
      valid: false,
      error: 'Password must contain at least 3 of: lowercase letters, uppercase letters, numbers, special characters',
    };
  }

  return { valid: true };
}

/**
 * Validate password confirmation
 */
export function validatePasswordMatch(password: string, confirmPassword: string): ValidationResult {
  if (!confirmPassword) {
    return { valid: false, error: 'Please confirm your password' };
  }

  if (password !== confirmPassword) {
    return { valid: false, error: 'Passwords do not match' };
  }

  return { valid: true };
}

/**
 * Validate username
 */
export function validateUsername(username: string): ValidationResult {
  if (!username) {
    return { valid: false, error: 'Username is required' };
  }

  if (username.length < 3) {
    return { valid: false, error: 'Username must be at least 3 characters long' };
  }

  if (username.length > 30) {
    return { valid: false, error: 'Username must be less than 30 characters' };
  }

  const usernameRegex = /^[a-zA-Z0-9_-]+$/;
  if (!usernameRegex.test(username)) {
    return {
      valid: false,
      error: 'Username can only contain letters, numbers, underscores, and hyphens',
    };
  }

  return { valid: true };
}

/**
 * Validate 2FA code
 */
export function validate2FACode(code: string): ValidationResult {
  if (!code) {
    return { valid: false, error: '2FA code is required' };
  }

  if (!/^\d{6}$/.test(code)) {
    return { valid: false, error: '2FA code must be 6 digits' };
  }

  return { valid: true };
}

/**
 * Get password strength level
 */
export function getPasswordStrength(password: string): {
  level: 'weak' | 'medium' | 'strong' | 'very-strong';
  score: number;
  feedback: string;
} {
  let score = 0;
  
  // Length
  if (password.length >= 12) score += 1;
  if (password.length >= 16) score += 1;
  if (password.length >= 20) score += 1;
  
  // Character variety
  if (/[a-z]/.test(password)) score += 1;
  if (/[A-Z]/.test(password)) score += 1;
  if (/[0-9]/.test(password)) score += 1;
  if (/[^a-zA-Z0-9]/.test(password)) score += 1;
  
  // Common patterns (negative points)
  if (/^[a-z]+$/.test(password)) score -= 1;
  if (/^[0-9]+$/.test(password)) score -= 1;
  if (/(.)\1{2,}/.test(password)) score -= 1; // Repeated characters
  
  let level: 'weak' | 'medium' | 'strong' | 'very-strong';
  let feedback: string;
  
  if (score < 3) {
    level = 'weak';
    feedback = 'Add more characters and variety';
  } else if (score < 5) {
    level = 'medium';
    feedback = 'Good, but could be stronger';
  } else if (score < 7) {
    level = 'strong';
    feedback = 'Strong password';
  } else {
    level = 'very-strong';
    feedback = 'Excellent password';
  }
  
  return { level, score, feedback };
}
