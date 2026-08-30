import { parseApiError } from './errors'
import { ensureCsrfToken, getCsrfHeaders, isUnsafeMethod } from './csrf'
import { getApiBaseUrl } from '../lib/env'

export type ApiRequestOptions = Omit<RequestInit, 'credentials' | 'body'> & {
  body?: unknown
}

function resolveUrl(path: string): string {
  if (path.startsWith('http://') || path.startsWith('https://')) {
    return path
  }

  const normalizedPath = path.startsWith('/') ? path : `/${path}`
  return `${getApiBaseUrl()}${normalizedPath}`
}

function buildHeaders(
  options: ApiRequestOptions,
  includeJsonContentType: boolean,
): Headers {
  const headers = new Headers(options.headers)

  if (includeJsonContentType && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }

  if (!headers.has('Accept')) {
    headers.set('Accept', 'application/json')
  }

  return headers
}

export async function apiFetch(
  path: string,
  options: ApiRequestOptions = {},
): Promise<Response> {
  const method = (options.method ?? 'GET').toUpperCase()

  if (isUnsafeMethod(method)) {
    await ensureCsrfToken()
  }

  const hasBody = options.body !== undefined && options.body !== null
  const headers = buildHeaders(options, hasBody)

  if (isUnsafeMethod(method)) {
    for (const [name, value] of Object.entries(getCsrfHeaders())) {
      headers.set(name, value)
    }
  }

  const { body, ...requestInit } = options

  return fetch(resolveUrl(path), {
    ...requestInit,
    method,
    credentials: 'include',
    headers,
    body: hasBody ? JSON.stringify(body) : undefined,
  })
}

export async function apiRequest<T>(path: string, options: ApiRequestOptions = {}): Promise<T> {
  const response = await apiFetch(path, options)

  if (response.status === 204) {
    return undefined as T
  }

  if (!response.ok) {
    throw await parseApiError(response)
  }

  const contentType = response.headers.get('content-type') ?? ''
  if (contentType.includes('application/json')) {
    return (await response.json()) as T
  }

  return undefined as T
}
