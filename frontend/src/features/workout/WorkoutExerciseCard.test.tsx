import { cleanup, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { WorkoutExerciseCard } from './WorkoutExerciseCard'
import { activeSessionFixture } from './workoutTestFixtures'

vi.mock('./PreviousPerformancePanel', () => ({
  PreviousPerformancePanel: () => null,
}))

describe('WorkoutExerciseCard', () => {
  const exercise = activeSessionFixture.exercises[0]
  const handlers = {
    onAddSet: vi.fn(),
    onUpdateSet: vi.fn(),
    onDeleteSet: vi.fn(),
    onRemoveExercise: vi.fn(),
  }

  afterEach(() => {
    cleanup()
    vi.restoreAllMocks()
    handlers.onAddSet.mockReset()
    handlers.onUpdateSet.mockReset()
    handlers.onDeleteSet.mockReset()
    handlers.onRemoveExercise.mockReset()
  })

  it('expands and collapses on toggle', async () => {
    const user = userEvent.setup()
    const onToggle = vi.fn()

    render(
      <WorkoutExerciseCard
        exercise={exercise}
        isExpanded={false}
        onToggle={onToggle}
        savingSetId={null}
        {...handlers}
      />,
    )

    expect(screen.queryByLabelText('Weight (kg)')).not.toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: /Expand Bench Press/i }))

    expect(onToggle).toHaveBeenCalled()
    expect(screen.getByRole('button', { name: /Expand Bench Press/i })).toHaveAttribute(
      'aria-expanded',
      'false',
    )
  })

  it('renders an accessible chevron indicator', () => {
    const { container } = render(
      <WorkoutExerciseCard
        exercise={exercise}
        isExpanded={false}
        onToggle={vi.fn()}
        savingSetId={null}
        {...handlers}
      />,
    )

    expect(container.querySelector('svg[aria-hidden="true"]')).toBeInTheDocument()
  })

  it('shows set details only when expanded', () => {
    const { rerender } = render(
      <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
        <WorkoutExerciseCard
          exercise={exercise}
          isExpanded={false}
          onToggle={vi.fn()}
          savingSetId={null}
          {...handlers}
        />
      </QueryClientProvider>,
    )

    expect(screen.queryByLabelText('Weight (kg)')).not.toBeInTheDocument()

    rerender(
      <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
        <WorkoutExerciseCard
          exercise={exercise}
          isExpanded
          onToggle={vi.fn()}
          savingSetId={null}
          {...handlers}
        />
      </QueryClientProvider>,
    )

    expect(screen.getAllByLabelText('Weight (kg)').length).toBeGreaterThan(0)
  })
})
