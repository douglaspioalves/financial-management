export interface RegisterRequest {
  name: string;
  email: string;
  password: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface AuthResponse {
  token: string;
  type: string;
  name: string;
  email: string;
}

export interface CurrentUser {
  name: string;
  email: string;
  token: string;
}
