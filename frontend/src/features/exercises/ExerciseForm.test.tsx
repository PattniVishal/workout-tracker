import { cleanup, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '../../api/errors'
import { ExerciseForm } from './ExerciseForm'

describe('ExerciseForm', () => {
  const onSubmit = vi.fn()

  beforeEach(() => {
    onSubmit.mockReset()
  })

  afterEach(() => {
    cleanup()
    vi.restoreAllMocks()
  })

  it('validates required fields on the client', async () => {
    const user = userEvent.setup()

    render(<ExerciseForm mode="create" isSubmitting={false} onSubmit={onSubmit} />)

    await user.click(screen.getByRole('button', { name: 'Create exercise' }))

    expect(screen.getByText('Name is required.')).toBeInTheDocument()
    expect(screen.getByText('Primary muscle group is required.')).toBeInTheDocument()
    expect(screen.getByText('Category is required.')).toBeInTheDocument()
    expect(onSubmit).not.toHaveBeenCalled()
  })

  it('submits the correct create payload', async () => {
    const user = userEvent.setup()
    onSubmit.mockResolvedValue(undefined)

    render(<ExerciseForm mode="create" isSubmitting={false} onSubmit={onSubmit} />)

    await user.type(screen.getByLabelText('Exercise name'), 'Cable Fly')
    await user.type(screen.getByLabelText('Primary muscle group'), 'Chest')
    await user.type(screen.getByLabelText('Secondary muscle groups'), 'Shoulders, Triceps')
    await user.type(screen.getByLabelText('Category'), 'Cable')
    await user.click(screen.getByRole('button', { name: 'Create exercise' }))

    await waitFor(() => {
      expect(onSubmit).toHaveBeenCalledWith({
        name: 'Cable Fly',
        primaryMuscleGroup: 'Chest',
        secondaryMuscleGroups: ['Shoulders', 'Triceps'],
        category: 'Cable',
      })
    })
  })

  it('displays backend field errors', async () => {
    const user = userEvent.setup()
    onSubmit.mockRejectedValue(
      new ApiError('Request is invalid.', {
        status: 400,
        code: 'VALIDATION_ERROR',
        message: 'Request is invalid.',
        fieldErrors: [{ field: 'name', message: 'Name must not be blank.' }],
      }),
    )

    render(<ExerciseForm mode="create" isSubmitting={false} onSubmit={onSubmit} />)

    await user.type(screen.getByLabelText('Exercise name'), 'Bad Name')
    await user.type(screen.getByLabelText('Primary muscle group'), 'Chest')
    await user.type(screen.getByLabelText('Category'), 'Cable')
    await user.click(screen.getByRole('button', { name: 'Create exercise' }))

    expect(await screen.findByText('Name must not be blank.')).toBeInTheDocument()
  })

  it('displays a general error message', async () => {
    const user = userEvent.setup()
    onSubmit.mockRejectedValue(
      new ApiError('Exercise already exists.', {
        status: 409,
        code: 'CONFLICT',
        message: 'Exercise already exists.',
        fieldErrors: [],
      }),
    )

    render(<ExerciseForm mode="create" isSubmitting={false} onSubmit={onSubmit} />)

    await user.type(screen.getByLabelText('Exercise name'), 'Cable Fly')
    await user.type(screen.getByLabelText('Primary muscle group'), 'Chest')
    await user.type(screen.getByLabelText('Category'), 'Cable')
    await user.click(screen.getByRole('button', { name: 'Create exercise' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Exercise already exists.')
  })

  it('prevents duplicate submissions while submitting', async () => {
    const user = userEvent.setup()
    onSubmit.mockImplementation(() => new Promise(() => undefined))

    render(<ExerciseForm mode="create" isSubmitting={true} onSubmit={onSubmit} />)

    await user.type(screen.getByLabelText('Exercise name'), 'Cable Fly')
    await user.click(screen.getByRole('button', { name: 'Saving…' }))

    expect(onSubmit).not.toHaveBeenCalled()
  })
})
