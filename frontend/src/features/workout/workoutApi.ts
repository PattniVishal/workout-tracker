import { apiFetch, apiRequest } from '../../api/client'
import { parseApiError } from '../../api/errors'
import type {
  AddSessionExerciseRequest,
  SessionResponse,
  StartSessionRequest,
  UpdateSetRequest,
} from './workoutTypes'

export const CURRENT_SESSION_QUERY_KEY = ['workout', 'current'] as const

export async function fetchCurrentSession(): Promise<SessionResponse | null> {
  const response = await apiFetch('/sessions/current')

  if (response.status === 204) {
    return null
  }

  if (!response.ok) {
    throw await parseApiError(response)
  }

  return (await response.json()) as SessionResponse
}

export function startSession(request: StartSessionRequest): Promise<SessionResponse> {
  return apiRequest<SessionResponse>('/sessions', {
    method: 'POST',
    body: request,
  })
}

export function addSessionExercise(request: AddSessionExerciseRequest): Promise<SessionResponse> {
  return apiRequest<SessionResponse>('/sessions/current/exercises', {
    method: 'POST',
    body: request,
  })
}

export function removeSessionExercise(workoutExerciseId: string): Promise<SessionResponse> {
  return apiRequest<SessionResponse>(`/sessions/current/exercises/${workoutExerciseId}`, {
    method: 'DELETE',
  })
}

export function addSessionSet(workoutExerciseId: string): Promise<SessionResponse> {
  return apiRequest<SessionResponse>(`/sessions/current/exercises/${workoutExerciseId}/sets`, {
    method: 'POST',
  })
}

export function updateSessionSet(
  workoutExerciseId: string,
  setId: string,
  request: UpdateSetRequest,
): Promise<SessionResponse> {
  return apiRequest<SessionResponse>(
    `/sessions/current/exercises/${workoutExerciseId}/sets/${setId}`,
    {
      method: 'PATCH',
      body: request,
    },
  )
}

export function removeSessionSet(workoutExerciseId: string, setId: string): Promise<SessionResponse> {
  return apiRequest<SessionResponse>(
    `/sessions/current/exercises/${workoutExerciseId}/sets/${setId}`,
    {
      method: 'DELETE',
    },
  )
}

export function completeCurrentSession(): Promise<SessionResponse> {
  return apiRequest<SessionResponse>('/sessions/current/complete', {
    method: 'POST',
  })
}

export function discardCurrentSession(): Promise<void> {
  return apiRequest<void>('/sessions/current', {
    method: 'DELETE',
  })
}

export function sortSessionExercises(session: SessionResponse) {
  return [...session.exercises].sort((left, right) => left.position - right.position)
}

export function sortSessionSets<T extends { setNumber: number }>(sets: T[]) {
  return [...sets].sort((left, right) => left.setNumber - right.setNumber)
}
