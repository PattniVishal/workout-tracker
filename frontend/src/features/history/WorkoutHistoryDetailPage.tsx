import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { ApiError } from '../../api/errors'
import { useConfirm } from '../../components/ConfirmProvider'
import { LoadingState } from '../../components/LoadingState'
import { formatDateTime } from '../../lib/formatDateTime'
import { formatDuration } from '../../lib/formatDuration'
import { DASHBOARD_QUERY_KEY } from '../dashboard/dashboardApi'
import { sortSessionExercises, sortSessionSets } from '../workout/workoutApi'
import type { SessionSet } from '../workout/workoutTypes'
import {
  deleteHistorySession,
  fetchHistorySession,
  historyDetailQueryKey,
  HISTORY_QUERY_KEY,
} from './historyApi'

function formatWeight(weightKg: number | null): string {
  return weightKg === null ? '—' : `${weightKg} kg`
}

function formatRepetitions(repetitions: number | null): string {
  return repetitions === null ? '—' : `${repetitions}`
}

function HistorySetRow({ set }: { set: SessionSet }) {
  return (
    <li className="flex flex-col gap-2 rounded-md border border-slate-200 p-3 sm:flex-row sm:items-center sm:justify-between">
      <div className="flex flex-wrap items-center gap-3 text-sm text-slate-700">
        <span className="font-medium text-slate-900">Set {set.setNumber}</span>
        <span>{formatWeight(set.weightKg)}</span>
        <span>{formatRepetitions(set.repetitions)} reps</span>
      </div>
      <span
        className={`inline-flex w-fit rounded-full px-2.5 py-0.5 text-xs font-medium ${
          set.completed
            ? 'bg-emerald-100 text-emerald-800'
            : 'bg-slate-100 text-slate-600'
        }`}
      >
        {set.completed ? 'Completed' : 'Incomplete'}
      </span>
    </li>
  )
}

export function WorkoutHistoryDetailPage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const confirm = useConfirm()
  const { sessionId } = useParams<{ sessionId: string }>()
  const [deleteError, setDeleteError] = useState<string | null>(null)

  const { data, isPending, isError, error, refetch, isFetching } = useQuery({
    queryKey: historyDetailQueryKey(sessionId ?? ''),
    queryFn: () => fetchHistorySession(sessionId!),
    enabled: Boolean(sessionId),
  })

  const deleteMutation = useMutation({
    mutationFn: () => deleteHistorySession(sessionId!),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: HISTORY_QUERY_KEY })
      queryClient.removeQueries({ queryKey: historyDetailQueryKey(sessionId!) })
      await queryClient.invalidateQueries({ queryKey: DASHBOARD_QUERY_KEY })
      await queryClient.invalidateQueries({ queryKey: ['workout', 'previous-performance'] })
      navigate('/history', { replace: true })
    },
    onError: (mutationError) => {
      if (mutationError instanceof ApiError) {
        setDeleteError(mutationError.message)
      } else {
        setDeleteError('Unable to delete workout. Please try again.')
      }
    },
  })

  async function handleDelete() {
    if (!sessionId || deleteMutation.isPending) {
      return
    }

    const confirmed = await confirm({
      title: 'Delete workout',
      message:
        'Delete this workout? This will permanently remove the workout and all logged sets.',
      confirmLabel: 'Yes, delete',
      destructive: true,
    })

    if (!confirmed) {
      return
    }

    setDeleteError(null)
    deleteMutation.mutate()
  }

  if (!sessionId) {
    return (
      <section className="rounded-lg border border-red-200 bg-red-50 p-6 shadow-sm" role="alert">
        <p className="text-sm text-red-700">Workout not found.</p>
        <Link to="/history" className="mt-4 inline-flex text-sm font-medium text-slate-900 underline">
          Back to history
        </Link>
      </section>
    )
  }

  if (isPending) {
    return <LoadingState message="Loading workout…" />
  }

  if (isError) {
    const message =
      error instanceof ApiError ? error.message : 'Unable to load workout. Please try again.'
    const isNotFound = error instanceof ApiError && error.status === 404

    return (
      <section className="rounded-lg border border-red-200 bg-red-50 p-6 shadow-sm" role="alert">
        <h2 className="text-lg font-semibold text-red-800">
          {isNotFound ? 'Workout not found' : 'Workout unavailable'}
        </h2>
        <p className="mt-2 text-sm text-red-700">{message}</p>
        <div className="mt-4 flex flex-wrap gap-3">
          {!isNotFound ? (
            <button
              type="button"
              onClick={() => refetch()}
              disabled={isFetching}
              className="rounded-md border border-red-300 bg-white px-4 py-2 text-sm font-medium text-red-800 hover:bg-red-100 disabled:opacity-60"
            >
              {isFetching ? 'Retrying…' : 'Try again'}
            </button>
          ) : null}
          <Link
            to="/history"
            className="inline-flex rounded-md border border-slate-300 bg-white px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50"
          >
            Back to history
          </Link>
        </div>
      </section>
    )
  }

  const sortedExercises = sortSessionExercises(data)

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <Link to="/history" className="text-sm font-medium text-slate-600 underline">
            Back to history
          </Link>
          <div className="mt-2 flex flex-wrap items-center gap-3">
            <h2 className="text-2xl font-semibold">{data.name}</h2>
            <span className="inline-flex rounded-full bg-emerald-100 px-2.5 py-0.5 text-xs font-medium text-emerald-800">
              {data.status}
            </span>
          </div>
          <div className="mt-2 space-y-1 text-sm text-slate-600">
            {data.completedAt ? <p>Completed {formatDateTime(data.completedAt)}</p> : null}
            {data.durationSeconds !== null ? <p>Duration {formatDuration(data.durationSeconds)}</p> : null}
          </div>
        </div>
        <button
          type="button"
          onClick={handleDelete}
          disabled={deleteMutation.isPending}
          className="rounded-md border border-red-300 px-4 py-2 text-sm font-medium text-red-700 hover:bg-red-50 disabled:opacity-60"
        >
          {deleteMutation.isPending ? 'Deleting…' : 'Delete workout'}
        </button>
      </div>

      {deleteError ? (
        <p className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700" role="alert">
          {deleteError}
        </p>
      ) : null}

      <section className="space-y-4">
        {sortedExercises.length === 0 ? (
          <div className="rounded-lg border border-slate-200 bg-white p-6 shadow-sm">
            <p className="text-sm text-slate-600">This workout has no exercises.</p>
          </div>
        ) : (
          sortedExercises.map((exercise) => {
            const sets = sortSessionSets(exercise.sets)

            return (
              <section
                key={exercise.id}
                className="rounded-lg border border-slate-200 bg-white p-5 shadow-sm"
              >
                <h3 className="text-lg font-semibold text-slate-900">
                  {exercise.position}. {exercise.exerciseName}
                </h3>
                {sets.length === 0 ? (
                  <p className="mt-4 text-sm text-slate-600">No sets recorded.</p>
                ) : (
                  <ul className="mt-4 space-y-2">
                    {sets.map((set) => (
                      <HistorySetRow key={set.id} set={set} />
                    ))}
                  </ul>
                )}
              </section>
            )
          })
        )}
      </section>
    </div>
  )
}
