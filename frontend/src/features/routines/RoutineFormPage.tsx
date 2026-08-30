import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { ApiError } from '../../api/errors'
import { LoadingState } from '../../components/LoadingState'
import { RoutineForm } from './RoutineForm'
import {
  createRoutine,
  fetchRoutine,
  routineDetailQueryKey,
  ROUTINES_QUERY_KEY,
  updateRoutine,
} from './routinesApi'
import type { RoutineFormExercise, SaveRoutineRequest } from './routineTypes'

type RoutineFormPageProps = {
  mode: 'create' | 'edit'
}

function toFormExercises(
  exercises: Array<{
    exerciseId: string
    exerciseName: string
    plannedSetCount: number
    position: number
  }>,
): RoutineFormExercise[] {
  return [...exercises]
    .sort((left, right) => left.position - right.position)
    .map((exercise) => ({
      exerciseId: exercise.exerciseId,
      exerciseName: exercise.exerciseName,
      plannedSetCount: exercise.plannedSetCount,
    }))
}

export function RoutineFormPage({ mode }: RoutineFormPageProps) {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { routineId } = useParams<{ routineId: string }>()

  const routineQuery = useQuery({
    queryKey: routineDetailQueryKey(routineId ?? ''),
    queryFn: () => fetchRoutine(routineId!),
    enabled: mode === 'edit' && Boolean(routineId),
  })

  const createMutation = useMutation({
    mutationFn: (request: SaveRoutineRequest) => createRoutine(request),
    onSuccess: async (routine) => {
      await queryClient.invalidateQueries({ queryKey: ROUTINES_QUERY_KEY })
      navigate(`/routines/${routine.id}`, { replace: true })
    },
  })

  const updateMutation = useMutation({
    mutationFn: (request: SaveRoutineRequest) => updateRoutine(routineId!, request),
    onSuccess: async (routine) => {
      await queryClient.invalidateQueries({ queryKey: ROUTINES_QUERY_KEY })
      await queryClient.invalidateQueries({ queryKey: routineDetailQueryKey(routine.id) })
      navigate(`/routines/${routine.id}`, { replace: true })
    },
  })

  const isSubmitting = createMutation.isPending || updateMutation.isPending

  async function handleSubmit(request: SaveRoutineRequest) {
    if (mode === 'create') {
      await createMutation.mutateAsync(request)
      return
    }

    await updateMutation.mutateAsync(request)
  }

  if (mode === 'edit') {
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

    if (routineQuery.isPending) {
      return <LoadingState message="Loading routine…" />
    }

    if (routineQuery.isError) {
      const message =
        routineQuery.error instanceof ApiError
          ? routineQuery.error.message
          : 'Unable to load routine. Please try again.'
      const isNotFound =
        routineQuery.error instanceof ApiError && routineQuery.error.status === 404

      return (
        <section className="rounded-lg border border-red-200 bg-red-50 p-6 shadow-sm" role="alert">
          <h2 className="text-lg font-semibold text-red-800">
            {isNotFound ? 'Routine not found' : 'Routine unavailable'}
          </h2>
          <p className="mt-2 text-sm text-red-700">{message}</p>
          <Link
            to="/routines"
            className="mt-4 inline-flex rounded-md border border-slate-300 bg-white px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50"
          >
            Back to routines
          </Link>
        </section>
      )
    }
  }

  return (
    <div className="space-y-6">
      <div>
        <Link
          to={mode === 'create' ? '/routines' : `/routines/${routineId}`}
          className="text-sm font-medium text-slate-600 underline"
        >
          {mode === 'create' ? 'Back to routines' : 'Back to routine'}
        </Link>
        <h2 className="mt-2 text-2xl font-semibold">
          {mode === 'create' ? 'Create routine' : 'Edit routine'}
        </h2>
      </div>

      <section className="rounded-lg border border-slate-200 bg-white p-6 shadow-sm">
        <RoutineForm
          mode={mode}
          initialName={mode === 'edit' ? routineQuery.data?.name : ''}
          initialDescription={mode === 'edit' ? routineQuery.data?.description : ''}
          initialExercises={
            mode === 'edit' && routineQuery.data
              ? toFormExercises(routineQuery.data.exercises)
              : []
          }
          isSubmitting={isSubmitting}
          onSubmit={handleSubmit}
        />
      </section>
    </div>
  )
}
