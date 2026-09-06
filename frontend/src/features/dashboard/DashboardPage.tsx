import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { ApiError } from '../../api/errors'
import { LoadingState } from '../../components/LoadingState'
import { formatDateTime } from '../../lib/formatDateTime'
import { DASHBOARD_QUERY_KEY, fetchDashboard } from './dashboardApi'
import type { DashboardResponse } from './dashboardTypes'

function formatExerciseCount(count: number): string {
  return count === 1 ? '1 exercise' : `${count} exercises`
}

function DashboardContent({ dashboard }: { dashboard: DashboardResponse }) {
  const workoutActionLabel = dashboard.hasActiveSession ? 'Resume Workout' : 'Start Workout'

  return (
    <div className="space-y-6">
      <section className="app-card">
        <h2 className="text-2xl font-semibold">Welcome back, {dashboard.displayName}</h2>
        <p className="mt-2 text-slate-600">Ready for your next workout?</p>
        <Link
          to="/workout"
          className="mt-4 inline-flex rounded-md bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-800"
        >
          {workoutActionLabel}
        </Link>
      </section>

      <section className="grid grid-cols-2 gap-4">
        <div className="app-card-compact">
          <p className="text-sm font-medium text-slate-600">Completed workouts</p>
          <p className="mt-1 text-3xl font-semibold">{dashboard.totalCompletedWorkouts}</p>
        </div>
        <div className="app-card-compact">
          <p className="text-sm font-medium text-slate-600">Completed sets</p>
          <p className="mt-1 text-3xl font-semibold">{dashboard.totalCompletedSets}</p>
        </div>
      </section>

      <section className="app-card">
        <div className="flex items-center justify-between gap-4">
          <h3 className="text-lg font-semibold">Routines</h3>
          <Link to="/routines" className="text-sm font-medium text-slate-900 underline">
            View all
          </Link>
        </div>

        {dashboard.routines.length === 0 ? (
          <p className="mt-4 text-sm text-slate-600">
            No routines yet.{' '}
            <Link to="/routines" className="font-medium text-slate-900 underline">
              Manage routines
            </Link>
          </p>
        ) : (
          <ul className="mt-4 divide-y divide-slate-200">
            {dashboard.routines.map((routine) => (
              <li key={routine.id} className="flex items-center justify-between py-3 first:pt-0 last:pb-0">
                <span className="font-medium text-slate-900">{routine.name}</span>
                <span className="text-sm text-slate-600">{formatExerciseCount(routine.exerciseCount)}</span>
              </li>
            ))}
          </ul>
        )}
      </section>

      <section className="rounded-lg border border-slate-200 bg-white p-6 shadow-sm">
        <h3 className="text-lg font-semibold">Most recent workout</h3>
        {dashboard.mostRecentCompleted ? (
          <div className="mt-4">
            <p className="font-medium text-slate-900">{dashboard.mostRecentCompleted.name}</p>
            <p className="mt-1 text-sm text-slate-600">
              Completed {formatDateTime(dashboard.mostRecentCompleted.completedAt)}
            </p>
          </div>
        ) : (
          <p className="mt-4 text-sm text-slate-600">No completed workouts yet.</p>
        )}
      </section>
    </div>
  )
}

export function DashboardPage() {
  const { data, isPending, isError, error, refetch, isFetching } = useQuery({
    queryKey: DASHBOARD_QUERY_KEY,
    queryFn: fetchDashboard,
  })

  if (isPending) {
    return <LoadingState message="Loading dashboard…" />
  }

  if (isError) {
    const message =
      error instanceof ApiError ? error.message : 'Unable to load dashboard. Please try again.'

    return (
      <section className="rounded-lg border border-red-200 bg-red-50 p-6 shadow-sm" role="alert">
        <h2 className="text-lg font-semibold text-red-800">Dashboard unavailable</h2>
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

  return <DashboardContent dashboard={data} />
}
