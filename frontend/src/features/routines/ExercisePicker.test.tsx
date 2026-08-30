import { cleanup, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ExercisePicker } from './ExercisePicker'
import { benchPressExercise, squatExercise } from './routinesTestFixtures'

vi.mock('../exercises/useMuscleGroupOptions', () => ({
  useMuscleGroupOptions: () => ['Chest', 'Legs'],
}))

vi.mock('./exercisesApi', () => ({
  fetchExercises: vi.fn(),
  exercisesQueryKey: (params: unknown) => ['exercises', params],
}))

import { fetchExercises } from './exercisesApi'

function renderPicker(
  props: {
    selectedExerciseIds?: string[]
    onSelect?: ReturnType<typeof vi.fn>
  } = {},
) {
  const onSelect = props.onSelect ?? vi.fn()

  render(
    <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
      <ExercisePicker selectedExerciseIds={props.selectedExerciseIds ?? []} onSelect={onSelect} />
    </QueryClientProvider>,
  )

  return { onSelect }
}

describe('ExercisePicker', () => {
  beforeEach(() => {
    vi.mocked(fetchExercises).mockReset()
    vi.mocked(fetchExercises).mockResolvedValue({
      exercises: [benchPressExercise, squatExercise],
    })
  })

  afterEach(() => {
    cleanup()
    vi.restoreAllMocks()
  })

  it('shows a loading state', () => {
    vi.mocked(fetchExercises).mockReturnValue(new Promise(() => undefined))

    renderPicker()

    expect(screen.getByRole('status')).toHaveTextContent('Loading exercises…')
  })

  it('renders exercises and selects one', async () => {
    const user = userEvent.setup()
    const { onSelect } = renderPicker()

    expect(await screen.findByText('Bench Press')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Add Bench Press' }))

    expect(onSelect).toHaveBeenCalledWith(benchPressExercise)
  })

  it('applies search and muscle group filters', async () => {
    const user = userEvent.setup()

    renderPicker()

    await screen.findByText('Bench Press')

    await user.type(screen.getByPlaceholderText('Search by name'), 'bench')
    await user.selectOptions(screen.getByLabelText('Muscle group filter'), 'Chest')
    await user.click(screen.getByRole('button', { name: 'Search' }))

    await waitFor(() => {
      expect(fetchExercises).toHaveBeenLastCalledWith({
        q: 'bench',
        muscleGroup: 'Chest',
      })
    })
  })

  it('disables add for already selected exercises', async () => {
    renderPicker({ selectedExerciseIds: [benchPressExercise.id] })

    expect(await screen.findByRole('button', { name: 'Bench Press already added' })).toBeDisabled()
  })
})
