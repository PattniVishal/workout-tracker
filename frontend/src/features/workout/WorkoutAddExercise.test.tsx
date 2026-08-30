import { cleanup, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { WorkoutAddExercise } from './WorkoutAddExercise'
import { benchPressExercise, squatExercise } from '../routines/routinesTestFixtures'

vi.mock('../exercises/useMuscleGroupOptions', () => ({
  useMuscleGroupOptions: () => ['Chest', 'Legs', 'Back'],
}))

vi.mock('../routines/exercisesApi', () => ({
  fetchExercises: vi.fn(),
  exercisesQueryKey: (params: unknown) => ['exercises', params],
}))

import { fetchExercises } from '../routines/exercisesApi'

function renderAddExercise() {
  return render(
    <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
      <WorkoutAddExercise onAdd={vi.fn()} />
    </QueryClientProvider>,
  )
}

describe('WorkoutAddExercise', () => {
  beforeEach(() => {
    vi.mocked(fetchExercises).mockReset()
    vi.mocked(fetchExercises).mockImplementation(async (params) => {
      if (params?.muscleGroup === 'Chest') {
        return { exercises: [benchPressExercise] }
      }
      if (params?.muscleGroup === 'Legs') {
        return { exercises: [squatExercise] }
      }
      return { exercises: [benchPressExercise, squatExercise] }
    })
  })

  afterEach(() => {
    cleanup()
    vi.restoreAllMocks()
  })

  it('keeps all muscle group options available after filtering', async () => {
    const user = userEvent.setup()

    renderAddExercise()

    await screen.findByText('Bench Press')

    await user.selectOptions(screen.getByLabelText('Muscle group filter'), 'Chest')
    await user.click(screen.getByRole('button', { name: 'Search' }))

    await waitFor(() => {
      expect(fetchExercises).toHaveBeenLastCalledWith({ q: '', muscleGroup: 'Chest' })
    })

    const filter = screen.getByLabelText('Muscle group filter')
    expect(filter).toContainHTML('<option value="Legs">Legs</option>')
    expect(filter).toContainHTML('<option value="Back">Back</option>')

    await user.selectOptions(filter, 'Legs')
    await user.click(screen.getByRole('button', { name: 'Search' }))

    await waitFor(() => {
      expect(fetchExercises).toHaveBeenLastCalledWith({ q: '', muscleGroup: 'Legs' })
      expect(screen.getByText('Squat')).toBeInTheDocument()
    })
  })
})
