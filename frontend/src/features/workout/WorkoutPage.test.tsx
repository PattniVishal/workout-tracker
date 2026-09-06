import { cleanup, render, screen, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router-dom'
import { ApiError } from '../../api/errors'
import { WorkoutPage } from './WorkoutPage'
import { activeSessionFixture } from './workoutTestFixtures'

vi.mock('./workoutApi', () => ({
  fetchCurrentSession: vi.fn(),
  CURRENT_SESSION_QUERY_KEY: ['workout', 'current'],
}))

vi.mock('./WorkoutStartPanel', () => ({
  WorkoutStartPanel: () => <div>Start workout UI</div>,
}))

vi.mock('./ActiveWorkoutView', () => ({
  ActiveWorkoutView: ({ session }: { session: { name: string } }) => (
    <div>Active workout: {session.name}</div>
  ),
}))

import { fetchCurrentSession } from './workoutApi'

function renderWorkoutPage(queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })) {
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <WorkoutPage />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('WorkoutPage', () => {
  beforeEach(() => {
    vi.mocked(fetchCurrentSession).mockReset()
  })

  afterEach(() => {
    cleanup()
    vi.restoreAllMocks()
  })

  it('renders the active workout when a current session exists', async () => {
    vi.mocked(fetchCurrentSession).mockResolvedValue(activeSessionFixture)

    renderWorkoutPage()

    expect(await screen.findByText('Active workout: Push Day')).toBeInTheDocument()
  })

  it('shows the start UI when there is no active session', async () => {
    vi.mocked(fetchCurrentSession).mockResolvedValue(null)

    renderWorkoutPage()

    expect(await screen.findByText('Start workout UI')).toBeInTheDocument()
  })

  it('shows a loading state', () => {
    vi.mocked(fetchCurrentSession).mockReturnValue(new Promise(() => undefined))

    renderWorkoutPage()

    expect(screen.getByRole('status')).toHaveTextContent('Loading workout…')
  })

  it('shows an error state with retry', async () => {
    const user = (await import('@testing-library/user-event')).default.setup()

    vi.mocked(fetchCurrentSession)
      .mockRejectedValueOnce(
        new ApiError('Unable to load workout.', {
          status: 500,
          code: 'INTERNAL_ERROR',
          message: 'Unable to load workout.',
          fieldErrors: [],
        }),
      )
      .mockResolvedValueOnce(activeSessionFixture)

    renderWorkoutPage()

    expect(await screen.findByRole('alert')).toHaveTextContent('Unable to load workout.')

    await user.click(screen.getByRole('button', { name: 'Try again' }))

    await waitFor(() => {
      expect(fetchCurrentSession).toHaveBeenCalledTimes(2)
      expect(screen.getByText('Active workout: Push Day')).toBeInTheDocument()
    })
  })
})
