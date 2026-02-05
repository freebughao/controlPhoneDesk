import React, { createContext, useCallback, useContext, useMemo, useState } from 'react';
import type { UserInfo } from '../types';
import { clearAuth, getToken, getUser, setToken, setUser } from './storage';

type AuthState = {
  token: string;
  user: UserInfo | null;
};

type AuthContextValue = AuthState & {
  login: (token: string, user: UserInfo) => void;
  logout: () => void;
  refreshUser: (user: UserInfo) => void;
};

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [tokenState, setTokenState] = useState(getToken());
  const [userState, setUserState] = useState<UserInfo | null>(getUser());

  const login = useCallback((token: string, user: UserInfo) => {
    setToken(token);
    setUser(user);
    setTokenState(token);
    setUserState(user);
  }, []);

  const logout = useCallback(() => {
    clearAuth();
    setTokenState('');
    setUserState(null);
  }, []);

  const refreshUser = useCallback((user: UserInfo) => {
    setUser(user);
    setUserState(user);
  }, []);

  const value = useMemo<AuthContextValue>(() => ({
    token: tokenState,
    user: userState,
    login,
    logout,
    refreshUser
  }), [login, logout, refreshUser, tokenState, userState]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider');
  }
  return context;
};
