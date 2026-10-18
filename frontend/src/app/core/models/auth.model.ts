export interface LoginRequest {
  username: string;
  password: string;
}

export interface AuthUserProfile {
  id: string;
  username: string;
  fullName: string;
  role: string;
}

export interface LoginResponse {
  token: string;
  expiresAt: string;
  user: AuthUserProfile;
}
