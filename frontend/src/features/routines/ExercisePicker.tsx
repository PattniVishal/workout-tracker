import type { FormEvent } from 'react'
import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { LoadingState } from '../../components/LoadingState'
import { ApiError } from '../../api/errors'
import { useMuscleGroupOptions } from '../exercises/useMuscleGroupOptions'
import { exercisesQueryKey, fetchExercises } from './exercisesApi'
import type { Exercise } from './routineTypes'

type ExercisePickerProps = {
  selectedExerciseIds: string[]
  onSelect: (exercise: Exercise) => void
}

export function ExercisePicker({ selectedExerciseIds, onSelect }: ExercisePickerProps) {
  const [searchQuery, setSearchQuery] = useState('')
  const [muscleGroup, setMuscleGroup] = useState('')
  const [appliedFilters, setAppliedFilters] = useState({ q: '', muscleGroup: '' })

  const muscleGroupOptions = useMuscleGroupOptions()

  const { data, isPending, isError, error, refetch, isFetching } = useQuery({
    queryKey: exercisesQueryKey(appliedFilters),
    queryFn: () => fetchExercises(appliedFilters),
  })

  function handleApplyFilters(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setAppliedFilters({
      q: searchQuery.trim(),
      muscleGroup,
    })
  }

  if (isPending) {
    return <LoadingState message="Loading exercises…" />
  }

  if (isError) {
    const message =
      error instanceof ApiError ? error.message : 'Unable to load exercises. Please try again.'

    return (
      <div className="rounded-md border border-red-200 bg-red-50 p-4" role="alert">
        <p className="text-sm text-red-700">{message}</p>
        <button
          type="button"
          onClick={() => refetch()}
          disabled={isFetching}
          className="mt-3 rounded-md border border-red-300 bg-white px-3 py-1.5 text-sm font-medium text-red-800 hover:bg-red-100 disabled:opacity-60"
        >
          {isFetching ? 'Retrying…' : 'Try again'}
        </button>
      </div>
    )
  }

  const exercises = data?.exercises ?? []

  return (
    <div className="app-panel space-y-4">
      <div>
        <h3 className="text-sm font-semibold text-slate-900">Add exercise</h3>
        <p className="mt-1 text-sm text-slate-600">Search and select exercises to add to this routine.</p>
      </div>

      <form className="app-form-grid" onSubmit={handleApplyFilters}>
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
          className="app-select"
          aria-label="Muscle group filter"
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

      {exercises.length === 0 ? (
        <p className="text-sm text-slate-600">No exercises match your filters.</p>
      ) : (
        <ul className="max-h-72 space-y-2 overflow-y-auto">
          {exercises.map((exercise) => {
            const isSelected = selectedExerciseIds.includes(exercise.id)

            return (
              <li
                key={exercise.id}
                className="flex flex-col gap-3 rounded-md border border-slate-200 bg-slate-50 p-3 sm:flex-row sm:items-center sm:justify-between"
              >
                <div className="min-w-0">
                  <p className="font-medium text-slate-900">{exercise.name}</p>
                  <p className="mt-1 text-sm text-slate-600">
                    {exercise.primaryMuscleGroup}
                    {exercise.secondaryMuscleGroups.length > 0
                      ? ` · ${exercise.secondaryMuscleGroups.join(', ')}`
                      : ''}
                  </p>
                  <p className="mt-1 text-xs text-slate-500">
                    {exercise.category} · {exercise.source}
                  </p>
                </div>
                <button
                  type="button"
                  onClick={() => onSelect(exercise)}
                  disabled={isSelected}
                  aria-label={isSelected ? `${exercise.name} already added` : `Add ${exercise.name}`}
                  className="shrink-0 rounded-md border border-slate-300 bg-white px-3 py-1.5 text-sm font-medium text-slate-700 hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-60"
                >
                  {isSelected ? 'Added' : 'Add'}
                </button>
              </li>
            )
          })}
        </ul>
      )}
    </div>
  )
}
