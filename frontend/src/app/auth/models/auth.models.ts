export interface AuthUser {
  id: string;
  email: string;
  createdAt: string;
}

export interface LoginCredentials {
  email: string;
  password: string;
}

export interface RegisterCredentials {
  email: string;
  password: string;
}

export interface AccessTokenResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
}