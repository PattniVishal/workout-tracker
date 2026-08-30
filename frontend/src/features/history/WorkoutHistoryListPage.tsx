import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { ApiError } from '../../api/errors'
import { LoadingState } from '../../components/LoadingState'
import { formatDateTime } from '../../lib/formatDateTime'
import { formatDuration } from '../../lib/formatDuration'
import { fetchHistory, HISTORY_QUERY_KEY } from './historyApi'

function formatExerciseCount(count: number): string {
  return count === 1 ? '1 exercise' : `${count} exercises`
}

function formatCompletedSetCount(count: number): string {
  return count === 1 ? '1 completed set' : `${count} completed sets`
}

export function WorkoutHistoryListPage() {
  const { data, isPending, isError, error, refetch, isFetching } = useQuery({
    queryKey: HISTORY_QUERY_KEY,
    queryFn: fetchHistory,
  })

  if (isPending) {
    return <LoadingState message="Loading workout history…" />
  }

  if (isError) {
    const message =
      error instanceof ApiError ? error.message : 'Unable to load workout history. Please try again.'

    return (
      <section className="rounded-lg border border-red-200 bg-red-50 p-6 shadow-sm" role="alert">
        <h2 className="text-lg font-semibold text-red-800">History unavailable</h2>
        <p className="mt-2 text-sm text-red-700">{message}</p>
        <button
          type="button"
          onClick={() => refetch()}
          disabled={isFetching}
          className="mt-4 rounded-md border border-red-300 bg-white px-4 py-2 text-sm font-medium text-red-800 hover:bg-red-100 disabled:cursor-not-allowed disabled:opacity-60"
        >
          {isFetching ? 'Retrying…' : 'Try again'}
        </button>
      </section>
    )
  }

  const workouts = data?.workouts ?? []

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-2xl font-semibold">History</h2>
        <p className="mt-1 text-sm text-slate-600">Review your completed workouts.</p>
      </div>

      {workouts.length === 0 ? (
        <section className="rounded-lg border border-slate-200 bg-white p-6 shadow-sm">
          <p className="text-slate-600">No completed workouts yet.</p>
          <Link
            to="/workout"
            className="mt-4 inline-flex rounded-md border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50"
          >
            Start a workout
          </Link>
        </section>
      ) : (
        <ul className="space-y-3">
          {workouts.map((workout) => (
            <li key={workout.id}>
              <Link
                to={`/history/${workout.id}`}
                className="block rounded-lg border border-slate-200 bg-white p-5 shadow-sm transition hover:border-slate-300 hover:bg-slate-50"
              >
                <div className="flex flex-col gap-2 sm:flex-row sm:items-start sm:justify-between">
                  <div>
                    <h3 className="text-lg font-semibold text-slate-900">{workout.name}</h3>
                    <p className="mt-1 text-sm text-slate-600">
                      Completed {formatDateTime(workout.completedAt)}
                    </p>
                  </div>
                  <div className="text-sm text-slate-600">
                    <p>{formatDuration(workout.durationSeconds)}</p>
                    <p className="mt-1">{formatExerciseCount(workout.exerciseCount)}</p>
                    <p className="mt-1">{formatCompletedSetCount(workout.completedSetCount)}</p>
                  </div>
                </div>
              </Link>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
