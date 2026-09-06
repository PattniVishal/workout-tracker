import { cleanup, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { ApiError } from '../../api/errors'
import { ConfirmProvider } from '../../components/ConfirmProvider'
import { DASHBOARD_QUERY_KEY } from '../dashboard/dashboardApi'
import { WorkoutHistoryDetailPage } from './WorkoutHistoryDetailPage'
import { historyDetailFixture } from './historyTestFixtures'

vi.mock('./historyApi', () => ({
  fetchHistorySession: vi.fn(),
  deleteHistorySession: vi.fn(),
  historyDetailQueryKey: (id: string) => ['history', id],
  HISTORY_QUERY_KEY: ['history'],
}))

import { deleteHistorySession, fetchHistorySession } from './historyApi'

function renderDetail(
  sessionId = 'session-2222-2222-2222-222222222222',
  queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } }),
) {
  return render(
    <QueryClientProvider client={queryClient}>
      <ConfirmProvider>
        <MemoryRouter initialEntries={[`/history/${sessionId}`]}>
          <Routes>
            <Route path="/history" element={<div>History list</div>} />
            <Route path="/history/:sessionId" element={<WorkoutHistoryDetailPage />} />
          </Routes>
        </MemoryRouter>
      </ConfirmProvider>
    </QueryClientProvider>,
  )
}

describe('WorkoutHistoryDetailPage', () => {
  beforeEach(() => {
    vi.mocked(fetchHistorySession).mockReset()
    vi.mocked(deleteHistorySession).mockReset()
  })

  afterEach(() => {
    cleanup()
    vi.restoreAllMocks()
  })

  it('renders workout metadata on success', async () => {
    vi.mocked(fetchHistorySession).mockResolvedValue(historyDetailFixture)

    renderDetail()

    expect(await screen.findByRole('heading', { name: 'Push Day' })).toBeInTheDocument()
    expect(screen.getByText('COMPLETED')).toBeInTheDocument()
    expect(screen.getByText('Duration 1h 0m')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Back to history' })).toHaveAttribute('href', '/history')
  })

  it('renders exercises and sets in position and setNumber order', async () => {
    vi.mocked(fetchHistorySession).mockResolvedValue(historyDetailFixture)

    renderDetail()

    await screen.findByRole('heading', { name: 'Push Day' })

    const exerciseHeadings = screen.getAllByRole('heading', { level: 3 })
    expect(exerciseHeadings[0]).toHaveTextContent('1. Bench Press')
    expect(exerciseHeadings[1]).toHaveTextContent('2. Squat')

    const setLabels = screen.getAllByText(/^Set \d+$/)
    expect(setLabels[0]).toHaveTextContent('Set 1')
    expect(setLabels[1]).toHaveTextContent('Set 1')
    expect(setLabels[2]).toHaveTextContent('Set 2')
  })

  it('distinguishes completed and incomplete sets', async () => {
    vi.mocked(fetchHistorySession).mockResolvedValue(historyDetailFixture)

    renderDetail()

    await screen.findByRole('heading', { name: 'Push Day' })

    expect(screen.getAllByText('Completed')).toHaveLength(2)
    expect(screen.getByText('Incomplete')).toBeInTheDocument()
    expect(screen.getByText('60 kg')).toBeInTheDocument()
    expect(screen.getByText('8 reps')).toBeInTheDocument()
  })

  it('shows a loading state', () => {
    vi.mocked(fetchHistorySession).mockReturnValue(new Promise(() => undefined))

    renderDetail()

    expect(screen.getByRole('status')).toHaveTextContent('Loading workout…')
  })

  it('shows a not found error', async () => {
    vi.mocked(fetchHistorySession).mockRejectedValue(
      new ApiError('Workout not found.', {
        status: 404,
        code: 'NOT_FOUND',
        message: 'Workout not found.',
        fieldErrors: [],
      }),
    )

    renderDetail()

    expect(await screen.findByRole('heading', { name: 'Workout not found' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Back to history' })).toHaveAttribute('href', '/history')
  })

  it('shows a general error with retry', async () => {
    const user = userEvent.setup()

    vi.mocked(fetchHistorySession)
      .mockRejectedValueOnce(
        new ApiError('Unable to load workout.', {
          status: 500,
          code: 'INTERNAL_ERROR',
          message: 'Unable to load workout.',
          fieldErrors: [],
        }),
      )
      .mockResolvedValueOnce(historyDetailFixture)

    renderDetail()

    expect(await screen.findByRole('alert')).toHaveTextContent('Unable to load workout.')
    await user.click(screen.getByRole('button', { name: 'Try again' }))

    await waitFor(() => {
      expect(fetchHistorySession).toHaveBeenCalledTimes(2)
      expect(screen.getByRole('heading', { name: 'Push Day' })).toBeInTheDocument()
    })
  })

  it('does not delete when confirmation is cancelled', async () => {
    const user = userEvent.setup()
    vi.mocked(fetchHistorySession).mockResolvedValue(historyDetailFixture)

    renderDetail()

    await screen.findByRole('heading', { name: 'Push Day' })
    await user.click(screen.getByRole('button', { name: 'Delete workout' }))
    const dialog = await screen.findByRole('dialog')
    await user.click(within(dialog).getByRole('button', { name: 'Cancel' }))

    expect(deleteHistorySession).not.toHaveBeenCalled()
  })

  it('deletes a workout after confirmation and navigates to history', async () => {
    const user = userEvent.setup()
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
    const invalidateSpy = vi.spyOn(queryClient, 'invalidateQueries')
    const removeSpy = vi.spyOn(queryClient, 'removeQueries')

    vi.mocked(fetchHistorySession).mockResolvedValue(historyDetailFixture)
    vi.mocked(deleteHistorySession).mockResolvedValue(undefined)

    renderDetail('session-2222-2222-2222-222222222222', queryClient)

    await screen.findByRole('heading', { name: 'Push Day' })
    await user.click(screen.getByRole('button', { name: 'Delete workout' }))
    const dialog = await screen.findByRole('dialog')
    await user.click(within(dialog).getByRole('button', { name: 'Yes, delete' }))

    await waitFor(() => {
      expect(deleteHistorySession).toHaveBeenCalledWith('session-2222-2222-2222-222222222222')
      expect(screen.getByText('History list')).toBeInTheDocument()
    })

    expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ['history'] })
    expect(removeSpy).toHaveBeenCalledWith({
      queryKey: ['history', 'session-2222-2222-2222-222222222222'],
    })
    expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: DASHBOARD_QUERY_KEY })
    expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ['workout', 'previous-performance'] })
  })

  it('shows a delete error and stays on the page', async () => {
    const user = userEvent.setup()
    vi.mocked(fetchHistorySession).mockResolvedValue(historyDetailFixture)
    vi.mocked(deleteHistorySession).mockRejectedValue(
      new ApiError('Unable to delete workout.', {
        status: 500,
        code: 'INTERNAL_ERROR',
        message: 'Unable to delete workout.',
        fieldErrors: [],
      }),
    )

    renderDetail()

    await screen.findByRole('heading', { name: 'Push Day' })
    await user.click(screen.getByRole('button', { name: 'Delete workout' }))
    const dialog = await screen.findByRole('dialog')
    await user.click(within(dialog).getByRole('button', { name: 'Yes, delete' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Unable to delete workout.')
    expect(screen.getByRole('heading', { name: 'Push Day' })).toBeInTheDocument()
    expect(screen.queryByText('History list')).not.toBeInTheDocument()
  })

  it('prevents duplicate delete submissions while pending', async () => {
    const user = userEvent.setup()
    let resolveDelete: (() => void) | undefined

    vi.mocked(fetchHistorySession).mockResolvedValue(historyDetailFixture)
    vi.mocked(deleteHistorySession).mockImplementation(
      () =>
        new Promise<void>((resolve) => {
          resolveDelete = resolve
        }),
    )

    renderDetail()

    await screen.findByRole('heading', { name: 'Push Day' })

    const deleteButton = screen.getByRole('button', { name: 'Delete workout' })
    await user.click(deleteButton)
    const dialog = await screen.findByRole('dialog')
    await user.click(within(dialog).getByRole('button', { name: 'Yes, delete' }))

    expect(screen.getByRole('button', { name: 'Deleting…' })).toBeDisabled()

    await user.click(screen.getByRole('button', { name: 'Deleting…' }))
    expect(deleteHistorySession).toHaveBeenCalledTimes(1)

    resolveDelete?.()
    await waitFor(() => {
      expect(screen.getByText('History list')).toBeInTheDocument()
    })
  })
})
