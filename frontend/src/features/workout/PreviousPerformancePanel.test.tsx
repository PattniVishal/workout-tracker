import { cleanup, render, screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { PreviousPerformancePanel } from './PreviousPerformancePanel'

vi.mock('./previousPerformanceApi', () => ({
  fetchPreviousPerformance: vi.fn(),
  previousPerformanceQueryKey: (exerciseId: string) => ['workout', 'previous-performance', exerciseId],
}))

import { fetchPreviousPerformance } from './previousPerformanceApi'

function renderPanel() {
  return render(
    <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
      <PreviousPerformancePanel exerciseId="aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa" />
    </QueryClientProvider>,
  )
}

describe('PreviousPerformancePanel', () => {
  beforeEach(() => {
    vi.mocked(fetchPreviousPerformance).mockReset()
  })

  afterEach(() => {
    cleanup()
    vi.restoreAllMocks()
  })

  it('renders previous performance when available', async () => {
    vi.mocked(fetchPreviousPerformance).mockResolvedValue({
      exerciseId: 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
      exerciseName: 'Bench Press',
      completedAt: '2026-08-20T10:00:00.000Z',
      sessionId: 'session-old',
      sets: [{ setNumber: 1, weightKg: 55, repetitions: 8 }],
    })

    renderPanel()

    expect(await screen.findByText('Previous performance')).toBeInTheDocument()
    expect(screen.getByText(/Set 1:/)).toBeInTheDocument()
  })

  it('shows an empty state when no previous performance exists', async () => {
    vi.mocked(fetchPreviousPerformance).mockResolvedValue(null)

    renderPanel()

    expect(await screen.findByText('No previous performance recorded.')).toBeInTheDocument()
  })
})
