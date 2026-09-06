import { useQuery } from '@tanstack/react-query'
import { ApiError } from '../../api/errors'
import { formatDateTime } from '../../lib/formatDateTime'
import { fetchPreviousPerformance, previousPerformanceQueryKey } from './previousPerformanceApi'
import { sortSessionSets } from './workoutApi'

type PreviousPerformancePanelProps = {
  exerciseId: string
}

export function PreviousPerformancePanel({ exerciseId }: PreviousPerformancePanelProps) {
  const { data, isPending, isError, error } = useQuery({
    queryKey: previousPerformanceQueryKey(exerciseId),
    queryFn: () => fetchPreviousPerformance(exerciseId),
    staleTime: 5 * 60 * 1000,
  })

  if (isPending) {
    return <p className="text-sm text-slate-500">Loading previous performance…</p>
  }

  if (isError) {
    const message =
      error instanceof ApiError
        ? error.message
        : 'Unable to load previous performance.'

    return <p className="text-sm text-slate-500">{message}</p>
  }

  if (!data) {
    return <p className="text-sm text-slate-500">No previous performance recorded.</p>
  }

  const sets = sortSessionSets(data.sets)

  return (
    <div className="rounded-md border border-slate-200 bg-slate-50 p-3">
      <p className="text-sm font-medium text-slate-700">Previous performance</p>
      <p className="mt-1 text-xs text-slate-600">Completed {formatDateTime(data.completedAt)}</p>
      <ul className="mt-2 space-y-1 text-sm text-slate-700">
        {sets.map((set) => (
          <li key={set.setNumber}>
            Set {set.setNumber}: {set.weightKg ?? '—'} kg × {set.repetitions ?? '—'} reps
          </li>
        ))}
      </ul>
    </div>
  )
}
