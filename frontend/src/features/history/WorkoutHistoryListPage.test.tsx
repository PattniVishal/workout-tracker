import { cleanup, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router-dom'
import { ApiError } from '../../api/errors'
import { WorkoutHistoryListPage } from './WorkoutHistoryListPage'
import { emptyHistoryFixture, historyListFixture } from './historyTestFixtures'

vi.mock('./historyApi', () => ({
  fetchHistory: vi.fn(),
  HISTORY_QUERY_KEY: ['history'],
}))

import { fetchHistory } from './historyApi'

function renderList(queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })) {
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <WorkoutHistoryListPage />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('WorkoutHistoryListPage', () => {
  beforeEach(() => {
    vi.mocked(fetchHistory).mockReset()
  })

  afterEach(() => {
    cleanup()
    vi.restoreAllMocks()
  })

  it('renders history entries on success', async () => {
    vi.mocked(fetchHistory).mockResolvedValue(historyListFixture)

    renderList()

    expect(await screen.findByRole('heading', { name: 'History' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /Push Day/i })).toHaveAttribute(
      'href',
      '/history/session-2222-2222-2222-222222222222',
    )
    expect(screen.getByRole('link', { name: /Pull Day/i })).toHaveAttribute(
      'href',
      '/history/session-1111-1111-1111-111111111111',
    )
  })

  it('renders workouts in backend-provided order', async () => {
    vi.mocked(fetchHistory).mockResolvedValue(historyListFixture)

    renderList()

    const links = await screen.findAllByRole('link', { name: /Day/i })
    expect(links[0]).toHaveAccessibleName(/Push Day/i)
    expect(links[1]).toHaveAccessibleName(/Pull Day/i)
  })

  it('displays completed date and time', async () => {
    vi.mocked(fetchHistory).mockResolvedValue(historyListFixture)

    renderList()

    await screen.findByRole('heading', { name: 'History' })

    // Locale-independent: fixture dates are in 2026; avoid assuming day-first vs month-first formatting.
    expect(screen.getAllByText(/^Completed .+2026/)).toHaveLength(2)
  })

  it('displays duration, exercise count, and completed set count', async () => {
    vi.mocked(fetchHistory).mockResolvedValue(historyListFixture)

    renderList()

    await screen.findByRole('heading', { name: 'History' })

    expect(screen.getByText('1h 0m')).toBeInTheDocument()
    expect(screen.getByText('5m')).toBeInTheDocument()
    expect(screen.getByText('5 exercises')).toBeInTheDocument()
    expect(screen.getByText('4 exercises')).toBeInTheDocument()
    expect(screen.getByText('14 completed sets')).toBeInTheDocument()
    expect(screen.getByText('12 completed sets')).toBeInTheDocument()
  })

  it('shows an empty state with a link to start a workout', async () => {
    vi.mocked(fetchHistory).mockResolvedValue(emptyHistoryFixture)

    renderList()

    expect(await screen.findByText('No completed workouts yet.')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Start a workout' })).toHaveAttribute('href', '/workout')
  })

  it('shows a loading state', () => {
    vi.mocked(fetchHistory).mockReturnValue(new Promise(() => undefined))

    renderList()

    expect(screen.getByRole('status')).toHaveTextContent('Loading workout history…')
  })

  it('shows an error state with retry', async () => {
    const user = userEvent.setup()

    vi.mocked(fetchHistory)
      .mockRejectedValueOnce(
        new ApiError('Unable to load workout history.', {
          status: 500,
          code: 'INTERNAL_ERROR',
          message: 'Unable to load workout history.',
          fieldErrors: [],
        }),
      )
      .mockResolvedValueOnce(historyListFixture)

    renderList()

    expect(await screen.findByRole('alert')).toHaveTextContent('Unable to load workout history.')
    await user.click(screen.getByRole('button', { name: 'Try again' }))

    await waitFor(() => {
      expect(fetchHistory).toHaveBeenCalledTimes(2)
      expect(screen.getByText('Push Day')).toBeInTheDocument()
    })
  })

  it('navigates to detail via workout links', async () => {
    vi.mocked(fetchHistory).mockResolvedValue(historyListFixture)

    renderList()

    expect(await screen.findByRole('link', { name: /Push Day/i })).toHaveAttribute(
      'href',
      '/history/session-2222-2222-2222-222222222222',
    )
  })
})
