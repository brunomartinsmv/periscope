import { apiRequest } from './client';
import type { LoginRequest, LoginResponse, User } from '../types';

export function login(credentials: LoginRequest): Promise<LoginResponse> {
  return apiRequest<LoginResponse>('/auth/login', {
    method: 'POST',
    body: credentials,
    auth: false,
  });
}

export function me(): Promise<User> {
  return apiRequest<User>('/auth/me');
}

export function logout(): Promise<void> {
  return apiRequest<void>('/auth/logout', { method: 'POST' });
}
