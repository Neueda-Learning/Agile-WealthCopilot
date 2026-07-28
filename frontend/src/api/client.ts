import type { ApiErrorBody } from '../types/api';

const BASE = '/api/v1';
const TOKEN_KEY = 'wc.accessToken';

/**
 * Error carrying the backend's standard error body, so callers can branch on
 * `code` (AI_PARSE_FAILED, VALIDATION_FAILED, …) and surface `details[]`
 * against the right form field.
 */
export class ApiError extends Error {
  readonly status: number;
  readonly code: string;
  readonly details: NonNullable<ApiErrorBody['details']>;

  constructor(status: number, body: Partial<ApiErrorBody>) {
    super(body.message || 'Request failed');
    this.name = 'ApiError';
    this.status = status;
    this.code = body.code || 'INTERNAL_ERROR';
    this.details = body.details || [];
  }

  /** Field-keyed issues, for pinning messages to inputs. */
  fieldIssues(): Record<string, string> {
    const out: Record<string, string> = {};
    for (const d of this.details) if (d.field) out[d.field] = d.issue;
    return out;
  }
}

export const tokenStore = {
  get: (): string | null => localStorage.getItem(TOKEN_KEY),
  set: (t: string) => localStorage.setItem(TOKEN_KEY, t),
  clear: () => localStorage.removeItem(TOKEN_KEY),
};

/** Fired on any 401 so the auth layer can drop the session and redirect. */
const UNAUTHORIZED_EVENT = 'wc:unauthorized';
export function onUnauthorized(handler: () => void): () => void {
  window.addEventListener(UNAUTHORIZED_EVENT, handler);
  return () => window.removeEventListener(UNAUTHORIZED_EVENT, handler);
}

interface RequestOptions {
  method?: string;
  body?: unknown;
  query?: Record<string, string | number | undefined | null>;
  /** Login/register must not trigger the global 401 redirect. */
  anonymous?: boolean;
  signal?: AbortSignal;
}

export async function request<T>(path: string, opts: RequestOptions = {}): Promise<T> {
  const { method = 'GET', body, query, anonymous = false, signal } = opts;

  let url = BASE + path;
  if (query) {
    const qs = new URLSearchParams();
    for (const [k, v] of Object.entries(query)) {
      if (v !== undefined && v !== null && v !== '') qs.append(k, String(v));
    }
    const s = qs.toString();
    if (s) url += '?' + s;
  }

  const headers: Record<string, string> = {};
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  const token = tokenStore.get();
  if (token && !anonymous) headers['Authorization'] = `Bearer ${token}`;

  let res: Response;
  try {
    res = await fetch(url, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
      signal,
    });
  } catch (e) {
    if ((e as Error).name === 'AbortError') throw e;
    throw new ApiError(0, {
      code: 'NETWORK_ERROR',
      message: 'Could not reach the server. Check that the backend is running.',
    });
  }

  if (res.status === 204) return undefined as T;

  const text = await res.text();
  let parsed: unknown = null;
  if (text) {
    try {
      parsed = JSON.parse(text);
    } catch {
      // Non-JSON body (proxy error page, stack trace). Handled below.
    }
  }

  if (!res.ok) {
    if (res.status === 401 && !anonymous) {
      tokenStore.clear();
      window.dispatchEvent(new Event(UNAUTHORIZED_EVENT));
    }
    const body = (parsed && typeof parsed === 'object' ? parsed : {}) as Partial<ApiErrorBody>;
    throw new ApiError(res.status, {
      ...body,
      message: body.message || `Request failed (${res.status})`,
    });
  }

  return parsed as T;
}
