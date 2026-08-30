import { useState } from 'react'
import { AccordionChevron } from '../../components/AccordionChevron'
import type { UpdateSetRequest } from './workoutTypes'
import { PreviousPerformancePanel } from './PreviousPerformancePanel'
import { sortSessionSets } from './workoutApi'
import { WorkoutSetRow } from './WorkoutSetRow'
import type { SessionExercise } from './workoutTypes'

type WorkoutExerciseCardProps = {
  exercise: SessionExercise
  isExpanded: boolean
  onToggle: () => void
  disabled?: boolean
  savingSetId: string | null
  onAddSet: (workoutExerciseId: string) => Promise<void>
  onUpdateSet: (
    workoutExerciseId: string,
    setId: string,
    request: UpdateSetRequest,
  ) => Promise<void>
  onDeleteSet: (workoutExerciseId: string, setId: string) => Promise<void>
  onRemoveExercise: (workoutExerciseId: string) => Promise<void>
}

export function WorkoutExerciseCard({
  exercise,
  isExpanded,
  onToggle,
  disabled = false,
  savingSetId,
  onAddSet,
  onUpdateSet,
  onDeleteSet,
  onRemoveExercise,
}: WorkoutExerciseCardProps) {
  const [isAddingSet, setIsAddingSet] = useState(false)
  const sets = sortSessionSets(exercise.sets)
  const completedSetCount = sets.filter((set) => set.completed).length

  return (
    <section className="app-card-compact overflow-hidden">
      <div className="flex items-start justify-between gap-3">
        <button
          type="button"
          onClick={onToggle}
          aria-expanded={isExpanded}
          aria-label={`${isExpanded ? 'Collapse' : 'Expand'} ${exercise.exerciseName}`}
          className="flex min-w-0 flex-1 items-start gap-3 rounded-md py-1 text-left focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-slate-500"
        >
          <span className="mt-0.5">
            <AccordionChevron expanded={isExpanded} />
          </span>
          <span className="min-w-0">
            <span className="block text-lg font-semibold text-slate-900">
              {exercise.position}. {exercise.exerciseName}
            </span>
            {!isExpanded ? (
              <span className="mt-1 block text-sm text-slate-600">
                {sets.length} {sets.length === 1 ? 'set' : 'sets'}
                {completedSetCount > 0
                  ? ` · ${completedSetCount} completed`
                  : ''}
              </span>
            ) : null}
          </span>
        </button>
        {isExpanded ? (
          <button
            type="button"
            onClick={() => void onRemoveExercise(exercise.id)}
            disabled={disabled}
            className="shrink-0 rounded-md border border-red-200 px-3 py-1.5 text-sm font-medium text-red-700 hover:bg-red-50 disabled:opacity-60"
          >
            Remove exercise
          </button>
        ) : null}
      </div>

      {isExpanded ? (
        <div className="mt-4 space-y-4 border-t border-slate-200 pt-4">
          <PreviousPerformancePanel exerciseId={exercise.exerciseId} />

          <div className="space-y-3">
            {sets.map((set) => (
              <WorkoutSetRow
                key={set.id}
                set={set}
                disabled={disabled}
                isSaving={savingSetId === set.id}
                onSave={(values) => onUpdateSet(exercise.id, set.id, values)}
                onDelete={() => onDeleteSet(exercise.id, set.id)}
              />
            ))}
          </div>

          <button
            type="button"
            onClick={async () => {
              setIsAddingSet(true)
              try {
                await onAddSet(exercise.id)
              } finally {
                setIsAddingSet(false)
              }
            }}
            disabled={disabled || isAddingSet}
            className="rounded-md border border-slate-300 bg-white px-3 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50 disabled:opacity-60"
          >
            {isAddingSet ? 'Adding set…' : 'Add set'}
          </button>
        </div>
      ) : null}
    </section>
  )
}
