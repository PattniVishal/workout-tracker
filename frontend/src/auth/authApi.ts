import { apiFetch, apiRequest } from '../api/client'
import { parseApiError } from '../api/errors'
import type { LoginRequest, RegisterRequest, UserResponse } from '../api/types'

export async function fetchCurrentUser(): Promise<UserResponse | null> {
  const response = await apiFetch('/auth/me')

  if (response.status === 401) {
    return null
  }

  if (!response.ok) {
    throw await parseApiError(response)
  }

  return (await response.json()) as UserResponse
}

export function register(request: RegisterRequest): Promise<UserResponse> {
  return apiRequest<UserResponse>('/auth/register', {
    method: 'POST',
    body: request,
  })
}

export function login(request: LoginRequest): Promise<UserResponse> {
  return apiRequest<UserResponse>('/auth/login', {
    method: 'POST',
    body: request,
  })
}

export function logout(): Promise<void> {
  return apiRequest<void>('/auth/logout', {
    method: 'POST',
  })
}
