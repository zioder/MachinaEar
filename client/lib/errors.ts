/**
 * Centralized error handling utilities
 */

export class AppError extends Error {
  constructor(
    message: string,
    public statusCode?: number,
    public code?: string
  ) {
    super(message);
    this.name = 'AppError';
  }
}

export class AuthError extends AppError {
  constructor(message: string, statusCode?: number) {
    super(message, statusCode, 'AUTH_ERROR');
    this.name = 'AuthError';
  }
}

export class ValidationError extends AppError {
  constructor(message: string, public fields?: { [key: string]: string }) {
    super(message, 400, 'VALIDATION_ERROR');
    this.name = 'ValidationError';
  }
}

export class NetworkError extends AppError {
  constructor(message: string = 'Network request failed') {
    super(message, 0, 'NETWORK_ERROR');
    this.name = 'NetworkError';
  }
}

/**
 * Parse error from API response
 */
export async function parseApiError(response: Response): Promise<AppError> {
  let errorMessage = 'An error occurred';
  let errorCode: string | undefined;

  try {
    const contentType = response.headers.get('content-type');
    
    if (contentType && contentType.includes('application/json')) {
      const errorData = await response.json();
      errorMessage = errorData.message || errorData.error || errorMessage;
      errorCode = errorData.code;
    } else {
      errorMessage = await response.text();
    }
  } catch (e) {
    // If parsing fails, use default message
  }

  // Return appropriate error type based on status code
  if (response.status === 401 || response.status === 403) {
    return new AuthError(errorMessage, response.status);
  }

  if (response.status === 400) {
    return new ValidationError(errorMessage);
  }

  return new AppError(errorMessage, response.status, errorCode);
}

/**
 * Handle errors in a consistent way
 */
export function handleError(error: unknown): string {
  if (error instanceof AppError) {
    return error.message;
  }

  if (error instanceof Error) {
    return error.message;
  }

  if (typeof error === 'string') {
    return error;
  }

  return 'An unexpected error occurred';
}

/**
 * Log error for debugging (can be extended to send to monitoring service)
 */
export function logError(error: unknown, context?: string) {
  const errorMessage = handleError(error);
  const logContext = context ? `[${context}]` : '';
  
  console.error(`${logContext} Error:`, {
    message: errorMessage,
    error,
    timestamp: new Date().toISOString(),
  });
}
