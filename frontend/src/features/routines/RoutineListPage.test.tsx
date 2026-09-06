import { cleanup, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router-dom'
import { ApiError } from '../../api/errors'
import { RoutineListPage } from './RoutineListPage'
import { routineListFixture } from './routinesTestFixtures'

vi.mock('./routinesApi', () => ({
  fetchRoutines: vi.fn(),
  ROUTINES_QUERY_KEY: ['routines'],
}))

import { fetchRoutines } from './routinesApi'

function renderList(queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })) {
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <RoutineListPage />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('RoutineListPage', () => {
  beforeEach(() => {
    vi.mocked(fetchRoutines).mockReset()
  })

  afterEach(() => {
    cleanup()
    vi.restoreAllMocks()
  })

  it('renders routines on success', async () => {
    vi.mocked(fetchRoutines).mockResolvedValue(routineListFixture)

    renderList()

    expect(await screen.findByRole('heading', { name: 'Routines' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /Push Day/i })).toHaveAttribute(
      'href',
      '/routines/11111111-1111-1111-1111-111111111111',
    )
    expect(screen.getByText('Chest and triceps')).toBeInTheDocument()
    expect(screen.getByText('2 exercises')).toBeInTheDocument()
  })

  it('shows a loading state', () => {
    vi.mocked(fetchRoutines).mockReturnValue(new Promise(() => undefined))

    renderList()

    expect(screen.getByRole('status')).toHaveTextContent('Loading routines…')
  })

  it('shows an error state with retry', async () => {
    const user = userEvent.setup()

    vi.mocked(fetchRoutines)
      .mockRejectedValueOnce(
        new ApiError('Unable to load routines.', {
          status: 500,
          code: 'INTERNAL_ERROR',
          message: 'Unable to load routines.',
          fieldErrors: [],
        }),
      )
      .mockResolvedValueOnce(routineListFixture)

    renderList()

    expect(await screen.findByRole('alert')).toHaveTextContent('Unable to load routines.')
    await user.click(screen.getByRole('button', { name: 'Try again' }))

    await waitFor(() => {
      expect(fetchRoutines).toHaveBeenCalledTimes(2)
      expect(screen.getByText('Push Day')).toBeInTheDocument()
    })
  })

  it('shows an empty state', async () => {
    vi.mocked(fetchRoutines).mockResolvedValue({ routines: [] })

    renderList()

    expect(await screen.findByText('You do not have any routines yet.')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Create your first routine' })).toHaveAttribute(
      'href',
      '/routines/new',
    )
  })
})
