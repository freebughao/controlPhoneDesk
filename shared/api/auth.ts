import type { UserInfo } from '../types';
import { resolveApiBase } from './client';

export type LoginResponse = {
  token: string;
  expiresAt: string;
  user: UserInfo;
};

export async function login(
  username: string,
  password: string,
  apiBase?: string
): Promise<LoginResponse> {
  const base = resolveApiBase(apiBase);
  const response = await fetch(`${base}/api/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password })
  });
  if (!response.ok) {
    const text = await response.text();
    throw new Error(text || '登录失败');
  }
  return (await response.json()) as LoginResponse;
}
