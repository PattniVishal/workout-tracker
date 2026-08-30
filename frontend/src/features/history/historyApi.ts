import { apiRequest } from '../../api/client'
import type { SessionResponse } from '../workout/workoutTypes'
import type { HistoryListResponse } from './historyTypes'

export const HISTORY_QUERY_KEY = ['history'] as const

export function historyDetailQueryKey(sessionId: string) {
  return ['history', sessionId] as const
}

export function fetchHistory(): Promise<HistoryListResponse> {
  return apiRequest<HistoryListResponse>('/history')
}

export function fetchHistorySession(sessionId: string): Promise<SessionResponse> {
  return apiRequest<SessionResponse>(`/history/${sessionId}`)
}

export function deleteHistorySession(sessionId: string): Promise<void> {
  return apiRequest<void>(`/history/${sessionId}`, {
    method: 'DELETE',
  })
}
