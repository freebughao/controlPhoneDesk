export type ApiClientOptions = {
  apiBase?: string;
  getToken?: () => string;
  onUnauthorized?: () => void;
};

const resolveDefaultApiBase = () => {
  if (typeof window === 'undefined') {
    return 'http://localhost:8080';
  }
  return `${window.location.protocol}//${window.location.hostname}:8080`;
};

export function resolveApiBase(configured?: string): string {
  return configured ?? resolveDefaultApiBase();
}

export function createApiFetch(options: ApiClientOptions) {
  const base = resolveApiBase(options.apiBase);
  const getToken = options.getToken ?? (() => '');
  const onUnauthorized = options.onUnauthorized ?? (() => {});

  return async function apiFetch<T>(path: string, init: RequestInit = {}): Promise<T> {
    const token = getToken();
    const headers = new Headers(init.headers);
    if (init.body != null && !headers.has('Content-Type')) {
      headers.set('Content-Type', 'application/json');
    }
    if (token) {
      headers.set('Authorization', `Bearer ${token}`);
    }

    const response = await fetch(`${base}${path}`, {
      ...init,
      headers
    });

    if (response.status === 401) {
      onUnauthorized();
      throw new Error('Unauthorized');
    }

    if (!response.ok) {
      const text = await response.text();
      throw new Error(text || `HTTP ${response.status}`);
    }

    if (response.status === 204) {
      return undefined as T;
    }

    const text = await response.text();
    if (!text) {
      return undefined as T;
    }
    try {
      return JSON.parse(text) as T;
    } catch {
      throw new Error(text);
    }
  };
}
