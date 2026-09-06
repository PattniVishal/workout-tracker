import { cleanup, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router-dom'
import { ApiError } from '../../api/errors'
import { DashboardPage } from './DashboardPage'
import {
  activeSessionDashboardFixture,
  dashboardFixture,
  emptyDashboardFixture,
} from './dashboardTestFixtures'

vi.mock('./dashboardApi', () => ({
  fetchDashboard: vi.fn(),
  DASHBOARD_QUERY_KEY: ['dashboard'],
}))

import { fetchDashboard } from './dashboardApi'

function renderDashboard(queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })) {
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <DashboardPage />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('DashboardPage', () => {
  beforeEach(() => {
    vi.mocked(fetchDashboard).mockReset()
  })

  afterEach(() => {
    cleanup()
    vi.restoreAllMocks()
  })

  it('renders completed workout stats side by side on mobile', async () => {
    vi.mocked(fetchDashboard).mockResolvedValue(dashboardFixture)

    renderDashboard()

    await screen.findByRole('heading', { name: 'Welcome back, Vishal' })

    const workoutsCard = screen.getByText('Completed workouts').closest('div')
    const setsCard = screen.getByText('Completed sets').closest('div')
    const statsGrid = workoutsCard?.parentElement

    expect(statsGrid).toHaveClass('grid-cols-2')
    expect(statsGrid).toContainElement(workoutsCard)
    expect(statsGrid).toContainElement(setsCard)
  })

  it('renders dashboard data on success', async () => {
    vi.mocked(fetchDashboard).mockResolvedValue(dashboardFixture)

    renderDashboard()

    expect(await screen.findByRole('heading', { name: 'Welcome back, Vishal' })).toBeInTheDocument()
    expect(screen.getByText('Completed workouts').nextElementSibling).toHaveTextContent('12')
    expect(screen.getByText('Completed sets').nextElementSibling).toHaveTextContent('140')
    expect(screen.getAllByText('Push Day')).toHaveLength(2)
    expect(screen.getByText('5 exercises')).toBeInTheDocument()
  })

  it('shows Resume Workout when there is an active session', async () => {
    vi.mocked(fetchDashboard).mockResolvedValue(activeSessionDashboardFixture)

    renderDashboard()

    expect(await screen.findByRole('link', { name: 'Resume Workout' })).toHaveAttribute('href', '/workout')
  })

  it('shows Start Workout when there is no active session', async () => {
    vi.mocked(fetchDashboard).mockResolvedValue(dashboardFixture)

    renderDashboard()

    expect(await screen.findByRole('link', { name: 'Start Workout' })).toHaveAttribute('href', '/workout')
  })

  it('renders routines from the dashboard response', async () => {
    vi.mocked(fetchDashboard).mockResolvedValue(dashboardFixture)

    renderDashboard()

    const routinesSection = await screen.findByRole('heading', { name: 'Routines' })
    const section = routinesSection.closest('section')
    expect(section).toHaveTextContent('Push Day')
    expect(section).toHaveTextContent('5 exercises')
  })

  it('shows an empty routines state', async () => {
    vi.mocked(fetchDashboard).mockResolvedValue(emptyDashboardFixture)

    renderDashboard()

    expect(await screen.findByText(/No routines yet/i)).toBeInTheDocument()
  })

  it('renders the most recent completed workout', async () => {
    vi.mocked(fetchDashboard).mockResolvedValue(dashboardFixture)

    renderDashboard()

    const recentSection = await screen.findByRole('heading', { name: 'Most recent workout' })
    const section = recentSection.closest('section')
    expect(section).toHaveTextContent('Push Day')
    expect(section).toHaveTextContent(/Completed /i)
  })

  it('shows an empty state when there is no completed workout', async () => {
    vi.mocked(fetchDashboard).mockResolvedValue(emptyDashboardFixture)

    renderDashboard()

    expect(await screen.findByText('No completed workouts yet.')).toBeInTheDocument()
  })

  it('shows a loading state while fetching', () => {
    vi.mocked(fetchDashboard).mockReturnValue(new Promise(() => undefined))

    renderDashboard()

    expect(screen.getByRole('status')).toHaveTextContent('Loading dashboard…')
  })

  it('shows an error state when the dashboard API fails', async () => {
    vi.mocked(fetchDashboard).mockRejectedValue(
      new ApiError('Unable to load dashboard.', {
        status: 500,
        code: 'INTERNAL_ERROR',
        message: 'Unable to load dashboard.',
        fieldErrors: [],
      }),
    )

    renderDashboard()

    expect(await screen.findByRole('alert')).toHaveTextContent('Unable to load dashboard.')
  })

  it('retries loading when Try again is clicked', async () => {
    const user = userEvent.setup()

    vi.mocked(fetchDashboard)
      .mockRejectedValueOnce(
        new ApiError('Unable to load dashboard.', {
          status: 500,
          code: 'INTERNAL_ERROR',
          message: 'Unable to load dashboard.',
          fieldErrors: [],
        }),
      )
      .mockResolvedValueOnce(dashboardFixture)

    renderDashboard()

    expect(await screen.findByRole('alert')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Try again' }))

    await waitFor(() => {
      expect(fetchDashboard).toHaveBeenCalledTimes(2)
      expect(screen.getByRole('heading', { name: 'Welcome back, Vishal' })).toBeInTheDocument()
    })
  })
})
