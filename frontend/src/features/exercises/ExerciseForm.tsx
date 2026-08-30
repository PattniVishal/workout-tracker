import type { FormEvent } from 'react'
import { useState } from 'react'
import { ApiError } from '../../api/errors'
import { AuthFormField } from '../../components/AuthFormField'
import { fieldErrorsToMap } from '../../lib/formErrors'
import type { SaveExerciseRequest } from './exerciseTypes'

type ExerciseFormProps = {
  mode: 'create' | 'edit'
  initialName?: string
  initialPrimaryMuscleGroup?: string
  initialSecondaryMuscleGroups?: string[]
  initialCategory?: string
  isSubmitting: boolean
  onSubmit: (request: SaveExerciseRequest) => Promise<void>
}

function parseSecondaryMuscleGroups(value: string): string[] {
  return value
    .split(',')
    .map((group) => group.trim())
    .filter((group) => group.length > 0)
}

function toSaveRequest(
  name: string,
  primaryMuscleGroup: string,
  secondaryMuscleGroupsInput: string,
  category: string,
): SaveExerciseRequest {
  return {
    name: name.trim(),
    primaryMuscleGroup: primaryMuscleGroup.trim(),
    secondaryMuscleGroups: parseSecondaryMuscleGroups(secondaryMuscleGroupsInput),
    category: category.trim(),
  }
}

export function ExerciseForm({
  mode,
  initialName = '',
  initialPrimaryMuscleGroup = '',
  initialSecondaryMuscleGroups = [],
  initialCategory = '',
  isSubmitting,
  onSubmit,
}: ExerciseFormProps) {
  const [name, setName] = useState(initialName)
  const [primaryMuscleGroup, setPrimaryMuscleGroup] = useState(initialPrimaryMuscleGroup)
  const [secondaryMuscleGroupsInput, setSecondaryMuscleGroupsInput] = useState(
    initialSecondaryMuscleGroups.join(', '),
  )
  const [category, setCategory] = useState(initialCategory)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [formError, setFormError] = useState<string | null>(null)

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    if (isSubmitting) {
      return
    }

    const clientErrors: Record<string, string> = {}

    if (!name.trim()) {
      clientErrors.name = 'Name is required.'
    }

    if (!primaryMuscleGroup.trim()) {
      clientErrors.primaryMuscleGroup = 'Primary muscle group is required.'
    }

    if (!category.trim()) {
      clientErrors.category = 'Category is required.'
    }

    if (Object.keys(clientErrors).length > 0) {
      setFieldErrors(clientErrors)
      setFormError(null)
      return
    }

    setFieldErrors({})
    setFormError(null)

    try {
      await onSubmit(
        toSaveRequest(name, primaryMuscleGroup, secondaryMuscleGroupsInput, category),
      )
    } catch (error) {
      if (error instanceof ApiError) {
        setFieldErrors(fieldErrorsToMap(error.fieldErrors))
        setFormError(error.message)
      } else {
        setFormError('Unable to save exercise. Please try again.')
      }
    }
  }

  return (
    <form className="space-y-6" onSubmit={handleSubmit} noValidate>
      {formError ? (
        <p className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700" role="alert">
          {formError}
        </p>
      ) : null}

      <AuthFormField
        id="name"
        label="Exercise name"
        value={name}
        onChange={setName}
        error={fieldErrors.name}
        disabled={isSubmitting}
      />

      <AuthFormField
        id="primaryMuscleGroup"
        label="Primary muscle group"
        value={primaryMuscleGroup}
        onChange={setPrimaryMuscleGroup}
        error={fieldErrors.primaryMuscleGroup}
        disabled={isSubmitting}
      />

      <div>
        <label htmlFor="secondaryMuscleGroups" className="block text-sm font-medium text-slate-700">
          Secondary muscle groups
        </label>
        <input
          id="secondaryMuscleGroups"
          name="secondaryMuscleGroups"
          type="text"
          value={secondaryMuscleGroupsInput}
          onChange={(event) => setSecondaryMuscleGroupsInput(event.target.value)}
          disabled={isSubmitting}
          placeholder="Comma-separated, e.g. Triceps, Shoulders"
          className="mt-1 block w-full rounded-md border border-slate-300 px-3 py-2 text-sm shadow-sm focus:border-slate-500 focus:outline-none focus:ring-1 focus:ring-slate-500 disabled:bg-slate-100"
        />
        {fieldErrors.secondaryMuscleGroups ? (
          <p className="mt-1 text-sm text-red-600" role="alert">
            {fieldErrors.secondaryMuscleGroups}
          </p>
        ) : (
          <p className="mt-1 text-xs text-slate-500">Optional. Separate multiple groups with commas.</p>
        )}
      </div>

      <AuthFormField
        id="category"
        label="Category"
        value={category}
        onChange={setCategory}
        error={fieldErrors.category}
        disabled={isSubmitting}
      />

      <button
        type="submit"
        disabled={isSubmitting}
        className="rounded-md bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-800 disabled:cursor-not-allowed disabled:bg-slate-400"
      >
        {isSubmitting ? 'Saving…' : mode === 'create' ? 'Create exercise' : 'Save changes'}
      </button>
    </form>
  )
}
