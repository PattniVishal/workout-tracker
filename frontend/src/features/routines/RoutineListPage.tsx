import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { ApiError } from '../../api/errors'
import { LoadingState } from '../../components/LoadingState'
import { formatDateTime } from '../../lib/formatDateTime'
import { fetchRoutines, ROUTINES_QUERY_KEY } from './routinesApi'

function formatExerciseCount(count: number): string {
  return count === 1 ? '1 exercise' : `${count} exercises`
}

export function RoutineListPage() {
  const { data, isPending, isError, error, refetch, isFetching } = useQuery({
    queryKey: ROUTINES_QUERY_KEY,
    queryFn: fetchRoutines,
  })

  if (isPending) {
    return <LoadingState message="Loading routines…" />
  }

  if (isError) {
    const message =
      error instanceof ApiError ? error.message : 'Unable to load routines. Please try again.'

    return (
      <section className="rounded-lg border border-red-200 bg-red-50 p-6 shadow-sm" role="alert">
        <h2 className="text-lg font-semibold text-red-800">Routines unavailable</h2>
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

  const routines = data?.routines ?? []

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h2 className="text-2xl font-semibold">Routines</h2>
          <p className="mt-1 text-sm text-slate-600">Create and manage your workout templates.</p>
        </div>
        <Link
          to="/routines/new"
          className="inline-flex justify-center rounded-md bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-800"
        >
          Create routine
        </Link>
      </div>

      {routines.length === 0 ? (
        <section className="rounded-lg border border-slate-200 bg-white p-6 shadow-sm">
          <p className="text-slate-600">You do not have any routines yet.</p>
          <Link
            to="/routines/new"
            className="mt-4 inline-flex rounded-md border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50"
          >
            Create your first routine
          </Link>
        </section>
      ) : (
        <ul className="space-y-3">
          {routines.map((routine) => (
            <li key={routine.id}>
              <Link
                to={`/routines/${routine.id}`}
                className="block rounded-lg border border-slate-200 bg-white p-5 shadow-sm transition hover:border-slate-300 hover:bg-slate-50"
              >
                <div className="flex flex-col gap-2 sm:flex-row sm:items-start sm:justify-between">
                  <div>
                    <h3 className="text-lg font-semibold text-slate-900">{routine.name}</h3>
                    {routine.description ? (
                      <p className="mt-1 text-sm text-slate-600">{routine.description}</p>
                    ) : null}
                  </div>
                  <div className="text-sm text-slate-600">
                    <p>{formatExerciseCount(routine.exerciseCount)}</p>
                    <p className="mt-1">Updated {formatDateTime(routine.updatedAt)}</p>
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
