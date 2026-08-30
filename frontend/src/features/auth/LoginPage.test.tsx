import { cleanup, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { LoginPage } from './LoginPage'

vi.mock('../../auth/authApi', () => ({
  login: vi.fn(),
  fetchCurrentUser: vi.fn(),
  register: vi.fn(),
  logout: vi.fn(),
}))

vi.mock('../../auth/useAuth', () => ({
  useAuth: vi.fn(),
}))

import { login } from '../../auth/authApi'
import { useAuth } from '../../auth/useAuth'
import { ApiError } from '../../api/errors'

describe('LoginPage', () => {
  const setUser = vi.fn()

  beforeEach(() => {
    vi.mocked(useAuth).mockReturnValue({
      status: 'unauthenticated',
      user: null,
      refreshUser: vi.fn(),
      setUser,
      logout: vi.fn(),
    })
  })

  afterEach(() => {
    cleanup()
    vi.restoreAllMocks()
  })

  it('renders the login form', () => {
    render(
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>,
    )

    expect(screen.getByRole('heading', { name: 'Sign in' })).toBeInTheDocument()
    expect(screen.getByLabelText('Email')).toBeInTheDocument()
    expect(screen.getByLabelText('Password')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Sign in' })).toBeInTheDocument()
  })

  it('updates auth state and redirects to the default route on successful login', async () => {
    const user = userEvent.setup()

    vi.mocked(login).mockResolvedValue({
      id: '11111111-1111-1111-1111-111111111111',
      displayName: 'Vishal',
      email: 'vishal@example.com',
    })

    render(
      <MemoryRouter initialEntries={['/login']}>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/" element={<div>Home page</div>} />
        </Routes>
      </MemoryRouter>,
    )

    await user.type(screen.getByLabelText('Email'), 'vishal@example.com')
    await user.type(screen.getByLabelText('Password'), 'secret-value')
    await user.click(screen.getByRole('button', { name: 'Sign in' }))

    await waitFor(() => {
      expect(login).toHaveBeenCalledWith({
        email: 'vishal@example.com',
        password: 'secret-value',
      })
    })

    expect(setUser).toHaveBeenCalledWith({
      id: '11111111-1111-1111-1111-111111111111',
      displayName: 'Vishal',
      email: 'vishal@example.com',
    })
    expect(screen.getByText('Home page')).toBeInTheDocument()
  })

  it('redirects to state.from after successful login when present', async () => {
    const user = userEvent.setup()

    vi.mocked(login).mockResolvedValue({
      id: '11111111-1111-1111-1111-111111111111',
      displayName: 'Vishal',
      email: 'vishal@example.com',
    })

    render(
      <MemoryRouter initialEntries={[{ pathname: '/login', state: { from: '/settings' } }]}>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/settings" element={<div>Settings page</div>} />
        </Routes>
      </MemoryRouter>,
    )

    await user.type(screen.getByLabelText('Email'), 'vishal@example.com')
    await user.type(screen.getByLabelText('Password'), 'secret-value')
    await user.click(screen.getByRole('button', { name: 'Sign in' }))

    await waitFor(() => {
      expect(screen.getByText('Settings page')).toBeInTheDocument()
    })
  })

  it('shows the backend error for invalid credentials', async () => {
    const user = userEvent.setup()

    vi.mocked(login).mockRejectedValue(
      new ApiError('Invalid email or password.', {
        status: 401,
        code: 'UNAUTHORIZED',
        message: 'Invalid email or password.',
        fieldErrors: [],
      }),
    )

    render(
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>,
    )

    await user.type(screen.getByLabelText('Email'), 'vishal@example.com')
    await user.type(screen.getByLabelText('Password'), 'wrong-password')
    await user.click(screen.getByRole('button', { name: 'Sign in' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Invalid email or password.')
    expect(setUser).not.toHaveBeenCalled()
  })
})
