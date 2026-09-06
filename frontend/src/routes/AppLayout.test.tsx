import { cleanup, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { AppLayout } from '../routes/AppLayout'

vi.mock('../auth/useAuth', () => ({
  useAuth: vi.fn(),
}))

import { useAuth } from '../auth/useAuth'

describe('AppLayout', () => {
  const logout = vi.fn()

  beforeEach(() => {
    logout.mockReset()
    vi.mocked(useAuth).mockReturnValue({
      status: 'authenticated',
      user: {
        id: '11111111-1111-1111-1111-111111111111',
        displayName: 'Vishal',
        email: 'vishal@example.com',
      },
      refreshUser: vi.fn(),
      setUser: vi.fn(),
      logout,
    })
  })

  afterEach(() => {
    cleanup()
    vi.restoreAllMocks()
  })

  it('renders navigation items', () => {
    render(
      <MemoryRouter initialEntries={['/']}>
        <Routes>
          <Route element={<AppLayout />}>
            <Route index element={<div>Protected content</div>} />
          </Route>
        </Routes>
      </MemoryRouter>,
    )

    expect(screen.getByRole('link', { name: 'Dashboard' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Routines' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Exercises' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Workout' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'History' })).toBeInTheDocument()
  })

  it('highlights the active navigation route', () => {
    render(
      <MemoryRouter initialEntries={['/history']}>
        <Routes>
          <Route element={<AppLayout />}>
            <Route path="/history" element={<div>History content</div>} />
          </Route>
        </Routes>
      </MemoryRouter>,
    )

    expect(screen.getByRole('link', { name: 'History' })).toHaveClass('border-slate-900')
  })

  it('calls logout and redirects to the login page', async () => {
    const user = userEvent.setup()
    logout.mockResolvedValue(undefined)

    render(
      <MemoryRouter initialEntries={['/']}>
        <Routes>
          <Route element={<AppLayout />}>
            <Route index element={<div>Protected content</div>} />
          </Route>
          <Route path="/login" element={<div>Login page</div>} />
        </Routes>
      </MemoryRouter>,
    )

    await user.click(screen.getByRole('button', { name: 'Sign out' }))

    await waitFor(() => {
      expect(logout).toHaveBeenCalledTimes(1)
      expect(screen.getByText('Login page')).toBeInTheDocument()
    })
  })
})
