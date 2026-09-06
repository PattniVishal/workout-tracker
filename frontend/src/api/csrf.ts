import { getApiBaseUrl } from '../lib/env'
import type { CsrfTokenResponse } from './types'

const DEFAULT_CSRF_HEADER = 'X-XSRF-TOKEN'

const UNSAFE_METHODS = new Set(['POST', 'PUT', 'PATCH', 'DELETE'])

let csrfHeaderName = DEFAULT_CSRF_HEADER
let csrfToken: string | null = null
let bootstrapPromise: Promise<void> | null = null

export function isUnsafeMethod(method: string): boolean {
  return UNSAFE_METHODS.has(method.toUpperCase())
}

export function getCsrfHeaderName(): string {
  return csrfHeaderName
}

export function getCsrfToken(): string | null {
  return csrfToken
}

export function clearCsrfToken(): void {
  csrfToken = null
  bootstrapPromise = null
}

export async function ensureCsrfToken(): Promise<void> {
  if (csrfToken) {
    return
  }

  if (!bootstrapPromise) {
    bootstrapPromise = bootstrapCsrfToken().finally(() => {
      bootstrapPromise = null
    })
  }

  await bootstrapPromise
}

async function bootstrapCsrfToken(): Promise<void> {
  const response = await fetch(`${getApiBaseUrl()}/auth/csrf`, {
    method: 'GET',
    credentials: 'include',
    headers: {
      Accept: 'application/json',
    },
  })

  if (!response.ok) {
    throw new Error(`Failed to bootstrap CSRF token (${response.status}).`)
  }

  const body = (await response.json()) as CsrfTokenResponse
  csrfHeaderName = body.headerName || DEFAULT_CSRF_HEADER
  csrfToken = body.token
}

export function getCsrfHeaders(): Record<string, string> {
  if (!csrfToken) {
    return {}
  }

  return {
    [csrfHeaderName]: csrfToken,
  }
}
