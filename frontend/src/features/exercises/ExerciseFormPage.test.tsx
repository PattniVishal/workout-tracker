import { cleanup, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { ExerciseFormPage } from './ExerciseFormPage'
import { benchPressExercise, customExerciseFixture, exerciseListFixture } from './exercisesTestFixtures'

vi.mock('./exercisesApi', async () => {
  const actual = await vi.importActual<typeof import('./exercisesApi')>('./exercisesApi')
  return {
    ...actual,
    fetchExercises: vi.fn(),
    createExercise: vi.fn(),
    updateExercise: vi.fn(),
    invalidateExerciseQueries: vi.fn(),
  }
})

import {
  createExercise,
  fetchExercises,
  invalidateExerciseQueries,
  updateExercise,
} from './exercisesApi'

function renderFormPage(
  initialEntry: string,
  queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } }),
) {
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[initialEntry]}>
        <Routes>
          <Route path="/exercises" element={<div>Exercises list</div>} />
          <Route path="/exercises/new" element={<ExerciseFormPage mode="create" />} />
          <Route path="/exercises/:exerciseId/edit" element={<ExerciseFormPage mode="edit" />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('ExerciseFormPage', () => {
  beforeEach(() => {
    vi.mocked(fetchExercises).mockReset()
    vi.mocked(createExercise).mockReset()
    vi.mocked(updateExercise).mockReset()
    vi.mocked(invalidateExerciseQueries).mockReset()
    vi.mocked(fetchExercises).mockResolvedValue(exerciseListFixture)
    vi.mocked(invalidateExerciseQueries).mockResolvedValue(undefined)
  })

  afterEach(() => {
    cleanup()
    vi.restoreAllMocks()
  })

  it('creates a custom exercise and navigates to the library', async () => {
    const user = userEvent.setup()
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })

    vi.mocked(createExercise).mockResolvedValue(customExerciseFixture)

    renderFormPage('/exercises/new', queryClient)

    await user.type(screen.getByLabelText('Exercise name'), 'Cable Fly')
    await user.type(screen.getByLabelText('Primary muscle group'), 'Chest')
    await user.type(screen.getByLabelText('Category'), 'Cable')
    await user.click(screen.getByRole('button', { name: 'Create exercise' }))

    await waitFor(() => {
      expect(createExercise).toHaveBeenCalledWith({
        name: 'Cable Fly',
        primaryMuscleGroup: 'Chest',
        secondaryMuscleGroups: [],
        category: 'Cable',
      })
      expect(invalidateExerciseQueries).toHaveBeenCalledWith(queryClient)
      expect(screen.getByText('Exercises list')).toBeInTheDocument()
    })
  })

  it('populates edit values for a custom exercise', async () => {
    renderFormPage(`/exercises/${customExerciseFixture.id}/edit`)

    expect(await screen.findByDisplayValue('Cable Fly')).toBeInTheDocument()
    expect(screen.getByDisplayValue('Chest')).toBeInTheDocument()
    expect(screen.getByDisplayValue('Shoulders')).toBeInTheDocument()
    expect(screen.getByDisplayValue('Cable')).toBeInTheDocument()
  })

  it('updates a custom exercise with the correct payload', async () => {
    const user = userEvent.setup()

    vi.mocked(updateExercise).mockResolvedValue({
      ...customExerciseFixture,
      name: 'Cable Fly Updated',
    })

    renderFormPage(`/exercises/${customExerciseFixture.id}/edit`)

    const nameInput = await screen.findByLabelText('Exercise name')
    await user.clear(nameInput)
    await user.type(nameInput, 'Cable Fly Updated')
    await user.click(screen.getByRole('button', { name: 'Save changes' }))

    await waitFor(() => {
      expect(updateExercise).toHaveBeenCalledWith(customExerciseFixture.id, {
        name: 'Cable Fly Updated',
        primaryMuscleGroup: 'Chest',
        secondaryMuscleGroups: ['Shoulders'],
        category: 'Cable',
      })
      expect(screen.getByText('Exercises list')).toBeInTheDocument()
    })
  })

  it('shows that system exercises are not editable', async () => {
    renderFormPage(`/exercises/${benchPressExercise.id}/edit`)

    expect(await screen.findByRole('heading', { name: 'System exercise' })).toBeInTheDocument()
    expect(screen.queryByLabelText('Exercise name')).not.toBeInTheDocument()
  })

  it('shows not found when the exercise is missing from the library list', async () => {
    renderFormPage('/exercises/eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee/edit')

    expect(await screen.findByRole('heading', { name: 'Exercise not found' })).toBeInTheDocument()
  })
})
