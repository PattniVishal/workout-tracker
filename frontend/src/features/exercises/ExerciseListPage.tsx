import { useState, type FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { ApiError } from '../../api/errors'
import { useConfirm } from '../../components/ConfirmProvider'
import { LoadingState } from '../../components/LoadingState'
import {
  archiveExercise,
  exercisesQueryKey,
  fetchExercises,
  invalidateExerciseQueries,
} from './exercisesApi'
import { getExerciseSourceLabel, isCustomExercise } from './exerciseTypes'
import { useMuscleGroupOptions } from './useMuscleGroupOptions'

export function ExerciseListPage() {
  const queryClient = useQueryClient()
  const confirm = useConfirm()
  const [searchQuery, setSearchQuery] = useState('')
  const [muscleGroup, setMuscleGroup] = useState('')
  const [appliedFilters, setAppliedFilters] = useState({ q: '', muscleGroup: '' })
  const [actionError, setActionError] = useState<string | null>(null)
  const [archivingExerciseId, setArchivingExerciseId] = useState<string | null>(null)

  const { data, isPending, isError, error, refetch, isFetching } = useQuery({
    queryKey: exercisesQueryKey(appliedFilters),
    queryFn: () => fetchExercises(appliedFilters),
  })

  const archiveMutation = useMutation({
    mutationFn: (exerciseId: string) => archiveExercise(exerciseId),
    onSuccess: async () => {
      await invalidateExerciseQueries(queryClient)
      setActionError(null)
    },
    onError: (mutationError) => {
      if (mutationError instanceof ApiError) {
        setActionError(mutationError.message)
      } else {
        setActionError('Unable to archive exercise. Please try again.')
      }
    },
    onSettled: () => {
      setArchivingExerciseId(null)
    },
  })

  const muscleGroupOptions = useMuscleGroupOptions()

  function handleApplyFilters(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setAppliedFilters({
      q: searchQuery.trim(),
      muscleGroup,
    })
    setActionError(null)
  }

  async function handleArchive(exerciseId: string, exerciseName: string) {
    if (archiveMutation.isPending) {
      return
    }

    const confirmed = await confirm({
      title: 'Archive exercise',
      message: `Archive ${exerciseName}? It will no longer be available for new routine or workout selections.`,
      confirmLabel: 'Archive',
      destructive: true,
    })

    if (!confirmed) {
      return
    }

    setActionError(null)
    setArchivingExerciseId(exerciseId)
    archiveMutation.mutate(exerciseId)
  }

  if (isPending) {
    return <LoadingState message="Loading exercises…" />
  }

  if (isError) {
    const message =
      error instanceof ApiError ? error.message : 'Unable to load exercises. Please try again.'

    return (
      <section className="rounded-lg border border-red-200 bg-red-50 p-6 shadow-sm" role="alert">
        <h2 className="text-lg font-semibold text-red-800">Exercises unavailable</h2>
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

  const exercises = data?.exercises ?? []
  const hasActiveFilters = Boolean(appliedFilters.q || appliedFilters.muscleGroup)

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h2 className="text-2xl font-semibold">Exercises</h2>
          <p className="mt-1 text-sm text-slate-600">Browse system exercises and manage your custom exercises.</p>
        </div>
        <Link
          to="/exercises/new"
          className="inline-flex justify-center rounded-md bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-800"
        >
          Create exercise
        </Link>
      </div>

      <form className="app-panel app-form-grid" onSubmit={handleApplyFilters}>
        <input
          type="search"
          value={searchQuery}
          onChange={(event) => setSearchQuery(event.target.value)}
          placeholder="Search by name"
          className="app-input"
        />
        <select
          value={muscleGroup}
          onChange={(event) => setMuscleGroup(event.target.value)}
          aria-label="Muscle group filter"
          className="app-select"
        >
          <option value="">All muscle groups</option>
          {muscleGroupOptions.map((group) => (
            <option key={group} value={group}>
              {group}
            </option>
          ))}
        </select>
        <button
          type="submit"
          className="rounded-md border border-slate-300 bg-white px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-100"
        >
          Search
        </button>
      </form>

      {actionError ? (
        <p className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700" role="alert">
          {actionError}
        </p>
      ) : null}

      {exercises.length === 0 ? (
        <section className="rounded-lg border border-slate-200 bg-white p-6 shadow-sm">
          <p className="text-slate-600">
            {hasActiveFilters
              ? 'No exercises match your filters.'
              : 'No exercises are available yet.'}
          </p>
          {!hasActiveFilters ? (
            <Link
              to="/exercises/new"
              className="mt-4 inline-flex rounded-md border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50"
            >
              Create your first exercise
            </Link>
          ) : null}
        </section>
      ) : (
        <ul className="space-y-3">
          {exercises.map((exercise) => {
            const isArchiving = archivingExerciseId === exercise.id && archiveMutation.isPending

            return (
              <li
                key={exercise.id}
                className="rounded-lg border border-slate-200 bg-white p-5 shadow-sm"
              >
                <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                  <div>
                    <div className="flex flex-wrap items-center gap-2">
                      <h3 className="text-lg font-semibold text-slate-900">{exercise.name}</h3>
                      <span
                        className={`inline-flex rounded-full px-2.5 py-0.5 text-xs font-medium ${
                          isCustomExercise(exercise)
                            ? 'bg-blue-100 text-blue-800'
                            : 'bg-slate-100 text-slate-700'
                        }`}
                      >
                        {getExerciseSourceLabel(exercise.source)}
                      </span>
                    </div>
                    <p className="mt-1 text-sm text-slate-600">
                      {exercise.primaryMuscleGroup}
                      {exercise.secondaryMuscleGroups.length > 0
                        ? ` · ${exercise.secondaryMuscleGroups.join(', ')}`
                        : ''}
                    </p>
                    <p className="mt-1 text-xs text-slate-500">{exercise.category}</p>
                  </div>
                  {isCustomExercise(exercise) ? (
                    <div className="flex flex-wrap gap-2">
                      <Link
                        to={`/exercises/${exercise.id}/edit`}
                        className="rounded-md border border-slate-300 px-3 py-1.5 text-sm font-medium text-slate-700 hover:bg-slate-50"
                      >
                        Edit
                      </Link>
                      <button
                        type="button"
                        onClick={() => handleArchive(exercise.id, exercise.name)}
                        disabled={archiveMutation.isPending}
                        className="rounded-md border border-red-300 px-3 py-1.5 text-sm font-medium text-red-700 hover:bg-red-50 disabled:opacity-60"
                      >
                        {isArchiving ? 'Archiving…' : 'Archive'}
                      </button>
                    </div>
                  ) : null}
                </div>
              </li>
            )
          })}
        </ul>
      )}
    </div>
  )
}
