import { cleanup, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { WorkoutSetRow } from './WorkoutSetRow'

describe('WorkoutSetRow', () => {
  const onSave = vi.fn()
  const onDelete = vi.fn()

  afterEach(() => {
    cleanup()
    vi.restoreAllMocks()
    onSave.mockReset()
    onDelete.mockReset()
  })

  it('saves a set as completed on Save set', async () => {
    const user = userEvent.setup()
    onSave.mockResolvedValue(undefined)

    render(
      <WorkoutSetRow
        set={{
          id: 'set-1',
          setNumber: 1,
          weightKg: null,
          repetitions: null,
          completed: false,
        }}
        onSave={onSave}
        onDelete={onDelete}
      />,
    )

    await user.type(screen.getByLabelText('Weight (kg)'), '60')
    await user.type(screen.getByLabelText('Reps'), '8')
    await user.click(screen.getByRole('button', { name: 'Save set' }))

    await waitFor(() => {
      expect(onSave).toHaveBeenCalledWith({
        weightKg: 60,
        repetitions: 8,
        completed: true,
      })
    })
  })

  it('does not render a completed checkbox', () => {
    render(
      <WorkoutSetRow
        set={{
          id: 'set-1',
          setNumber: 1,
          weightKg: null,
          repetitions: null,
          completed: false,
        }}
        onSave={onSave}
        onDelete={onDelete}
      />,
    )

    expect(screen.queryByRole('checkbox')).not.toBeInTheDocument()
  })

  it('requires weight and reps before saving a set', async () => {
    const user = userEvent.setup()

    render(
      <WorkoutSetRow
        set={{
          id: 'set-1',
          setNumber: 1,
          weightKg: null,
          repetitions: null,
          completed: false,
        }}
        onSave={onSave}
        onDelete={onDelete}
      />,
    )

    await user.click(screen.getByRole('button', { name: 'Save set' }))

    expect(
      await screen.findByText('Weight and repetitions are required before completing a set.'),
    ).toBeInTheDocument()
    expect(onSave).not.toHaveBeenCalled()
  })

  it('updates a completed set and keeps completed=true', async () => {
    const user = userEvent.setup()
    onSave.mockResolvedValue(undefined)

    render(
      <WorkoutSetRow
        set={{
          id: 'set-1',
          setNumber: 1,
          weightKg: 60,
          repetitions: 8,
          completed: true,
        }}
        onSave={onSave}
        onDelete={onDelete}
      />,
    )

    const weightInput = screen.getByLabelText('Weight (kg)')
    await user.clear(weightInput)
    await user.type(weightInput, '65')
    await user.click(screen.getByRole('button', { name: 'Update set' }))

    await waitFor(() => {
      expect(onSave).toHaveBeenCalledWith({
        weightKg: 65,
        repetitions: 8,
        completed: true,
      })
    })
  })

  it('displays the backend set number', () => {
    render(
      <WorkoutSetRow
        set={{
          id: 'set-1',
          setNumber: 3,
          weightKg: 50,
          repetitions: 10,
          completed: true,
        }}
        onSave={onSave}
        onDelete={onDelete}
      />,
    )

    expect(screen.getByText('Set 3')).toBeInTheDocument()
    expect(screen.getByText('Completed')).toBeInTheDocument()
  })
})
