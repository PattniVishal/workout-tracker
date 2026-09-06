import type { FormEvent } from 'react'
import { useState } from 'react'
import { ApiError } from '../../api/errors'
import { AuthFormField } from '../../components/AuthFormField'
import { fieldErrorsToMap } from '../../lib/formErrors'
import { ExercisePicker } from './ExercisePicker'
import type { Exercise, RoutineFormExercise, SaveRoutineRequest } from './routineTypes'

type RoutineFormProps = {
  mode: 'create' | 'edit'
  initialName?: string
  initialDescription?: string | null
  initialExercises?: RoutineFormExercise[]
  isSubmitting: boolean
  onSubmit: (request: SaveRoutineRequest) => Promise<void>
}

function toSaveRequest(
  name: string,
  description: string,
  exercises: RoutineFormExercise[],
): SaveRoutineRequest {
  return {
    name: name.trim(),
    description: description.trim() ? description.trim() : null,
    exercises: exercises.map((exercise) => ({
      exerciseId: exercise.exerciseId,
      plannedSetCount: exercise.plannedSetCount,
    })),
  }
}

export function RoutineForm({
  mode,
  initialName = '',
  initialDescription = '',
  initialExercises = [],
  isSubmitting,
  onSubmit,
}: RoutineFormProps) {
  const [name, setName] = useState(initialName)
  const [description, setDescription] = useState(initialDescription ?? '')
  const [exercises, setExercises] = useState<RoutineFormExercise[]>(initialExercises)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [formError, setFormError] = useState<string | null>(null)

  function handleAddExercise(exercise: Exercise) {
    if (exercises.some((item) => item.exerciseId === exercise.id)) {
      return
    }

    setExercises((current) => [
      ...current,
      {
        exerciseId: exercise.id,
        exerciseName: exercise.name,
        plannedSetCount: 3,
      },
    ])
    setFormError(null)
  }

  function handleRemoveExercise(exerciseId: string) {
    setExercises((current) => current.filter((exercise) => exercise.exerciseId !== exerciseId))
  }

  function handlePlannedSetCountChange(exerciseId: string, value: string) {
    const parsed = Number.parseInt(value, 10)
    setExercises((current) =>
      current.map((exercise) =>
        exercise.exerciseId === exerciseId
          ? { ...exercise, plannedSetCount: Number.isNaN(parsed) ? 1 : parsed }
          : exercise,
      ),
    )
  }

  function moveExercise(exerciseId: string, direction: -1 | 1) {
    setExercises((current) => {
      const index = current.findIndex((exercise) => exercise.exerciseId === exerciseId)
      if (index < 0) {
        return current
      }

      const targetIndex = index + direction
      if (targetIndex < 0 || targetIndex >= current.length) {
        return current
      }

      const next = [...current]
      const [moved] = next.splice(index, 1)
      next.splice(targetIndex, 0, moved)
      return next
    })
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    if (isSubmitting) {
      return
    }

    const clientErrors: Record<string, string> = {}

    if (!name.trim()) {
      clientErrors.name = 'Name is required.'
    }

    for (const exercise of exercises) {
      if (exercise.plannedSetCount < 1) {
        clientErrors.exercises = 'Each exercise must have at least 1 planned set.'
        break
      }
    }

    if (Object.keys(clientErrors).length > 0) {
      setFieldErrors(clientErrors)
      setFormError(null)
      return
    }

    setFieldErrors({})
    setFormError(null)

    try {
      await onSubmit(toSaveRequest(name, description, exercises))
    } catch (error) {
      if (error instanceof ApiError) {
        setFieldErrors(fieldErrorsToMap(error.fieldErrors))
        setFormError(error.message)
      } else {
        setFormError('Unable to save routine. Please try again.')
      }
    }
  }

  const selectedExerciseIds = exercises.map((exercise) => exercise.exerciseId)

  return (
    <form className="space-y-6" onSubmit={handleSubmit} noValidate>
      {formError ? (
        <p className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700" role="alert">
          {formError}
        </p>
      ) : null}

      <AuthFormField
        id="name"
        label="Routine name"
        value={name}
        onChange={setName}
        error={fieldErrors.name}
        disabled={isSubmitting}
      />

      <div>
        <label htmlFor="description" className="block text-sm font-medium text-slate-700">
          Description
        </label>
        <textarea
          id="description"
          value={description}
          onChange={(event) => setDescription(event.target.value)}
          rows={3}
          disabled={isSubmitting}
          className="mt-1 block w-full rounded-md border border-slate-300 px-3 py-2 text-sm shadow-sm focus:border-slate-500 focus:outline-none focus:ring-1 focus:ring-slate-500 disabled:bg-slate-100"
        />
      </div>

      <section className="space-y-3">
        <div>
          <h2 className="text-lg font-semibold">Exercises</h2>
          <p className="mt-1 text-sm text-slate-600">
            Exercise order in this list is the routine order sent to the server.
          </p>
        </div>

        {fieldErrors.exercises ? (
          <p className="text-sm text-red-600" role="alert">
            {fieldErrors.exercises}
          </p>
        ) : null}

        {exercises.length === 0 ? (
          <p className="text-sm text-slate-600">No exercises added yet.</p>
        ) : (
          <ul className="space-y-3">
            {exercises.map((exercise, index) => (
              <li
                key={exercise.exerciseId}
                className="rounded-md border border-slate-200 bg-white p-4 shadow-sm"
              >
                <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                  <div>
                    <p className="font-medium text-slate-900">
                      {index + 1}. {exercise.exerciseName}
                    </p>
                  </div>
                  <div className="flex flex-wrap gap-2">
                    <button
                      type="button"
                      onClick={() => moveExercise(exercise.exerciseId, -1)}
                      disabled={isSubmitting || index === 0}
                      className="rounded-md border border-slate-300 px-2 py-1 text-xs font-medium text-slate-700 hover:bg-slate-50 disabled:opacity-60"
                    >
                      Move up
                    </button>
                    <button
                      type="button"
                      onClick={() => moveExercise(exercise.exerciseId, 1)}
                      disabled={isSubmitting || index === exercises.length - 1}
                      className="rounded-md border border-slate-300 px-2 py-1 text-xs font-medium text-slate-700 hover:bg-slate-50 disabled:opacity-60"
                    >
                      Move down
                    </button>
                    <button
                      type="button"
                      onClick={() => handleRemoveExercise(exercise.exerciseId)}
                      disabled={isSubmitting}
                      className="rounded-md border border-red-200 px-2 py-1 text-xs font-medium text-red-700 hover:bg-red-50 disabled:opacity-60"
                    >
                      Remove
                    </button>
                  </div>
                </div>
                <div className="mt-3 max-w-xs">
                  <label
                    htmlFor={`planned-set-count-${exercise.exerciseId}`}
                    className="block text-sm font-medium text-slate-700"
                  >
                    Planned sets
                  </label>
                  <input
                    id={`planned-set-count-${exercise.exerciseId}`}
                    type="number"
                    min={1}
                    value={exercise.plannedSetCount}
                    onChange={(event) =>
                      handlePlannedSetCountChange(exercise.exerciseId, event.target.value)
                    }
                    disabled={isSubmitting}
                    className="mt-1 block w-full rounded-md border border-slate-300 px-3 py-2 text-sm shadow-sm focus:border-slate-500 focus:outline-none focus:ring-1 focus:ring-slate-500"
                  />
                </div>
              </li>
            ))}
          </ul>
        )}
      </section>

      <ExercisePicker selectedExerciseIds={selectedExerciseIds} onSelect={handleAddExercise} />

      <div className="flex flex-wrap gap-3">
        <button
          type="submit"
          disabled={isSubmitting}
          className="rounded-md bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-800 disabled:cursor-not-allowed disabled:bg-slate-400"
        >
          {isSubmitting ? 'Saving…' : mode === 'create' ? 'Create routine' : 'Save changes'}
        </button>
      </div>
    </form>
  )
}
