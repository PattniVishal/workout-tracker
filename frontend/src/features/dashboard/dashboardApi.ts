import { apiRequest } from '../../api/client'
import type { DashboardResponse } from './dashboardTypes'

export const DASHBOARD_QUERY_KEY = ['dashboard'] as const

export function fetchDashboard(): Promise<DashboardResponse> {
  return apiRequest<DashboardResponse>('/dashboard')
}
