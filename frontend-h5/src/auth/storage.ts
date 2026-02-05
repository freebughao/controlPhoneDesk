import { createAuthStorage } from '@shared/auth/storage';

const storage = createAuthStorage('h5');

export const { getToken, setToken, getUser, setUser, clearAuth } = storage;
