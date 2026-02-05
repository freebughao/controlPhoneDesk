import { login as sharedLogin } from '@shared/api/auth';
import type { LoginResponse } from '@shared/api/auth';
import { API_BASE } from './client';

export type { LoginResponse };

export async function login(username: string, password: string): Promise<LoginResponse> {
  return sharedLogin(username, password, API_BASE);
}
