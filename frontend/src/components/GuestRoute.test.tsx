import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { GuestRoute } from './GuestRoute'

vi.mock('../auth/useAuth', () => ({
  useAuth: vi.fn(),
}))

import { useAuth } from '../auth/useAuth'

describe('GuestRoute', () => {
  beforeEach(() => {
    vi.mocked(useAuth).mockReset()
  })

  it('shows a loading state while auth initializes', () => {
    vi.mocked(useAuth).mockReturnValue({
      status: 'loading',
      user: null,
      refreshUser: vi.fn(),
      setUser: vi.fn(),
      logout: vi.fn(),
    })

    render(
      <MemoryRouter initialEntries={['/login']}>
        <Routes>
          <Route element={<GuestRoute />}>
            <Route path="/login" element={<div>Login form</div>} />
          </Route>
        </Routes>
      </MemoryRouter>,
    )

    expect(screen.getByRole('status')).toHaveTextContent('Checking session…')
  })

  it('redirects authenticated users away from public auth routes', () => {
    vi.mocked(useAuth).mockReturnValue({
      status: 'authenticated',
      user: {
        id: '11111111-1111-1111-1111-111111111111',
        displayName: 'Vishal',
        email: 'vishal@example.com',
      },
      refreshUser: vi.fn(),
      setUser: vi.fn(),
      logout: vi.fn(),
    })

    render(
      <MemoryRouter initialEntries={['/login']}>
        <Routes>
          <Route element={<GuestRoute />}>
            <Route path="/login" element={<div>Login form</div>} />
          </Route>
          <Route path="/" element={<div>Home page</div>} />
        </Routes>
      </MemoryRouter>,
    )

    expect(screen.getByText('Home page')).toBeInTheDocument()
  })

  it('renders guest content for unauthenticated users', () => {
    vi.mocked(useAuth).mockReturnValue({
      status: 'unauthenticated',
      user: null,
      refreshUser: vi.fn(),
      setUser: vi.fn(),
      logout: vi.fn(),
    })

    render(
      <MemoryRouter initialEntries={['/login']}>
        <Routes>
          <Route element={<GuestRoute />}>
            <Route path="/login" element={<div>Login form</div>} />
          </Route>
        </Routes>
      </MemoryRouter>,
    )

    expect(screen.getByText('Login form')).toBeInTheDocument()
  })
})
