import { apiRequest } from '../../api/client'
import type {
  RoutineDetail,
  RoutineListResponse,
  SaveRoutineRequest,
} from './routineTypes'

export const ROUTINES_QUERY_KEY = ['routines'] as const

export function routineDetailQueryKey(routineId: string) {
  return ['routines', routineId] as const
}

export function fetchRoutines(): Promise<RoutineListResponse> {
  return apiRequest<RoutineListResponse>('/routines')
}

export function fetchRoutine(routineId: string): Promise<RoutineDetail> {
  return apiRequest<RoutineDetail>(`/routines/${routineId}`)
}

export function createRoutine(request: SaveRoutineRequest): Promise<RoutineDetail> {
  return apiRequest<RoutineDetail>('/routines', {
    method: 'POST',
    body: request,
  })
}

export function updateRoutine(
  routineId: string,
  request: SaveRoutineRequest,
): Promise<RoutineDetail> {
  return apiRequest<RoutineDetail>(`/routines/${routineId}`, {
    method: 'PUT',
    body: request,
  })
}

export function deleteRoutine(routineId: string): Promise<void> {
  return apiRequest<void>(`/routines/${routineId}`, {
    method: 'DELETE',
  })
}
