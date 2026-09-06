import { apiFetch } from '../../api/client'
import { parseApiError } from '../../api/errors'
import type { PreviousPerformance } from './workoutTypes'

export function previousPerformanceQueryKey(exerciseId: string) {
  return ['workout', 'previous-performance', exerciseId] as const
}

export async function fetchPreviousPerformance(
  exerciseId: string,
): Promise<PreviousPerformance | null> {
  const response = await apiFetch(`/exercises/${exerciseId}/previous-performance`)

  if (response.status === 204) {
    return null
  }

  if (!response.ok) {
    throw await parseApiError(response)
  }

  return (await response.json()) as PreviousPerformance
}
