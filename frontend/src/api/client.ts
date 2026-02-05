import { createApiFetch, resolveApiBase } from '@shared/api/client';
import { clearAuth, getToken } from '../auth/storage';

export const API_BASE = resolveApiBase(import.meta.env.VITE_API_BASE);

export const apiFetch = createApiFetch({
  apiBase: API_BASE,
  getToken,
  onUnauthorized: () => {
    clearAuth();
    if (typeof window !== 'undefined') {
      window.location.href = '/login';
    }
  }
});
