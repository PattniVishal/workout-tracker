import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { ApiError } from '../../api/errors'
import { LoadingState } from '../../components/LoadingState'
import { ExerciseForm } from './ExerciseForm'
import {
  createExercise,
  exercisesQueryKey,
  fetchExercises,
  invalidateExerciseQueries,
  updateExercise,
} from './exercisesApi'
import { isCustomExercise } from './exerciseTypes'
import type { SaveExerciseRequest } from './exerciseTypes'

type ExerciseFormPageProps = {
  mode: 'create' | 'edit'
}

export function ExerciseFormPage({ mode }: ExerciseFormPageProps) {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { exerciseId } = useParams<{ exerciseId: string }>()

  const exerciseQuery = useQuery({
    queryKey: exercisesQueryKey({}),
    queryFn: () => fetchExercises({}),
    enabled: mode === 'edit' && Boolean(exerciseId),
    select: (data) => data.exercises.find((exercise) => exercise.id === exerciseId),
  })

  const createMutation = useMutation({
    mutationFn: (request: SaveExerciseRequest) => createExercise(request),
    onSuccess: async () => {
      await invalidateExerciseQueries(queryClient)
      navigate('/exercises', { replace: true })
    },
  })

  const updateMutation = useMutation({
    mutationFn: (request: SaveExerciseRequest) => updateExercise(exerciseId!, request),
    onSuccess: async () => {
      await invalidateExerciseQueries(queryClient)
      navigate('/exercises', { replace: true })
    },
  })

  const isSubmitting = createMutation.isPending || updateMutation.isPending

  async function handleSubmit(request: SaveExerciseRequest) {
    if (isSubmitting) {
      return
    }

    try {
      if (mode === 'create') {
        await createMutation.mutateAsync(request)
      } else {
        await updateMutation.mutateAsync(request)
      }
    } catch {
      // ExerciseForm handles ApiError display.
    }
  }

  if (mode === 'edit') {
    if (!exerciseId) {
      return (
        <section className="rounded-lg border border-red-200 bg-red-50 p-6 shadow-sm" role="alert">
          <p className="text-sm text-red-700">Exercise not found.</p>
          <Link to="/exercises" className="mt-4 inline-flex text-sm font-medium text-slate-900 underline">
            Back to exercises
          </Link>
        </section>
      )
    }

    if (exerciseQuery.isPending) {
      return <LoadingState message="Loading exercise…" />
    }

    if (exerciseQuery.isError) {
      const message =
        exerciseQuery.error instanceof ApiError
          ? exerciseQuery.error.message
          : 'Unable to load exercise. Please try again.'

      return (
        <section className="rounded-lg border border-red-200 bg-red-50 p-6 shadow-sm" role="alert">
          <h2 className="text-lg font-semibold text-red-800">Exercise unavailable</h2>
          <p className="mt-2 text-sm text-red-700">{message}</p>
          <Link
            to="/exercises"
            className="mt-4 inline-flex rounded-md border border-slate-300 bg-white px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50"
          >
            Back to exercises
          </Link>
        </section>
      )
    }

    const exercise = exerciseQuery.data

    if (!exercise) {
      return (
        <section className="rounded-lg border border-red-200 bg-red-50 p-6 shadow-sm" role="alert">
          <h2 className="text-lg font-semibold text-red-800">Exercise not found</h2>
          <p className="mt-2 text-sm text-red-700">This exercise is not available in your library.</p>
          <Link
            to="/exercises"
            className="mt-4 inline-flex rounded-md border border-slate-300 bg-white px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50"
          >
            Back to exercises
          </Link>
        </section>
      )
    }

    if (!isCustomExercise(exercise)) {
      return (
        <section className="rounded-lg border border-red-200 bg-red-50 p-6 shadow-sm" role="alert">
          <h2 className="text-lg font-semibold text-red-800">System exercise</h2>
          <p className="mt-2 text-sm text-red-700">System exercises cannot be edited.</p>
          <Link
            to="/exercises"
            className="mt-4 inline-flex rounded-md border border-slate-300 bg-white px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50"
          >
            Back to exercises
          </Link>
        </section>
      )
    }
  }

  const exercise = exerciseQuery.data

  return (
    <div className="space-y-6">
      <div>
        <Link
          to="/exercises"
          className="text-sm font-medium text-slate-600 underline"
        >
          Back to exercises
        </Link>
        <h2 className="mt-2 text-2xl font-semibold">
          {mode === 'create' ? 'Create exercise' : 'Edit exercise'}
        </h2>
      </div>

      <section className="rounded-lg border border-slate-200 bg-white p-6 shadow-sm">
        <ExerciseForm
          mode={mode}
          initialName={mode === 'edit' ? exercise?.name : ''}
          initialPrimaryMuscleGroup={mode === 'edit' ? exercise?.primaryMuscleGroup : ''}
          initialSecondaryMuscleGroups={mode === 'edit' ? exercise?.secondaryMuscleGroups : []}
          initialCategory={mode === 'edit' ? exercise?.category : ''}
          isSubmitting={isSubmitting}
          onSubmit={handleSubmit}
        />
      </section>
    </div>
  )
}
