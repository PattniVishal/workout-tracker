import { Navigate, Outlet } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'
import { LoadingState } from './LoadingState'

export function GuestRoute() {
  const { status } = useAuth()

  if (status === 'loading') {
    return <LoadingState message="Checking session…" />
  }

  if (status === 'authenticated') {
    return <Navigate to="/" replace />
  }

  return <Outlet />
}
