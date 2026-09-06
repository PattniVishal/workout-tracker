import { cleanup, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { RoutineFormPage } from './RoutineFormPage'
import { routineDetailFixture } from './routinesTestFixtures'

vi.mock('./routinesApi', () => ({
  fetchRoutine: vi.fn(),
  createRoutine: vi.fn(),
  updateRoutine: vi.fn(),
  routineDetailQueryKey: (id: string) => ['routines', id],
  ROUTINES_QUERY_KEY: ['routines'],
}))

vi.mock('./ExercisePicker', () => ({
  ExercisePicker: () => <div>Exercise picker</div>,
}))

import { fetchRoutine, updateRoutine } from './routinesApi'

function renderEditPage(queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })) {
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/routines/11111111-1111-1111-1111-111111111111/edit']}>
        <Routes>
          <Route path="/routines/:routineId/edit" element={<RoutineFormPage mode="edit" />} />
          <Route path="/routines/:routineId" element={<div>Routine detail</div>} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('RoutineFormPage edit', () => {
  beforeEach(() => {
    vi.mocked(fetchRoutine).mockReset()
    vi.mocked(updateRoutine).mockReset()
  })

  afterEach(() => {
    cleanup()
    vi.restoreAllMocks()
  })

  it('loads existing routine values for editing', async () => {
    vi.mocked(fetchRoutine).mockResolvedValue(routineDetailFixture)

    renderEditPage()

    expect(await screen.findByDisplayValue('Push Day')).toBeInTheDocument()
    expect(screen.getByDisplayValue('Chest and triceps')).toBeInTheDocument()
    expect(screen.getByText(/Bench Press/)).toBeInTheDocument()
    expect(screen.getByText(/Squat/)).toBeInTheDocument()
  })

  it('submits an update payload and navigates to detail', async () => {
    const user = userEvent.setup()
    vi.mocked(fetchRoutine).mockResolvedValue(routineDetailFixture)
    vi.mocked(updateRoutine).mockResolvedValue(routineDetailFixture)

    renderEditPage()

    await screen.findByDisplayValue('Push Day')
    await user.clear(screen.getByLabelText('Routine name'))
    await user.type(screen.getByLabelText('Routine name'), 'Updated Push Day')
    await user.click(screen.getByRole('button', { name: 'Save changes' }))

    await waitFor(() => {
      expect(updateRoutine).toHaveBeenCalledWith('11111111-1111-1111-1111-111111111111', {
        name: 'Updated Push Day',
        description: 'Chest and triceps',
        exercises: [
          {
            exerciseId: 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
            plannedSetCount: 3,
          },
          {
            exerciseId: 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
            plannedSetCount: 4,
          },
        ],
      })
      expect(screen.getByText('Routine detail')).toBeInTheDocument()
    })
  })
})
