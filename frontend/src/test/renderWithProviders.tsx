import { render, type RenderOptions } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter, type MemoryRouterProps } from 'react-router-dom'
import { AuthProvider } from '../auth/AuthProvider'
import { ConfirmProvider } from '../components/ConfirmProvider'
import type { ReactElement, ReactNode } from 'react'

type RenderWithProvidersOptions = {
  route?: string
  routerProps?: MemoryRouterProps
  queryClient?: QueryClient
} & Omit<RenderOptions, 'wrapper'>

export function createTestQueryClient() {
  return new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
      },
    },
  })
}

export function renderWithProviders(
  ui: ReactElement,
  {
    route = '/',
    routerProps,
    queryClient = createTestQueryClient(),
    ...renderOptions
  }: RenderWithProvidersOptions = {},
) {
  function Wrapper({ children }: { children: ReactNode }) {
    return (
      <QueryClientProvider client={queryClient}>
        <AuthProvider>
          <ConfirmProvider>
            <MemoryRouter initialEntries={[route]} {...routerProps}>
              {children}
            </MemoryRouter>
          </ConfirmProvider>
        </AuthProvider>
      </QueryClientProvider>
    )
  }

  return {
    queryClient,
    ...render(ui, { wrapper: Wrapper, ...renderOptions }),
  }
}
