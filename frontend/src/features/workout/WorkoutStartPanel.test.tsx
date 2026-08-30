import { cleanup, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ApiError } from '../../api/errors'
import { WorkoutStartPanel } from './WorkoutStartPanel'
import { activeSessionFixture } from './workoutTestFixtures'
import { routineListFixture } from '../routines/routinesTestFixtures'

vi.mock('./workoutApi', () => ({
  fetchCurrentSession: vi.fn(),
  startSession: vi.fn(),
  CURRENT_SESSION_QUERY_KEY: ['workout', 'current'],
}))

vi.mock('../routines/routinesApi', () => ({
  fetchRoutines: vi.fn(),
  ROUTINES_QUERY_KEY: ['routines'],
}))

import { fetchCurrentSession, startSession } from './workoutApi'
import { fetchRoutines } from '../routines/routinesApi'

function renderStartPanel(queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })) {
  return render(
    <QueryClientProvider client={queryClient}>
      <WorkoutStartPanel />
    </QueryClientProvider>,
  )
}

describe('WorkoutStartPanel', () => {
  beforeEach(() => {
    vi.mocked(fetchRoutines).mockResolvedValue(routineListFixture)
    vi.mocked(startSession).mockReset()
    vi.mocked(fetchCurrentSession).mockReset()
  })

  afterEach(() => {
    cleanup()
    vi.restoreAllMocks()
  })

  it('starts a workout from a routine', async () => {
    const user = userEvent.setup()
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })

    vi.mocked(startSession).mockResolvedValue(activeSessionFixture)

    renderStartPanel(queryClient)

    await user.selectOptions(await screen.findByLabelText('Select routine'), routineListFixture.routines[0].id)
    await user.click(screen.getByRole('button', { name: 'Start from routine' }))

    await waitFor(() => {
      expect(startSession).toHaveBeenCalledWith({
        routineId: routineListFixture.routines[0].id,
      })
      expect(queryClient.getQueryData(['workout', 'current'])).toEqual(activeSessionFixture)
    })
  })

  it('starts an ad-hoc workout', async () => {
    const user = userEvent.setup()

    vi.mocked(startSession).mockResolvedValue({
      ...activeSessionFixture,
      name: 'Morning Session',
      originRoutineId: null,
    })

    renderStartPanel()

    await user.type(await screen.findByPlaceholderText('Workout name'), 'Morning Session')
    await user.click(screen.getByRole('button', { name: 'Start ad-hoc' }))

    await waitFor(() => {
      expect(startSession).toHaveBeenCalledWith({ name: 'Morning Session' })
    })
  })

  it('resumes when ACTIVE_SESSION_EXISTS is returned', async () => {
    const user = userEvent.setup()
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })

    vi.mocked(startSession).mockRejectedValue(
      new ApiError('An active workout already exists.', {
        status: 409,
        code: 'ACTIVE_SESSION_EXISTS',
        message: 'An active workout already exists.',
        fieldErrors: [],
      }),
    )
    vi.mocked(fetchCurrentSession).mockResolvedValue(activeSessionFixture)

    renderStartPanel(queryClient)

    await user.selectOptions(await screen.findByLabelText('Select routine'), routineListFixture.routines[0].id)
    await user.click(screen.getByRole('button', { name: 'Start from routine' }))

    await waitFor(() => {
      expect(fetchCurrentSession).toHaveBeenCalled()
      expect(queryClient.getQueryData(['workout', 'current'])).toEqual(activeSessionFixture)
    })
  })
})
