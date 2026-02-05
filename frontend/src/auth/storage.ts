import { createAuthStorage } from '@shared/auth/storage';

const storage = createAuthStorage('admin');

export const { getToken, setToken, getUser, setUser, clearAuth } = storage;
