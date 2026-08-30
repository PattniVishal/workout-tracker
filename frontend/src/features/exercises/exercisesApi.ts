import type { QueryClient } from '@tanstack/react-query'
import { apiRequest } from '../../api/client'
import type {
  Exercise,
  ExerciseListResponse,
  ExerciseQueryParams,
  SaveExerciseRequest,
} from './exerciseTypes'

export const EXERCISES_QUERY_KEY_PREFIX = ['exercises'] as const

export function exercisesQueryKey(params: ExerciseQueryParams = {}) {
  return ['exercises', params] as const
}

export function fetchExercises(params: ExerciseQueryParams = {}): Promise<ExerciseListResponse> {
  const searchParams = new URLSearchParams()

  const trimmedQuery = params.q?.trim()
  if (trimmedQuery) {
    searchParams.set('q', trimmedQuery)
  }

  if (params.muscleGroup) {
    searchParams.set('muscleGroup', params.muscleGroup)
  }

  const query = searchParams.toString()
  const path = query ? `/exercises?${query}` : '/exercises'

  return apiRequest<ExerciseListResponse>(path)
}

export function createExercise(request: SaveExerciseRequest): Promise<Exercise> {
  return apiRequest<Exercise>('/exercises', {
    method: 'POST',
    body: request,
  })
}

export function updateExercise(exerciseId: string, request: SaveExerciseRequest): Promise<Exercise> {
  return apiRequest<Exercise>(`/exercises/${exerciseId}`, {
    method: 'PUT',
    body: request,
  })
}

export function archiveExercise(exerciseId: string): Promise<void> {
  return apiRequest<void>(`/exercises/${exerciseId}/archive`, {
    method: 'POST',
  })
}

export async function invalidateExerciseQueries(queryClient: QueryClient): Promise<void> {
  await queryClient.invalidateQueries({ queryKey: EXERCISES_QUERY_KEY_PREFIX })
}
