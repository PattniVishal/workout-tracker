import { useState } from 'react'
import { Outlet, useNavigate } from 'react-router-dom'
import { ApiError } from '../api/errors'
import { useAuth } from '../auth/useAuth'
import { AppNav } from '../components/AppNav'

export function AppLayout() {
  const navigate = useNavigate()
  const { user, logout } = useAuth()
  const [logoutError, setLogoutError] = useState<string | null>(null)
  const [isLoggingOut, setIsLoggingOut] = useState(false)

  async function handleLogout() {
    if (isLoggingOut) {
      return
    }

    setIsLoggingOut(true)
    setLogoutError(null)

    try {
      await logout()
      navigate('/login', { replace: true })
    } catch (error) {
      if (error instanceof ApiError) {
        setLogoutError(error.message)
      } else {
        setLogoutError('Unable to sign out. Please try again.')
      }
    } finally {
      setIsLoggingOut(false)
    }
  }

  return (
    <div className="min-h-screen bg-slate-100 text-slate-900">
      <header className="border-b border-slate-200 bg-white">
        <div className="mx-auto flex max-w-5xl items-center justify-between px-4 py-4">
          <h1 className="text-lg font-semibold">Workout Tracker</h1>
          <div className="flex items-center gap-4">
            {user ? (
              <span className="hidden text-sm text-slate-600 sm:inline">{user.displayName}</span>
            ) : null}
            <button
              type="button"
              onClick={handleLogout}
              disabled={isLoggingOut}
              className="rounded-md border border-slate-300 px-3 py-1.5 text-sm font-medium text-slate-700 hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-60"
            >
              {isLoggingOut ? 'Signing out…' : 'Sign out'}
            </button>
          </div>
        </div>
        <AppNav />
        {logoutError ? (
          <p className="mx-auto max-w-5xl px-4 pb-3 text-sm text-red-600" role="alert">
            {logoutError}
          </p>
        ) : null}
      </header>
      <main className="mx-auto max-w-5xl px-4 py-6">
        <Outlet />
      </main>
    </div>
  )
}
