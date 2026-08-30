import { useQuery } from '@tanstack/react-query'
import { ApiError } from '../../api/errors'
import { LoadingState } from '../../components/LoadingState'
import { ActiveWorkoutView } from './ActiveWorkoutView'
import { CURRENT_SESSION_QUERY_KEY, fetchCurrentSession } from './workoutApi'
import { WorkoutStartPanel } from './WorkoutStartPanel'

export function WorkoutPage() {
  const { data: session, isPending, isError, error, refetch, isFetching } = useQuery({
    queryKey: CURRENT_SESSION_QUERY_KEY,
    queryFn: fetchCurrentSession,
  })

  if (isPending) {
    return <LoadingState message="Loading workout…" />
  }

  if (isError) {
    const message =
      error instanceof ApiError ? error.message : 'Unable to load workout. Please try again.'

    return (
      <section className="rounded-lg border border-red-200 bg-red-50 p-6 shadow-sm" role="alert">
        <h2 className="text-lg font-semibold text-red-800">Workout unavailable</h2>
        <p className="mt-2 text-sm text-red-700">{message}</p>
        <button
          type="button"
          onClick={() => refetch()}
          disabled={isFetching}
          className="mt-4 rounded-md border border-red-300 bg-white px-4 py-2 text-sm font-medium text-red-800 hover:bg-red-100 disabled:opacity-60"
        >
          {isFetching ? 'Retrying…' : 'Try again'}
        </button>
      </section>
    )
  }

  if (!session) {
    return <WorkoutStartPanel />
  }

  return <ActiveWorkoutView session={session} />
}
