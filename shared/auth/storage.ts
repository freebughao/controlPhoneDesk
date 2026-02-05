import type { UserInfo } from '../types';

type AuthStorage = {
  getToken: () => string;
  setToken: (token: string) => void;
  getUser: () => UserInfo | null;
  setUser: (user: UserInfo) => void;
  clearAuth: () => void;
};

export function createAuthStorage(namespace: string): AuthStorage {
  const tokenKey = `${namespace}_token`;
  const userKey = `${namespace}_user`;

  const getToken = () => {
    if (typeof window === 'undefined') return '';
    return window.localStorage.getItem(tokenKey) ?? '';
  };

  const setToken = (token: string) => {
    if (typeof window === 'undefined') return;
    window.localStorage.setItem(tokenKey, token);
  };

  const getUser = (): UserInfo | null => {
    if (typeof window === 'undefined') return null;
    const raw = window.localStorage.getItem(userKey);
    if (!raw) return null;
    try {
      return JSON.parse(raw) as UserInfo;
    } catch {
      return null;
    }
  };

  const setUser = (user: UserInfo) => {
    if (typeof window === 'undefined') return;
    window.localStorage.setItem(userKey, JSON.stringify(user));
  };

  const clearAuth = () => {
    if (typeof window === 'undefined') return;
    window.localStorage.removeItem(tokenKey);
    window.localStorage.removeItem(userKey);
  };

  return {
    getToken,
    setToken,
    getUser,
    setUser,
    clearAuth
  };
}
