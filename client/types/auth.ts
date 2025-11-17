export interface LoginCredentials {
  email: string;
  password: string;
}

export interface RegisterCredentials {
  email: string;
  username: string;
  password: string;
  confirmPassword?: string;
}

export interface TokenPair {
  accessToken: string;
  refreshToken: string;
}

export interface DecodedToken {
  sub: string; // email
  exp: number;
  iat: number;
  typ?: string;
  roles?: string[];
  username?: string;
}

export interface User {
  email: string;
  username: string;
  roles: string[];
}
