import { useEffect, useState } from 'react'
import { NumericStepper } from '../../components/NumericStepper'
import type { SessionSet } from './workoutTypes'

type WorkoutSetRowProps = {
  set: SessionSet
  disabled?: boolean
  isSaving?: boolean
  onSave: (values: {
    weightKg?: number
    repetitions?: number
    completed?: boolean
  }) => Promise<void>
  onDelete: () => Promise<void>
}

function parseOptionalNumber(value: string): number | undefined {
  const trimmed = value.trim()
  if (!trimmed) {
    return undefined
  }

  const parsed = Number(trimmed)
  return Number.isNaN(parsed) ? undefined : parsed
}

export function WorkoutSetRow({
  set,
  disabled = false,
  isSaving = false,
  onSave,
  onDelete,
}: WorkoutSetRowProps) {
  const [weightInput, setWeightInput] = useState(set.weightKg?.toString() ?? '')
  const [repsInput, setRepsInput] = useState(set.repetitions?.toString() ?? '')
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    setWeightInput(set.weightKg?.toString() ?? '')
    setRepsInput(set.repetitions?.toString() ?? '')
    setError(null)
  }, [set.id, set.weightKg, set.repetitions, set.completed])

  async function handleSave() {
    const weightKg = parseOptionalNumber(weightInput)
    const repetitions = parseOptionalNumber(repsInput)

    if (weightKg === undefined || repetitions === undefined) {
      setError('Weight and repetitions are required before completing a set.')
      return
    }

    if (weightKg < 0) {
      setError('Weight cannot be negative.')
      return
    }

    if (repetitions < 0) {
      setError('Repetitions cannot be negative.')
      return
    }

    setError(null)

    await onSave({
      weightKg,
      repetitions,
      completed: true,
    })
  }

  return (
    <div
      className={`rounded-md border p-3 ${
        set.completed ? 'border-emerald-200 bg-emerald-50/60' : 'border-slate-200 bg-white'
      }`}
    >
      <div className="flex flex-col gap-3 sm:flex-row sm:items-end">
        <div className="flex items-center gap-2 text-sm font-medium text-slate-700 sm:w-24">
          <span>Set {set.setNumber}</span>
          {set.completed ? (
            <span className="inline-flex rounded-full bg-emerald-100 px-2 py-0.5 text-xs font-medium text-emerald-800">
              Completed
            </span>
          ) : null}
        </div>
        <div className="grid min-w-0 flex-1 gap-3 sm:grid-cols-2">
          <div>
            <label className="block text-xs font-medium text-slate-600" htmlFor={`weight-${set.id}`}>
              Weight (kg)
            </label>
            <input
              id={`weight-${set.id}`}
              type="number"
              min={0}
              step="0.5"
              value={weightInput}
              onChange={(event) => setWeightInput(event.target.value)}
              disabled={disabled || isSaving}
              className="app-input mt-1"
            />
          </div>
          <NumericStepper
            id={`reps-${set.id}`}
            label="Reps"
            value={repsInput}
            onChange={setRepsInput}
            min={0}
            step={1}
            disabled={disabled || isSaving}
          />
        </div>
        <div className="flex gap-2">
          <button
            type="button"
            onClick={() => void handleSave()}
            disabled={disabled || isSaving}
            className="rounded-md border border-slate-300 bg-white px-3 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50 disabled:opacity-60"
          >
            {isSaving ? 'Saving…' : set.completed ? 'Update set' : 'Save set'}
          </button>
          <button
            type="button"
            onClick={() => void onDelete()}
            disabled={disabled || isSaving}
            className="rounded-md border border-red-200 bg-white px-3 py-2 text-sm font-medium text-red-700 hover:bg-red-50 disabled:opacity-60"
          >
            Delete
          </button>
        </div>
      </div>
      {error ? (
        <p className="mt-2 text-sm text-red-600" role="alert">
          {error}
        </p>
      ) : null}
    </div>
  )
}
