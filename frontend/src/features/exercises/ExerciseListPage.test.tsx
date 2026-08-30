import { cleanup, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router-dom'
import { ApiError } from '../../api/errors'
import { ConfirmProvider } from '../../components/ConfirmProvider'
import { EXERCISES_QUERY_KEY_PREFIX } from './exercisesApi'
import { ExerciseListPage } from './ExerciseListPage'
import { customExerciseFixture, exerciseListFixture } from './exercisesTestFixtures'

vi.mock('./useMuscleGroupOptions', () => ({
  useMuscleGroupOptions: () => ['Chest', 'Legs'],
}))

vi.mock('./exercisesApi', async () => {
  const actual = await vi.importActual<typeof import('./exercisesApi')>('./exercisesApi')
  return {
    ...actual,
    fetchExercises: vi.fn(),
    archiveExercise: vi.fn(),
    invalidateExerciseQueries: vi.fn(),
  }
})

import { archiveExercise, fetchExercises, invalidateExerciseQueries } from './exercisesApi'

function renderList(queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })) {
  return render(
    <QueryClientProvider client={queryClient}>
      <ConfirmProvider>
        <MemoryRouter>
          <ExerciseListPage />
        </MemoryRouter>
      </ConfirmProvider>
    </QueryClientProvider>,
  )
}

describe('ExerciseListPage', () => {
  beforeEach(() => {
    vi.mocked(fetchExercises).mockReset()
    vi.mocked(archiveExercise).mockReset()
    vi.mocked(invalidateExerciseQueries).mockReset()
    vi.mocked(fetchExercises).mockResolvedValue(exerciseListFixture)
    vi.mocked(invalidateExerciseQueries).mockResolvedValue(undefined)
  })

  afterEach(() => {
    cleanup()
    vi.restoreAllMocks()
  })

  it('renders exercises and distinguishes system from custom', async () => {
    renderList()

    expect(await screen.findByRole('heading', { name: 'Bench Press' })).toBeInTheDocument()
    expect(screen.getAllByText('System')).toHaveLength(2)
    expect(screen.getByText('My exercise')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Edit' })).toHaveAttribute(
      'href',
      '/exercises/dddddddd-dddd-dddd-dddd-dddddddddddd/edit',
    )
    expect(screen.queryByRole('button', { name: 'Archive' })).toBeInTheDocument()
    expect(screen.getAllByRole('button', { name: 'Archive' })).toHaveLength(1)
  })

  it('applies search and muscle group filters on Search', async () => {
    const user = userEvent.setup()

    renderList()

    await screen.findByRole('heading', { name: 'Bench Press' })

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

  it('supports blank search', async () => {
    const user = userEvent.setup()

    renderList()

    await screen.findByRole('heading', { name: 'Bench Press' })
    await user.click(screen.getByRole('button', { name: 'Search' }))

    await waitFor(() => {
      expect(fetchExercises).toHaveBeenLastCalledWith({
        q: '',
        muscleGroup: '',
      })
    })
  })

  it('shows a loading state', () => {
    vi.mocked(fetchExercises).mockReturnValue(new Promise(() => undefined))

    renderList()

    expect(screen.getByRole('status')).toHaveTextContent('Loading exercises…')
  })

  it('shows an error state with retry', async () => {
    const user = userEvent.setup()

    vi.mocked(fetchExercises)
      .mockRejectedValueOnce(
        new ApiError('Unable to load exercises.', {
          status: 500,
          code: 'INTERNAL_ERROR',
          message: 'Unable to load exercises.',
          fieldErrors: [],
        }),
      )
      .mockResolvedValueOnce(exerciseListFixture)

    renderList()

    expect(await screen.findByRole('alert')).toHaveTextContent('Unable to load exercises.')
    await user.click(screen.getByRole('button', { name: 'Try again' }))

    await waitFor(() => {
      expect(fetchExercises).toHaveBeenCalledTimes(2)
      expect(screen.getByRole('heading', { name: 'Bench Press' })).toBeInTheDocument()
    })
  })

  it('shows an empty state when no exercises match filters', async () => {
    vi.mocked(fetchExercises).mockResolvedValue({ exercises: [] })

    renderList()

    expect(await screen.findByText('No exercises are available yet.')).toBeInTheDocument()
  })

  it('shows a filtered empty state', async () => {
    const user = userEvent.setup()
    vi.mocked(fetchExercises)
      .mockResolvedValueOnce(exerciseListFixture)
      .mockResolvedValueOnce({ exercises: [] })

    renderList()

    await screen.findByRole('heading', { name: 'Bench Press' })
    await user.type(screen.getByPlaceholderText('Search by name'), 'missing')
    await user.click(screen.getByRole('button', { name: 'Search' }))

    expect(await screen.findByText('No exercises match your filters.')).toBeInTheDocument()
  })

  it('links to create exercise', async () => {
    renderList()

    expect(await screen.findByRole('link', { name: 'Create exercise' })).toHaveAttribute(
      'href',
      '/exercises/new',
    )
  })

  it('does not archive when confirmation is cancelled', async () => {
    const user = userEvent.setup()

    renderList()

    await screen.findByRole('heading', { name: 'Cable Fly' })
    await user.click(screen.getByRole('button', { name: 'Archive' }))
    const dialog = await screen.findByRole('dialog')
    await user.click(within(dialog).getByRole('button', { name: 'Cancel' }))

    expect(archiveExercise).not.toHaveBeenCalled()
  })

  it('archives a custom exercise after confirmation', async () => {
    const user = userEvent.setup()
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
    const invalidateSpy = vi.spyOn(queryClient, 'invalidateQueries')

    vi.mocked(archiveExercise).mockResolvedValue(undefined)

    renderList(queryClient)

    await screen.findByRole('heading', { name: 'Cable Fly' })
    await user.click(screen.getByRole('button', { name: 'Archive' }))
    const dialog = await screen.findByRole('dialog')
    await user.click(within(dialog).getByRole('button', { name: 'Archive' }))

    await waitFor(() => {
      expect(archiveExercise).toHaveBeenCalledWith(customExerciseFixture.id)
      expect(invalidateExerciseQueries).toHaveBeenCalled()
      expect(invalidateSpy).not.toHaveBeenCalled()
    })
  })

  it('shows an archive error and keeps the exercise visible', async () => {
    const user = userEvent.setup()

    vi.mocked(archiveExercise).mockRejectedValue(
      new ApiError('Unable to archive exercise.', {
        status: 500,
        code: 'INTERNAL_ERROR',
        message: 'Unable to archive exercise.',
        fieldErrors: [],
      }),
    )

    renderList()

    await screen.findByRole('heading', { name: 'Cable Fly' })
    await user.click(screen.getByRole('button', { name: 'Archive' }))
    const dialog = await screen.findByRole('dialog')
    await user.click(within(dialog).getByRole('button', { name: 'Archive' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Unable to archive exercise.')
    expect(screen.getByRole('heading', { name: 'Cable Fly' })).toBeInTheDocument()
  })

  it('prevents duplicate archive submissions while pending', async () => {
    const user = userEvent.setup()
    let resolveArchive: (() => void) | undefined

    vi.mocked(archiveExercise).mockImplementation(
      () =>
        new Promise<void>((resolve) => {
          resolveArchive = resolve
        }),
    )

    renderList()

    await screen.findByRole('heading', { name: 'Cable Fly' })

    const archiveButton = screen.getByRole('button', { name: 'Archive' })
    await user.click(archiveButton)
    const dialog = await screen.findByRole('dialog')
    await user.click(within(dialog).getByRole('button', { name: 'Archive' }))

    expect(screen.getByRole('button', { name: 'Archiving…' })).toBeDisabled()
    await user.click(screen.getByRole('button', { name: 'Archiving…' }))
    expect(archiveExercise).toHaveBeenCalledTimes(1)

    resolveArchive?.()
    await waitFor(() => {
      expect(invalidateExerciseQueries).toHaveBeenCalled()
    })
  })

  it('invalidates the exercises query family after archive', async () => {
    const user = userEvent.setup()
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
    vi.mocked(invalidateExerciseQueries).mockImplementation(async (client) => {
      await client.invalidateQueries({ queryKey: EXERCISES_QUERY_KEY_PREFIX })
    })
    vi.mocked(archiveExercise).mockResolvedValue(undefined)

    renderList(queryClient)

    await screen.findByRole('heading', { name: 'Cable Fly' })
    await user.click(screen.getByRole('button', { name: 'Archive' }))
    const dialog = await screen.findByRole('dialog')
    await user.click(within(dialog).getByRole('button', { name: 'Archive' }))

    await waitFor(() => {
      expect(invalidateExerciseQueries).toHaveBeenCalledWith(queryClient)
    })
  })
})
