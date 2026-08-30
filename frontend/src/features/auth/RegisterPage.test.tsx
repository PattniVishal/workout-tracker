import { cleanup, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { RegisterPage } from './RegisterPage'

vi.mock('../../auth/authApi', () => ({
  register: vi.fn(),
  fetchCurrentUser: vi.fn(),
  login: vi.fn(),
  logout: vi.fn(),
}))

vi.mock('../../auth/useAuth', () => ({
  useAuth: vi.fn(),
}))

import { register } from '../../auth/authApi'
import { useAuth } from '../../auth/useAuth'
import { ApiError } from '../../api/errors'

describe('RegisterPage', () => {
  beforeEach(() => {
    vi.mocked(useAuth).mockReturnValue({
      status: 'unauthenticated',
      user: null,
      refreshUser: vi.fn(),
      setUser: vi.fn(),
      logout: vi.fn(),
    })
  })

  afterEach(() => {
    cleanup()
    vi.restoreAllMocks()
  })

  it('renders the registration form', () => {
    render(
      <MemoryRouter>
        <RegisterPage />
      </MemoryRouter>,
    )

    expect(screen.getByRole('heading', { name: 'Create account' })).toBeInTheDocument()
    expect(screen.getByLabelText('Display name')).toBeInTheDocument()
    expect(screen.getByLabelText('Email')).toBeInTheDocument()
    expect(screen.getByLabelText('Password')).toBeInTheDocument()
  })

  it('sends the expected registration request', async () => {
    const user = userEvent.setup()

    vi.mocked(register).mockResolvedValue({
      id: '11111111-1111-1111-1111-111111111111',
      displayName: 'Vishal',
      email: 'vishal@example.com',
    })

    render(
      <MemoryRouter>
        <RegisterPage />
      </MemoryRouter>,
    )

    await user.type(screen.getByLabelText('Display name'), 'Vishal')
    await user.type(screen.getByLabelText('Email'), 'vishal@example.com')
    await user.type(screen.getByLabelText('Password'), 'secret-value')
    await user.click(screen.getByRole('button', { name: 'Create account' }))

    await waitFor(() => {
      expect(register).toHaveBeenCalledWith({
        displayName: 'Vishal',
        email: 'vishal@example.com',
        password: 'secret-value',
      })
    })
  })

  it('displays field validation errors from the backend', async () => {
    const user = userEvent.setup()

    vi.mocked(register).mockRejectedValue(
      new ApiError('Request is invalid.', {
        status: 400,
        code: 'VALIDATION_ERROR',
        message: 'Request is invalid.',
        fieldErrors: [
          { field: 'email', message: 'must be a well-formed email address' },
        ],
      }),
    )

    render(
      <MemoryRouter>
        <RegisterPage />
      </MemoryRouter>,
    )

    await user.type(screen.getByLabelText('Display name'), 'Vishal')
    await user.type(screen.getByLabelText('Email'), 'not-an-email')
    await user.type(screen.getByLabelText('Password'), 'secret-value')
    await user.click(screen.getByRole('button', { name: 'Create account' }))

    expect(await screen.findByText('must be a well-formed email address')).toBeInTheDocument()
  })

  it('displays duplicate email errors from the backend', async () => {
    const user = userEvent.setup()

    vi.mocked(register).mockRejectedValue(
      new ApiError('Email is already registered.', {
        status: 409,
        code: 'EMAIL_ALREADY_REGISTERED',
        message: 'Email is already registered.',
        fieldErrors: [],
      }),
    )

    render(
      <MemoryRouter>
        <RegisterPage />
      </MemoryRouter>,
    )

    await user.type(screen.getByLabelText('Display name'), 'Vishal')
    await user.type(screen.getByLabelText('Email'), 'vishal@example.com')
    await user.type(screen.getByLabelText('Password'), 'secret-value')
    await user.click(screen.getByRole('button', { name: 'Create account' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Email is already registered.')
  })

  it('redirects to login without authenticating the user after success', async () => {
    const user = userEvent.setup()
    const setUser = vi.fn()

    vi.mocked(useAuth).mockReturnValue({
      status: 'unauthenticated',
      user: null,
      refreshUser: vi.fn(),
      setUser,
      logout: vi.fn(),
    })

    vi.mocked(register).mockResolvedValue({
      id: '11111111-1111-1111-1111-111111111111',
      displayName: 'Vishal',
      email: 'vishal@example.com',
    })

    render(
      <MemoryRouter initialEntries={['/register']}>
        <Routes>
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/login" element={<div>Login page</div>} />
        </Routes>
      </MemoryRouter>,
    )

    await user.type(screen.getByLabelText('Display name'), 'Vishal')
    await user.type(screen.getByLabelText('Email'), 'vishal@example.com')
    await user.type(screen.getByLabelText('Password'), 'secret-value')
    await user.click(screen.getByRole('button', { name: 'Create account' }))

    await waitFor(() => {
      expect(screen.getByText('Login page')).toBeInTheDocument()
    })
    expect(setUser).not.toHaveBeenCalled()
  })
})
