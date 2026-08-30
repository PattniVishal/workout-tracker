import { cleanup, render, screen, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { clearCsrfToken } from '../api/csrf'
import { AuthProvider } from './AuthProvider'
import { useAuth } from './useAuth'

vi.mock('./authApi', () => ({
  fetchCurrentUser: vi.fn(),
  login: vi.fn(),
  register: vi.fn(),
  logout: vi.fn(),
}))

import { fetchCurrentUser, logout as logoutRequest } from './authApi'

function AuthStatusProbe() {
  const { status, user, logout } = useAuth()
  return (
    <div>
      <span data-testid="status">{status}</span>
      <span data-testid="email">{user?.email ?? 'none'}</span>
      <button type="button" onClick={() => logout()}>
        Log out
      </button>
    </div>
  )
}

describe('AuthProvider', () => {
  let queryClient: QueryClient

  beforeEach(() => {
    queryClient = new QueryClient({
      defaultOptions: {
        queries: {
          retry: false,
        },
      },
    })
    vi.stubEnv('VITE_API_BASE_URL', 'http://localhost:8080/api')
    clearCsrfToken()
  })

  afterEach(() => {
    cleanup()
    queryClient.clear()
    vi.unstubAllGlobals()
    vi.restoreAllMocks()
    clearCsrfToken()
  })

  it('transitions from loading to unauthenticated when /me returns 401', async () => {
    vi.mocked(fetchCurrentUser).mockResolvedValue(null)

    render(
      <QueryClientProvider client={queryClient}>
        <AuthProvider>
          <AuthStatusProbe />
        </AuthProvider>
      </QueryClientProvider>,
    )

    await waitFor(() => {
      expect(screen.getByTestId('status')).toHaveTextContent('unauthenticated')
    })
    expect(screen.getByTestId('email')).toHaveTextContent('none')
  })

  it('transitions from loading to authenticated when /me succeeds', async () => {
    vi.mocked(fetchCurrentUser).mockResolvedValue({
      id: '11111111-1111-1111-1111-111111111111',
      displayName: 'Vishal',
      email: 'vishal@example.com',
    })

    render(
      <QueryClientProvider client={queryClient}>
        <AuthProvider>
          <AuthStatusProbe />
        </AuthProvider>
      </QueryClientProvider>,
    )

    await waitFor(() => {
      expect(screen.getByTestId('status')).toHaveTextContent('authenticated')
    })
    expect(screen.getByTestId('email')).toHaveTextContent('vishal@example.com')
  })

  it('restores session state through GET /api/auth/me on mount', async () => {
    vi.mocked(fetchCurrentUser).mockResolvedValue({
      id: '11111111-1111-1111-1111-111111111111',
      displayName: 'Vishal',
      email: 'vishal@example.com',
    })

    render(
      <QueryClientProvider client={queryClient}>
        <AuthProvider>
          <AuthStatusProbe />
        </AuthProvider>
      </QueryClientProvider>,
    )

    await waitFor(() => {
      expect(fetchCurrentUser).toHaveBeenCalledTimes(1)
      expect(screen.getByTestId('status')).toHaveTextContent('authenticated')
    })
  })

  it('clears authentication state after logout', async () => {
    vi.mocked(fetchCurrentUser).mockResolvedValue({
      id: '11111111-1111-1111-1111-111111111111',
      displayName: 'Vishal',
      email: 'vishal@example.com',
    })
    vi.mocked(logoutRequest).mockResolvedValue(undefined)

    const user = (await import('@testing-library/user-event')).default.setup()

    render(
      <QueryClientProvider client={queryClient}>
        <AuthProvider>
          <AuthStatusProbe />
        </AuthProvider>
      </QueryClientProvider>,
    )

    await waitFor(() => {
      expect(screen.getByTestId('status')).toHaveTextContent('authenticated')
    })

    await user.click(screen.getByRole('button', { name: 'Log out' }))

    await waitFor(() => {
      expect(logoutRequest).toHaveBeenCalledTimes(1)
      expect(screen.getByTestId('status')).toHaveTextContent('unauthenticated')
      expect(screen.getByTestId('email')).toHaveTextContent('none')
    })
  })
})
