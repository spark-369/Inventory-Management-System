/**
 * Thin fetch wrapper around the Spring Boot REST API.
 *
 * Base URL resolution order:
 *  1. VITE_API_BASE_URL (explicit override, e.g. a deployed API)
 *  2. "/api" — the Vite dev proxy forwards this to http://localhost:8080
 */
const BASE_URL = import.meta.env.VITE_API_BASE_URL?.replace(/\/$/, '') ?? '/api'

/** Shape of the structured error body produced by GlobalExceptionHandler. */
interface ApiErrorBody {
  status?: number
  error?: string
  message?: string
  details?: Record<string, string>
}

/** Error thrown for any non-2xx response, carrying the server's message. */
export class ApiError extends Error {
  readonly status: number
  readonly details?: Record<string, string>

  constructor(message: string, status: number, details?: Record<string, string>) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.details = details
  }
}

async function parseError(response: Response): Promise<ApiError> {
  const raw = await response.text()
  if (!raw) {
    return new ApiError(`Request failed with status ${response.status}`, response.status)
  }

  try {
    const body = JSON.parse(raw) as ApiErrorBody
    const message = body.message || body.error || `Request failed with status ${response.status}`
    return new ApiError(message, response.status, body.details)
  } catch {
    // Non-JSON payload (e.g. a proxy or HTML error page).
    return new ApiError(raw.slice(0, 300), response.status)
  }
}

async function request<T>(endpoint: string, options?: RequestInit): Promise<T> {
  let response: Response
  try {
    response = await fetch(`${BASE_URL}${endpoint}`, {
      ...options,
      headers: {
        Accept: 'application/json',
        ...(options?.body ? { 'Content-Type': 'application/json' } : {}),
        ...(options?.headers as Record<string, string> | undefined),
      },
    })
  } catch {
    throw new ApiError('Cannot reach the server. Is the backend running?', 0)
  }

  if (!response.ok) {
    throw await parseError(response)
  }

  if (response.status === 204 || response.headers.get('content-length') === '0') {
    return undefined as T
  }

  const text = await response.text()
  return (text ? JSON.parse(text) : undefined) as T
}

/** Builds a `?a=1&b=2` query string, skipping empty values. */
function toQuery(params?: Record<string, string | number | boolean | undefined | null>): string {
  if (!params) return ''
  const search = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      search.append(key, String(value))
    }
  })
  const query = search.toString()
  return query ? `?${query}` : ''
}

export const api = {
  get: <T>(endpoint: string, params?: Record<string, string | number | boolean | undefined | null>) =>
    request<T>(`${endpoint}${toQuery(params)}`),
  post: <T>(endpoint: string, data?: unknown) =>
    request<T>(endpoint, { method: 'POST', body: data === undefined ? undefined : JSON.stringify(data) }),
  put: <T>(endpoint: string, data?: unknown) =>
    request<T>(endpoint, { method: 'PUT', body: data === undefined ? undefined : JSON.stringify(data) }),
  patch: <T>(endpoint: string, data?: unknown) =>
    request<T>(endpoint, { method: 'PATCH', body: data === undefined ? undefined : JSON.stringify(data) }),
  delete: (endpoint: string) => request<void>(endpoint, { method: 'DELETE' }),
}

/** Normalizes any thrown value into a display-safe message. */
export function getErrorMessage(error: unknown): string {
  if (error instanceof ApiError) return error.message
  if (error instanceof Error) return error.message
  return 'Something went wrong. Please try again.'
}
