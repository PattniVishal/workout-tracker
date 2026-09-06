import { cleanup, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '../../api/errors'
import { RoutineForm } from './RoutineForm'
import { archivedRoutineExercise } from './routinesTestFixtures'
import type { SaveRoutineRequest } from './routineTypes'

vi.mock('./ExercisePicker', () => ({
  ExercisePicker: ({
    onSelect,
  }: {
    onSelect: (exercise: {
      id: string
      name: string
      primaryMuscleGroup: string
      secondaryMuscleGroups: string[]
      category: string
      source: string
    }) => void
  }) => (
    <button
      type="button"
      onClick={() =>
        onSelect({
          id: 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
          name: 'Bench Press',
          primaryMuscleGroup: 'Chest',
          secondaryMuscleGroups: ['Triceps'],
          category: 'Barbell',
          source: 'SYSTEM',
        })
      }
    >
      Mock add exercise
    </button>
  ),
}))

describe('RoutineForm', () => {
  const onSubmit = vi.fn()

  beforeEach(() => {
    onSubmit.mockReset()
  })

  afterEach(() => {
    cleanup()
    vi.restoreAllMocks()
  })

  it('validates required name', async () => {
    const user = userEvent.setup()

    render(<RoutineForm mode="create" isSubmitting={false} onSubmit={onSubmit} />)

    await user.click(screen.getByRole('button', { name: 'Create routine' }))

    expect(await screen.findByText('Name is required.')).toBeInTheDocument()
    expect(onSubmit).not.toHaveBeenCalled()
  })

  it('submits the exact backend payload on create', async () => {
    const user = userEvent.setup()
    onSubmit.mockResolvedValue(undefined)

    render(<RoutineForm mode="create" isSubmitting={false} onSubmit={onSubmit} />)

    await user.type(screen.getByLabelText('Routine name'), 'Push Day')
    await user.type(screen.getByLabelText('Description'), 'Chest and triceps')
    await user.click(screen.getByRole('button', { name: 'Mock add exercise' }))
    await user.click(screen.getByRole('button', { name: 'Create routine' }))

    await waitFor(() => {
      expect(onSubmit).toHaveBeenCalledWith({
        name: 'Push Day',
        description: 'Chest and triceps',
        exercises: [
          {
            exerciseId: 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
            plannedSetCount: 3,
          },
        ],
      } satisfies SaveRoutineRequest)
    })
  })

  it('displays backend field errors', async () => {
    const user = userEvent.setup()
    onSubmit.mockRejectedValue(
      new ApiError('Request is invalid.', {
        status: 400,
        code: 'VALIDATION_ERROR',
        message: 'Request is invalid.',
        fieldErrors: [{ field: 'name', message: 'must not be blank' }],
      }),
    )

    render(<RoutineForm mode="create" isSubmitting={false} onSubmit={onSubmit} />)

    await user.type(screen.getByLabelText('Routine name'), 'Push Day')
    await user.click(screen.getByRole('button', { name: 'Create routine' }))

    expect(await screen.findByText('must not be blank')).toBeInTheDocument()
  })

  it('displays duplicate exercise conflict errors', async () => {
    const user = userEvent.setup()
    onSubmit.mockRejectedValue(
      new ApiError('Routine cannot contain the same exercise more than once.', {
        status: 409,
        code: 'DUPLICATE_ROUTINE_EXERCISE',
        message: 'Routine cannot contain the same exercise more than once.',
        fieldErrors: [],
      }),
    )

    render(<RoutineForm mode="create" isSubmitting={false} onSubmit={onSubmit} />)

    await user.type(screen.getByLabelText('Routine name'), 'Push Day')
    await user.click(screen.getByRole('button', { name: 'Create routine' }))

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Routine cannot contain the same exercise more than once.',
    )
  })

  it('retains an existing exercise that is absent from the picker', async () => {
    const user = userEvent.setup()
    onSubmit.mockResolvedValue(undefined)

    render(
      <RoutineForm
        mode="edit"
        initialName="Legacy Routine"
        initialExercises={[
          {
            exerciseId: archivedRoutineExercise.exerciseId,
            exerciseName: archivedRoutineExercise.exerciseName,
            plannedSetCount: archivedRoutineExercise.plannedSetCount,
          },
        ]}
        isSubmitting={false}
        onSubmit={onSubmit}
      />,
    )

    expect(screen.getByText(/Archived Custom Press/)).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Save changes' }))

    await waitFor(() => {
      expect(onSubmit).toHaveBeenCalledWith({
        name: 'Legacy Routine',
        description: null,
        exercises: [
          {
            exerciseId: archivedRoutineExercise.exerciseId,
            plannedSetCount: 3,
          },
        ],
      })
    })
  })
})
