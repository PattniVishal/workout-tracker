import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { ApiError } from '../../api/errors'
import { useConfirm } from '../../components/ConfirmProvider'
import { DASHBOARD_QUERY_KEY } from '../dashboard/dashboardApi'
import { ElapsedTime } from './ElapsedTime'
import { WorkoutAddExercise } from './WorkoutAddExercise'
import { WorkoutExerciseCard } from './WorkoutExerciseCard'
import {
  addSessionExercise,
  addSessionSet,
  completeCurrentSession,
  CURRENT_SESSION_QUERY_KEY,
  discardCurrentSession,
  removeSessionExercise,
  removeSessionSet,
  sortSessionExercises,
  updateSessionSet,
} from './workoutApi'
import type { SessionResponse, UpdateSetRequest } from './workoutTypes'

type ActiveWorkoutViewProps = {
  session: SessionResponse
}

export function ActiveWorkoutView({ session }: ActiveWorkoutViewProps) {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const confirm = useConfirm()
  const [actionError, setActionError] = useState<string | null>(null)
  const [savingSetId, setSavingSetId] = useState<string | null>(null)
  const [isAddingExercise, setIsAddingExercise] = useState(false)
  const [expandedExerciseId, setExpandedExerciseId] = useState<string | null>(null)

  function applySession(nextSession: SessionResponse) {
    queryClient.setQueryData(CURRENT_SESSION_QUERY_KEY, nextSession)
  }

  function handleToggleExercise(exerciseId: string) {
    setExpandedExerciseId((current) => (current === exerciseId ? null : exerciseId))
  }

  const completeMutation = useMutation({
    mutationFn: completeCurrentSession,
    onSuccess: () => {
      queryClient.setQueryData(CURRENT_SESSION_QUERY_KEY, null)
      queryClient.invalidateQueries({ queryKey: DASHBOARD_QUERY_KEY })
      navigate('/', { replace: true })
    },
    onError: (error) => {
      if (error instanceof ApiError && error.code === 'NO_COMPLETED_SETS') {
        setActionError('Complete at least one set before finishing this workout.')
        return
      }

      if (error instanceof ApiError) {
        setActionError(error.message)
      } else {
        setActionError('Unable to complete workout. Please try again.')
      }
    },
  })

  const discardMutation = useMutation({
    mutationFn: discardCurrentSession,
    onSuccess: () => {
      queryClient.setQueryData(CURRENT_SESSION_QUERY_KEY, null)
      queryClient.invalidateQueries({ queryKey: DASHBOARD_QUERY_KEY })
    },
    onError: (error) => {
      if (error instanceof ApiError) {
        setActionError(error.message)
      } else {
        setActionError('Unable to discard workout. Please try again.')
      }
    },
  })

  const isBusy =
    savingSetId !== null ||
    isAddingExercise ||
    completeMutation.isPending ||
    discardMutation.isPending

  async function runSessionMutation<T>(operation: () => Promise<T>): Promise<T> {
    try {
      setActionError(null)
      return await operation()
    } catch (error) {
      if (error instanceof ApiError) {
        setActionError(error.message)
      } else {
        setActionError('Workout update failed. Please try again.')
      }
      throw error
    }
  }

  async function handleUpdateSet(
    workoutExerciseId: string,
    setId: string,
    request: UpdateSetRequest,
  ) {
    setSavingSetId(setId)
    try {
      const nextSession = await runSessionMutation(() =>
        updateSessionSet(workoutExerciseId, setId, request),
      )
      applySession(nextSession)
    } finally {
      setSavingSetId(null)
    }
  }

  async function handleAddSet(workoutExerciseId: string) {
    const nextSession = await runSessionMutation(() => addSessionSet(workoutExerciseId))
    applySession(nextSession)
  }

  async function handleDeleteSet(workoutExerciseId: string, setId: string) {
    const nextSession = await runSessionMutation(() =>
      removeSessionSet(workoutExerciseId, setId),
    )
    applySession(nextSession)
  }

  async function handleRemoveExercise(workoutExerciseId: string) {
    const confirmed = await confirm({
      title: 'Remove exercise',
      message: 'Remove this exercise from the active workout?',
      confirmLabel: 'Remove',
      destructive: true,
    })

    if (!confirmed) {
      return
    }

    const nextSession = await runSessionMutation(() => removeSessionExercise(workoutExerciseId))
    applySession(nextSession)

    if (expandedExerciseId === workoutExerciseId) {
      setExpandedExerciseId(null)
    }
  }

  async function handleAddExercise(exerciseId: string, initialSetCount: number) {
    setIsAddingExercise(true)
    try {
      const nextSession = await runSessionMutation(() =>
        addSessionExercise({ exerciseId, initialSetCount }),
      )
      applySession(nextSession)
    } finally {
      setIsAddingExercise(false)
    }
  }

  async function handleComplete() {
    if (completeMutation.isPending) {
      return
    }

    const confirmed = await confirm({
      title: 'Complete workout',
      message: 'Complete this workout?',
      confirmLabel: 'Yes, complete',
    })

    if (!confirmed) {
      return
    }

    setActionError(null)
    completeMutation.mutate()
  }

  async function handleDiscard() {
    if (discardMutation.isPending) {
      return
    }

    const confirmed = await confirm({
      title: 'Discard workout',
      message:
        'Discard this workout? The active workout and all logged sets will be permanently deleted.',
      confirmLabel: 'Yes, discard',
      destructive: true,
    })

    if (!confirmed) {
      return
    }

    setActionError(null)
    discardMutation.mutate()
  }

  const exercises = sortSessionExercises(session)

  return (
    <div className="space-y-6">
      <section className="app-card">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
          <div>
            <h2 className="text-2xl font-semibold">{session.name}</h2>
            <p className="mt-1 text-sm font-medium text-slate-700">IN_PROGRESS</p>
            <div className="mt-2">
              <ElapsedTime startedAt={session.startedAt} />
            </div>
          </div>
          <div className="flex flex-wrap gap-2">
            <button
              type="button"
              onClick={() => void handleComplete()}
              disabled={isBusy}
              className="rounded-md bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-800 disabled:bg-slate-400"
            >
              {completeMutation.isPending ? 'Completing…' : 'Complete workout'}
            </button>
            <button
              type="button"
              onClick={() => void handleDiscard()}
              disabled={isBusy}
              className="rounded-md border border-red-300 bg-white px-4 py-2 text-sm font-medium text-red-700 hover:bg-red-50 disabled:opacity-60"
            >
              {discardMutation.isPending ? 'Discarding…' : 'Discard workout'}
            </button>
          </div>
        </div>

        {actionError ? (
          <p className="mt-4 rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700" role="alert">
            {actionError}
          </p>
        ) : null}
      </section>

      {exercises.length === 0 ? (
        <section className="app-card">
          <p className="text-sm text-slate-600">No exercises in this workout yet. Add one below.</p>
        </section>
      ) : (
        exercises.map((exercise) => (
          <WorkoutExerciseCard
            key={exercise.id}
            exercise={exercise}
            isExpanded={expandedExerciseId === exercise.id}
            onToggle={() => handleToggleExercise(exercise.id)}
            disabled={isBusy}
            savingSetId={savingSetId}
            onAddSet={handleAddSet}
            onUpdateSet={handleUpdateSet}
            onDeleteSet={handleDeleteSet}
            onRemoveExercise={handleRemoveExercise}
          />
        ))
      )}

      <WorkoutAddExercise
        disabled={isBusy}
        isSubmitting={isAddingExercise}
        onAdd={handleAddExercise}
      />
    </div>
  )
}
