import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ApiError } from '../../api/errors'
import { DASHBOARD_QUERY_KEY } from '../dashboard/dashboardApi'
import { ROUTINES_QUERY_KEY, fetchRoutines } from '../routines/routinesApi'
import { LoadingState } from '../../components/LoadingState'
import {
  CURRENT_SESSION_QUERY_KEY,
  fetchCurrentSession,
  startSession,
} from './workoutApi'
import type { StartSessionRequest } from './workoutTypes'

export function WorkoutStartPanel() {
  const queryClient = useQueryClient()
  const [selectedRoutineId, setSelectedRoutineId] = useState('')
  const [adhocName, setAdhocName] = useState('')
  const [formError, setFormError] = useState<string | null>(null)

  const routinesQuery = useQuery({
    queryKey: ROUTINES_QUERY_KEY,
    queryFn: fetchRoutines,
  })

  const startMutation = useMutation({
    mutationFn: (request: StartSessionRequest) => startSession(request),
    onSuccess: (session) => {
      queryClient.setQueryData(CURRENT_SESSION_QUERY_KEY, session)
      queryClient.invalidateQueries({ queryKey: DASHBOARD_QUERY_KEY })
    },
    onError: async (error) => {
      if (error instanceof ApiError && error.code === 'ACTIVE_SESSION_EXISTS') {
        const existingSession = await fetchCurrentSession()
        if (existingSession) {
          queryClient.setQueryData(CURRENT_SESSION_QUERY_KEY, existingSession)
          return
        }
      }

      if (error instanceof ApiError) {
        setFormError(error.message)
      } else {
        setFormError('Unable to start workout. Please try again.')
      }
    },
  })

  async function handleStartFromRoutine() {
    if (!selectedRoutineId || startMutation.isPending) {
      return
    }

    setFormError(null)
    try {
      await startMutation.mutateAsync({ routineId: selectedRoutineId })
    } catch {
      // Errors are handled in the mutation onError callback.
    }
  }

  async function handleStartAdHoc() {
    const trimmedName = adhocName.trim()

    if (!trimmedName) {
      setFormError('Workout name is required for an ad-hoc workout.')
      return
    }

    if (startMutation.isPending) {
      return
    }

    setFormError(null)
    try {
      await startMutation.mutateAsync({ name: trimmedName })
    } catch {
      // Errors are handled in the mutation onError callback.
    }
  }

  if (routinesQuery.isPending) {
    return <LoadingState message="Loading routines…" />
  }

  if (routinesQuery.isError) {
    const message =
      routinesQuery.error instanceof ApiError
        ? routinesQuery.error.message
        : 'Unable to load routines.'

    return (
      <section className="rounded-lg border border-red-200 bg-red-50 p-6 shadow-sm" role="alert">
        <p className="text-sm text-red-700">{message}</p>
        <button
          type="button"
          onClick={() => routinesQuery.refetch()}
          className="mt-4 rounded-md border border-red-300 bg-white px-4 py-2 text-sm font-medium text-red-800"
        >
          Try again
        </button>
      </section>
    )
  }

  const routines = routinesQuery.data?.routines ?? []

  return (
    <div className="space-y-6">
      <section className="rounded-lg border border-slate-200 bg-white p-6 shadow-sm">
        <h2 className="text-xl font-semibold">Start workout</h2>
        <p className="mt-2 text-sm text-slate-600">No active workout session. Choose how to begin.</p>

        {formError ? (
          <p className="mt-4 rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700" role="alert">
            {formError}
          </p>
        ) : null}

        <div className="mt-6 space-y-6">
          <div>
            <h3 className="text-sm font-semibold text-slate-900">From routine</h3>
            {routines.length === 0 ? (
              <p className="mt-2 text-sm text-slate-600">You do not have any routines yet.</p>
            ) : (
              <div className="mt-3 flex flex-col gap-3 sm:flex-row">
                <select
                  value={selectedRoutineId}
                  onChange={(event) => setSelectedRoutineId(event.target.value)}
                  disabled={startMutation.isPending}
                  aria-label="Select routine"
                  className="flex-1 rounded-md border border-slate-300 px-3 py-2 text-sm"
                >
                  <option value="">Select a routine</option>
                  {routines.map((routine) => (
                    <option key={routine.id} value={routine.id}>
                      {routine.name}
                    </option>
                  ))}
                </select>
                <button
                  type="button"
                  onClick={() => void handleStartFromRoutine()}
                  disabled={!selectedRoutineId || startMutation.isPending}
                  className="rounded-md bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-800 disabled:bg-slate-400"
                >
                  {startMutation.isPending ? 'Starting…' : 'Start from routine'}
                </button>
              </div>
            )}
          </div>

          <div>
            <h3 className="text-sm font-semibold text-slate-900">Ad-hoc workout</h3>
            <div className="mt-3 flex flex-col gap-3 sm:flex-row">
              <input
                type="text"
                value={adhocName}
                onChange={(event) => setAdhocName(event.target.value)}
                placeholder="Workout name"
                disabled={startMutation.isPending}
                className="flex-1 rounded-md border border-slate-300 px-3 py-2 text-sm"
              />
              <button
                type="button"
                onClick={() => void handleStartAdHoc()}
                disabled={startMutation.isPending}
                className="rounded-md border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50 disabled:opacity-60"
              >
                {startMutation.isPending ? 'Starting…' : 'Start ad-hoc'}
              </button>
            </div>
          </div>
        </div>
      </section>
    </div>
  )
}
