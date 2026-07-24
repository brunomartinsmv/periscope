import { ApiError, type ApiErrorBody } from '../types';

const TOKEN_KEY = 'periscope.token';
const USER_KEY = 'periscope.user';

export function getApiBase(): string {
  const base = import.meta.env.VITE_API_BASE || '/periscope/rest';
  return base.replace(/\/$/, '');
}

export function getStoredToken(): string | null {
  return localStorage.getItem(TOKEN_KEY);
}

export function setStoredToken(token: string | null): void {
  if (token) {
    localStorage.setItem(TOKEN_KEY, token);
  } else {
    localStorage.removeItem(TOKEN_KEY);
  }
}

export function getStoredUserJson(): string | null {
  return localStorage.getItem(USER_KEY);
}

export function setStoredUserJson(json: string | null): void {
  if (json) {
    localStorage.setItem(USER_KEY, json);
  } else {
    localStorage.removeItem(USER_KEY);
  }
}

export function clearSession(): void {
  setStoredToken(null);
  setStoredUserJson(null);
}

type UnauthorizedHandler = () => void;
let onUnauthorized: UnauthorizedHandler | null = null;

export function setUnauthorizedHandler(handler: UnauthorizedHandler | null): void {
  onUnauthorized = handler;
}

export interface RequestOptions extends Omit<RequestInit, 'body'> {
  body?: unknown;
  auth?: boolean;
  formData?: FormData;
}

async function parseError(response: Response): Promise<ApiError> {
  let body: ApiErrorBody | null = null;
  let message = response.statusText || `HTTP ${response.status}`;
  try {
    const text = await response.text();
    if (text) {
      try {
        body = JSON.parse(text) as ApiErrorBody;
        message = body.error || body.message || message;
      } catch {
        message = text.slice(0, 200);
      }
    }
  } catch {
    // ignore
  }
  return new ApiError(response.status, message, body);
}

export async function apiRequest<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { body, auth = true, formData, headers: customHeaders, ...rest } = options;
  const headers = new Headers(customHeaders);

  if (auth) {
    const token = getStoredToken();
    if (token) {
      headers.set('Authorization', `Bearer ${token}`);
    }
  }

  let payload: BodyInit | undefined;
  if (formData) {
    payload = formData;
  } else if (body !== undefined) {
    headers.set('Content-Type', 'application/json');
    payload = JSON.stringify(body);
  }

  const url = path.startsWith('http') ? path : `${getApiBase()}${path.startsWith('/') ? path : `/${path}`}`;
  const response = await fetch(url, {
    ...rest,
    headers,
    body: payload,
  });

  if (response.status === 401) {
    clearSession();
    onUnauthorized?.();
    throw await parseError(response);
  }

  if (!response.ok) {
    throw await parseError(response);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  const contentType = response.headers.get('Content-Type') || '';
  if (contentType.includes('application/json')) {
    return (await response.json()) as T;
  }

  return (await response.blob()) as T;
}
