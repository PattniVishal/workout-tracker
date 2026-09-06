import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { ApiError } from '../../api/errors'
import { useConfirm } from '../../components/ConfirmProvider'
import { LoadingState } from '../../components/LoadingState'
import {
  deleteRoutine,
  fetchRoutine,
  routineDetailQueryKey,
  ROUTINES_QUERY_KEY,
} from './routinesApi'

export function RoutineDetailPage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const confirm = useConfirm()
  const { routineId } = useParams<{ routineId: string }>()
  const [deleteError, setDeleteError] = useState<string | null>(null)

  const { data, isPending, isError, error, refetch, isFetching } = useQuery({
    queryKey: routineDetailQueryKey(routineId ?? ''),
    queryFn: () => fetchRoutine(routineId!),
    enabled: Boolean(routineId),
  })

  const deleteMutation = useMutation({
    mutationFn: () => deleteRoutine(routineId!),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ROUTINES_QUERY_KEY })
      queryClient.removeQueries({ queryKey: routineDetailQueryKey(routineId!) })
      navigate('/routines', { replace: true })
    },
    onError: (mutationError) => {
      if (mutationError instanceof ApiError) {
        setDeleteError(mutationError.message)
      } else {
        setDeleteError('Unable to delete routine. Please try again.')
      }
    },
  })

  async function handleDelete() {
    if (!routineId || deleteMutation.isPending) {
      return
    }

    const confirmed = await confirm({
      title: 'Delete routine',
      message: 'Delete this routine? Completed workout history will be preserved.',
      confirmLabel: 'Delete routine',
      destructive: true,
    })

    if (!confirmed) {
      return
    }

    setDeleteError(null)
    deleteMutation.mutate()
  }

  if (!routineId) {
    return (
      <section className="rounded-lg border border-red-200 bg-red-50 p-6 shadow-sm" role="alert">
        <p className="text-sm text-red-700">Routine not found.</p>
        <Link to="/routines" className="mt-4 inline-flex text-sm font-medium text-slate-900 underline">
          Back to routines
        </Link>
      </section>
    )
  }

  if (isPending) {
    return <LoadingState message="Loading routine…" />
  }

  if (isError) {
    const message =
      error instanceof ApiError ? error.message : 'Unable to load routine. Please try again.'
    const isNotFound = error instanceof ApiError && error.status === 404

    return (
      <section className="rounded-lg border border-red-200 bg-red-50 p-6 shadow-sm" role="alert">
        <h2 className="text-lg font-semibold text-red-800">
          {isNotFound ? 'Routine not found' : 'Routine unavailable'}
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
            to="/routines"
            className="inline-flex rounded-md border border-slate-300 bg-white px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50"
          >
            Back to routines
          </Link>
        </div>
      </section>
    )
  }

  const sortedExercises = [...data.exercises].sort((left, right) => left.position - right.position)

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <Link to="/routines" className="text-sm font-medium text-slate-600 underline">
            Back to routines
          </Link>
          <h2 className="mt-2 text-2xl font-semibold">{data.name}</h2>
          {data.description ? <p className="mt-2 text-slate-600">{data.description}</p> : null}
        </div>
        <div className="flex flex-wrap gap-2">
          <Link
            to={`/routines/${data.id}/edit`}
            className="rounded-md border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50"
          >
            Edit
          </Link>
          <button
            type="button"
            onClick={handleDelete}
            disabled={deleteMutation.isPending}
            className="rounded-md border border-red-300 px-4 py-2 text-sm font-medium text-red-700 hover:bg-red-50 disabled:opacity-60"
          >
            {deleteMutation.isPending ? 'Deleting…' : 'Delete'}
          </button>
        </div>
      </div>

      {deleteError ? (
        <p className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700" role="alert">
          {deleteError}
        </p>
      ) : null}

      <section className="rounded-lg border border-slate-200 bg-white p-6 shadow-sm">
        <h3 className="text-lg font-semibold">Exercises</h3>
        {sortedExercises.length === 0 ? (
          <p className="mt-4 text-sm text-slate-600">This routine has no exercises.</p>
        ) : (
          <ol className="mt-4 space-y-3">
            {sortedExercises.map((exercise) => (
              <li
                key={exercise.id}
                className="flex flex-col gap-1 rounded-md border border-slate-200 p-4 sm:flex-row sm:items-center sm:justify-between"
              >
                <div>
                  <p className="font-medium text-slate-900">
                    {exercise.position}. {exercise.exerciseName}
                  </p>
                </div>
                <p className="text-sm text-slate-600">
                  {exercise.plannedSetCount} planned {exercise.plannedSetCount === 1 ? 'set' : 'sets'}
                </p>
              </li>
            ))}
          </ol>
        )}
      </section>
    </div>
  )
}
