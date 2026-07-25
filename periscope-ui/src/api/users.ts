import { apiRequest } from './client';
import type { User } from '../types';

export function listUsers(): Promise<User[]> {
  return apiRequest<User[]>('/users');
}
