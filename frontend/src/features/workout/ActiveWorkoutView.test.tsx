import { cleanup, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { ApiError } from '../../api/errors'
import { ConfirmProvider } from '../../components/ConfirmProvider'
import { ActiveWorkoutView } from './ActiveWorkoutView'
import { activeSessionFixture } from './workoutTestFixtures'

vi.mock('./PreviousPerformancePanel', () => ({
  PreviousPerformancePanel: () => <div>Previous performance</div>,
}))

vi.mock('./WorkoutAddExercise', () => ({
  WorkoutAddExercise: ({ onAdd }: { onAdd: (exerciseId: string, count: number) => Promise<void> }) => (
    <button type="button" onClick={() => onAdd('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 2)}>
      Add exercise
    </button>
  ),
}))

vi.mock('./workoutApi', async (importOriginal) => {
  const actual = await importOriginal<typeof import('./workoutApi')>()
  return {
    ...actual,
    addSessionExercise: vi.fn(),
    addSessionSet: vi.fn(),
    completeCurrentSession: vi.fn(),
    discardCurrentSession: vi.fn(),
    removeSessionExercise: vi.fn(),
    removeSessionSet: vi.fn(),
    updateSessionSet: vi.fn(),
    CURRENT_SESSION_QUERY_KEY: ['workout', 'current'],
  }
})

import {
  addSessionExercise,
  addSessionSet,
  completeCurrentSession,
  discardCurrentSession,
  updateSessionSet,
} from './workoutApi'

function renderActiveWorkout(queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })) {
  return render(
    <QueryClientProvider client={queryClient}>
      <ConfirmProvider>
        <MemoryRouter initialEntries={['/workout']}>
          <Routes>
            <Route path="/workout" element={<ActiveWorkoutView session={activeSessionFixture} />} />
            <Route path="/" element={<div>Dashboard</div>} />
          </Routes>
        </MemoryRouter>
      </ConfirmProvider>
    </QueryClientProvider>,
  )
}

describe('ActiveWorkoutView', () => {
  beforeEach(() => {
    vi.mocked(addSessionExercise).mockReset()
    vi.mocked(addSessionSet).mockReset()
    vi.mocked(updateSessionSet).mockReset()
    vi.mocked(completeCurrentSession).mockReset()
    vi.mocked(discardCurrentSession).mockReset()
  })

  afterEach(() => {
    cleanup()
    vi.restoreAllMocks()
  })

  it('renders exercises in backend order', async () => {
    renderActiveWorkout()

    expect(await screen.findByText('Push Day')).toBeInTheDocument()
    expect(screen.getByText(/Bench Press/)).toBeInTheDocument()
  })

  it('renders all exercises collapsed by default', async () => {
    const multiExerciseSession = {
      ...activeSessionFixture,
      exercises: [
        activeSessionFixture.exercises[0],
        {
          ...activeSessionFixture.exercises[0],
          id: 'workout-exercise-2222-2222-222222222222',
          exerciseName: 'Squat',
          position: 2,
        },
      ],
    }

    render(
      <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
        <ConfirmProvider>
          <MemoryRouter>
            <ActiveWorkoutView session={multiExerciseSession} />
          </MemoryRouter>
        </ConfirmProvider>
      </QueryClientProvider>,
    )

    const benchToggle = await screen.findByRole('button', { name: /Expand Bench Press/i })
    const squatToggle = screen.getByRole('button', { name: /Expand Squat/i })

    expect(benchToggle).toHaveAttribute('aria-expanded', 'false')
    expect(squatToggle).toHaveAttribute('aria-expanded', 'false')
    expect(screen.queryByLabelText('Weight (kg)')).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Remove exercise' })).not.toBeInTheDocument()
  })

  it('expands only one exercise at a time', async () => {
    const user = userEvent.setup()
    const multiExerciseSession = {
      ...activeSessionFixture,
      exercises: [
        activeSessionFixture.exercises[0],
        {
          ...activeSessionFixture.exercises[0],
          id: 'workout-exercise-2222-2222-222222222222',
          exerciseName: 'Squat',
          position: 2,
        },
      ],
    }

    render(
      <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
        <ConfirmProvider>
          <MemoryRouter>
            <ActiveWorkoutView session={multiExerciseSession} />
          </MemoryRouter>
        </ConfirmProvider>
      </QueryClientProvider>,
    )

    const benchToggle = await screen.findByRole('button', { name: /Expand Bench Press/i })
    const squatToggle = screen.getByRole('button', { name: /Expand Squat/i })

    expect(benchToggle).toHaveAttribute('aria-expanded', 'false')
    expect(squatToggle).toHaveAttribute('aria-expanded', 'false')

    await user.click(benchToggle)

    expect(benchToggle).toHaveAttribute('aria-expanded', 'true')
    expect(screen.getAllByRole('button', { name: 'Remove exercise' })).toHaveLength(1)

    await user.click(squatToggle)

    expect(screen.getAllByRole('button', { name: 'Remove exercise' })).toHaveLength(1)
    expect(benchToggle).toHaveAttribute('aria-expanded', 'false')
    expect(squatToggle).toHaveAttribute('aria-expanded', 'true')
    expect(screen.queryByText(/Bench Press/)).toBeInTheDocument()
    expect(screen.getByText(/Squat/)).toBeInTheDocument()
  })

  it('collapses the open exercise when toggled again', async () => {
    const user = userEvent.setup()

    renderActiveWorkout()

    const toggle = await screen.findByRole('button', { name: /Expand Bench Press/i })
    expect(screen.queryByLabelText('Weight (kg)')).not.toBeInTheDocument()

    await user.click(toggle)

    expect(screen.getAllByLabelText('Weight (kg)').length).toBeGreaterThan(0)
    expect(toggle).toHaveAttribute('aria-expanded', 'true')

    await user.click(toggle)

    expect(screen.queryByLabelText('Weight (kg)')).not.toBeInTheDocument()
    expect(toggle).toHaveAttribute('aria-expanded', 'false')
  })

  it('keeps a newly added exercise collapsed', async () => {
    const user = userEvent.setup()
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
    const sessionWithAddedExercise = {
      ...activeSessionFixture,
      exercises: [
        ...activeSessionFixture.exercises,
        {
          id: 'workout-exercise-new-1111-111111111111',
          exerciseId: 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
          exerciseName: 'Added Exercise',
          position: 2,
          sets: [
            {
              id: 'set-new-1111-1111-1111-111111111111',
              setNumber: 1,
              weightKg: null,
              repetitions: null,
              completed: false,
            },
            {
              id: 'set-new-2222-2222-2222-222222222222',
              setNumber: 2,
              weightKg: null,
              repetitions: null,
              completed: false,
            },
          ],
        },
      ],
    }

    vi.mocked(addSessionExercise).mockResolvedValue(sessionWithAddedExercise)

    const { rerender } = render(
      <QueryClientProvider client={queryClient}>
        <ConfirmProvider>
          <MemoryRouter>
            <ActiveWorkoutView session={activeSessionFixture} />
          </MemoryRouter>
        </ConfirmProvider>
      </QueryClientProvider>,
    )

    await user.click(screen.getByRole('button', { name: 'Add exercise' }))

    await waitFor(() => {
      expect(addSessionExercise).toHaveBeenCalled()
    })

    rerender(
      <QueryClientProvider client={queryClient}>
        <ConfirmProvider>
          <MemoryRouter>
            <ActiveWorkoutView session={sessionWithAddedExercise} />
          </MemoryRouter>
        </ConfirmProvider>
      </QueryClientProvider>,
    )

    const addedToggle = await screen.findByRole('button', { name: /Expand Added Exercise/i })
    expect(addedToggle).toHaveAttribute('aria-expanded', 'false')
    expect(screen.queryByRole('button', { name: 'Remove exercise' })).not.toBeInTheDocument()
  })

  it('updates the current session cache when a set is saved', async () => {
    const user = userEvent.setup()
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
    const updatedSession = {
      ...activeSessionFixture,
      exercises: [
        {
          ...activeSessionFixture.exercises[0],
          sets: [
            {
              ...activeSessionFixture.exercises[0].sets[0],
              completed: true,
            },
          ],
        },
      ],
    }

    vi.mocked(updateSessionSet).mockResolvedValue(updatedSession)

    renderActiveWorkout(queryClient)

    await user.click(await screen.findByRole('button', { name: /Expand Bench Press/i }))

    const incompleteSetId = 'set-2222-2222-2222-222222222222'
    await user.clear(screen.getByLabelText('Weight (kg)', { selector: `#weight-${incompleteSetId}` }))
    await user.type(screen.getByLabelText('Weight (kg)', { selector: `#weight-${incompleteSetId}` }), '60')
    await user.clear(screen.getByLabelText('Reps', { selector: `#reps-${incompleteSetId}` }))
    await user.type(screen.getByLabelText('Reps', { selector: `#reps-${incompleteSetId}` }), '8')
    await user.click(screen.getByRole('button', { name: 'Save set' }))

    await waitFor(() => {
      expect(updateSessionSet).toHaveBeenCalled()
      expect(queryClient.getQueryData(['workout', 'current'])).toEqual(updatedSession)
    })
  })

  it('shows NO_COMPLETED_SETS message on completion failure', async () => {
    const user = userEvent.setup()

    vi.mocked(completeCurrentSession).mockRejectedValue(
      new ApiError('No completed sets.', {
        status: 409,
        code: 'NO_COMPLETED_SETS',
        message: 'No completed sets.',
        fieldErrors: [],
      }),
    )

    renderActiveWorkout()

    await user.click(screen.getByRole('button', { name: 'Complete workout' }))
    const dialog = await screen.findByRole('dialog')
    await user.click(within(dialog).getByRole('button', { name: 'Yes, complete' }))

    expect(
      await screen.findByText('Complete at least one set before finishing this workout.'),
    ).toBeInTheDocument()
  })

  it('clears the current session and navigates home on successful completion', async () => {
    const user = userEvent.setup()
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })

    vi.mocked(completeCurrentSession).mockResolvedValue({
      ...activeSessionFixture,
      status: 'COMPLETED',
    })

    renderActiveWorkout(queryClient)

    await user.click(screen.getByRole('button', { name: 'Complete workout' }))
    const dialog = await screen.findByRole('dialog')
    await user.click(within(dialog).getByRole('button', { name: 'Yes, complete' }))

    await waitFor(() => {
      expect(queryClient.getQueryData(['workout', 'current'])).toBeNull()
      expect(screen.getByText('Dashboard')).toBeInTheDocument()
    })
  })

  it('does not discard when confirmation is cancelled', async () => {
    const user = userEvent.setup()

    renderActiveWorkout()

    await user.click(screen.getByRole('button', { name: 'Discard workout' }))
    const dialog = await screen.findByRole('dialog')
    await user.click(within(dialog).getByRole('button', { name: 'Cancel' }))

    expect(discardCurrentSession).not.toHaveBeenCalled()
  })

  it('clears the current session after discard', async () => {
    const user = userEvent.setup()
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })

    vi.mocked(discardCurrentSession).mockResolvedValue(undefined)

    renderActiveWorkout(queryClient)

    await user.click(screen.getByRole('button', { name: 'Discard workout' }))
    const dialog = await screen.findByRole('dialog')
    await user.click(within(dialog).getByRole('button', { name: 'Yes, discard' }))

    await waitFor(() => {
      expect(discardCurrentSession).toHaveBeenCalled()
      expect(queryClient.getQueryData(['workout', 'current'])).toBeNull()
    })
  })
})
