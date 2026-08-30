import { useQuery, useQueryClient } from '@tanstack/react-query'
import { createContext, useCallback, useMemo, type ReactNode } from 'react'
import { clearCsrfToken } from '../api/csrf'
import { fetchCurrentUser, logout as logoutRequest } from './authApi'
import type { UserResponse } from '../api/types'

export const AUTH_ME_QUERY_KEY = ['auth', 'me'] as const

export type AuthStatus = 'loading' | 'authenticated' | 'unauthenticated'

export type AuthContextValue = {
  status: AuthStatus
  user: UserResponse | null
  refreshUser: () => Promise<UserResponse | null>
  setUser: (user: UserResponse | null) => void
  logout: () => Promise<void>
}

export const AuthContext = createContext<AuthContextValue | null>(null)

type AuthProviderProps = {
  children: ReactNode
}

export function AuthProvider({ children }: AuthProviderProps) {
  const queryClient = useQueryClient()

  const { data: user, isPending, refetch } = useQuery({
    queryKey: AUTH_ME_QUERY_KEY,
    queryFn: fetchCurrentUser,
    staleTime: 5 * 60 * 1000,
    retry: false,
  })

  const refreshUser = useCallback(async () => {
    const result = await refetch()
    return result.data ?? null
  }, [refetch])

  const setUser = useCallback(
    (nextUser: UserResponse | null) => {
      queryClient.setQueryData(AUTH_ME_QUERY_KEY, nextUser)
    },
    [queryClient],
  )

  const logout = useCallback(async () => {
    await logoutRequest()
    clearCsrfToken()
    queryClient.setQueryData(AUTH_ME_QUERY_KEY, null)
  }, [queryClient])

  const value = useMemo<AuthContextValue>(
    () => ({
      status: isPending ? 'loading' : user ? 'authenticated' : 'unauthenticated',
      user: user ?? null,
      refreshUser,
      setUser,
      logout,
    }),
    [isPending, user, refreshUser, setUser, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
