import { cleanup, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { ApiError } from '../../api/errors'
import { ConfirmProvider } from '../../components/ConfirmProvider'
import { RoutineDetailPage } from './RoutineDetailPage'
import { routineDetailFixture } from './routinesTestFixtures'

vi.mock('./routinesApi', () => ({
  fetchRoutine: vi.fn(),
  deleteRoutine: vi.fn(),
  routineDetailQueryKey: (id: string) => ['routines', id],
  ROUTINES_QUERY_KEY: ['routines'],
}))

import { deleteRoutine, fetchRoutine } from './routinesApi'

function renderDetail(
  routineId = '11111111-1111-1111-1111-111111111111',
  queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } }),
) {
  return render(
    <QueryClientProvider client={queryClient}>
      <ConfirmProvider>
        <MemoryRouter initialEntries={[`/routines/${routineId}`]}>
          <Routes>
            <Route path="/routines" element={<div>Routines list</div>} />
            <Route path="/routines/:routineId" element={<RoutineDetailPage />} />
            <Route path="/routines/:routineId/edit" element={<div>Edit routine</div>} />
          </Routes>
        </MemoryRouter>
      </ConfirmProvider>
    </QueryClientProvider>,
  )
}

describe('RoutineDetailPage', () => {
  beforeEach(() => {
    vi.mocked(fetchRoutine).mockReset()
    vi.mocked(deleteRoutine).mockReset()
  })

  afterEach(() => {
    cleanup()
    vi.restoreAllMocks()
  })

  it('renders routine detail with ordered exercises', async () => {
    vi.mocked(fetchRoutine).mockResolvedValue(routineDetailFixture)

    renderDetail()

    expect(await screen.findByRole('heading', { name: 'Push Day' })).toBeInTheDocument()
    expect(screen.getByText('1. Bench Press')).toBeInTheDocument()
    expect(screen.getByText('2. Squat')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Back to routines' })).toHaveAttribute('href', '/routines')
  })

  it('shows a loading state', () => {
    vi.mocked(fetchRoutine).mockReturnValue(new Promise(() => undefined))

    renderDetail()

    expect(screen.getByRole('status')).toHaveTextContent('Loading routine…')
  })

  it('shows a not found error', async () => {
    vi.mocked(fetchRoutine).mockRejectedValue(
      new ApiError('Routine not found.', {
        status: 404,
        code: 'NOT_FOUND',
        message: 'Routine not found.',
        fieldErrors: [],
      }),
    )

    renderDetail()

    expect(await screen.findByRole('heading', { name: 'Routine not found' })).toBeInTheDocument()
  })

  it('deletes a routine after confirmation', async () => {
    const user = userEvent.setup()
    vi.mocked(fetchRoutine).mockResolvedValue(routineDetailFixture)
    vi.mocked(deleteRoutine).mockResolvedValue(undefined)

    renderDetail()

    await screen.findByRole('heading', { name: 'Push Day' })
    await user.click(screen.getByRole('button', { name: 'Delete' }))
    const dialog = await screen.findByRole('dialog')
    await user.click(within(dialog).getByRole('button', { name: 'Delete routine' }))

    await waitFor(() => {
      expect(deleteRoutine).toHaveBeenCalledWith('11111111-1111-1111-1111-111111111111')
      expect(screen.getByText('Routines list')).toBeInTheDocument()
    })
  })

  it('does not delete when confirmation is cancelled', async () => {
    const user = userEvent.setup()
    vi.mocked(fetchRoutine).mockResolvedValue(routineDetailFixture)

    renderDetail()

    await screen.findByRole('heading', { name: 'Push Day' })
    await user.click(screen.getByRole('button', { name: 'Delete' }))
    const dialog = await screen.findByRole('dialog')
    await user.click(within(dialog).getByRole('button', { name: 'Cancel' }))

    expect(deleteRoutine).not.toHaveBeenCalled()
  })

  it('shows a delete error when deletion fails', async () => {
    const user = userEvent.setup()
    vi.mocked(fetchRoutine).mockResolvedValue(routineDetailFixture)
    vi.mocked(deleteRoutine).mockRejectedValue(
      new ApiError('Unable to delete routine.', {
        status: 500,
        code: 'INTERNAL_ERROR',
        message: 'Unable to delete routine.',
        fieldErrors: [],
      }),
    )

    renderDetail()

    await screen.findByRole('heading', { name: 'Push Day' })
    await user.click(screen.getByRole('button', { name: 'Delete' }))
    const dialog = await screen.findByRole('dialog')
    await user.click(within(dialog).getByRole('button', { name: 'Delete routine' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Unable to delete routine.')
  })
})
