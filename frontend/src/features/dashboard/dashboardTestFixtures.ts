import type { DashboardResponse } from './dashboardTypes'

export const dashboardFixture: DashboardResponse = {
  displayName: 'Vishal',
  hasActiveSession: false,
  activeSessionId: null,
  routines: [
    {
      id: 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
      name: 'Push Day',
      exerciseCount: 5,
    },
  ],
  mostRecentCompleted: {
    id: 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
    name: 'Push Day',
    completedAt: '2026-08-25T10:30:00.000Z',
  },
  totalCompletedWorkouts: 12,
  totalCompletedSets: 140,
}

export const emptyDashboardFixture: DashboardResponse = {
  displayName: 'Vishal',
  hasActiveSession: false,
  activeSessionId: null,
  routines: [],
  mostRecentCompleted: null,
  totalCompletedWorkouts: 0,
  totalCompletedSets: 0,
}

export const activeSessionDashboardFixture: DashboardResponse = {
  ...dashboardFixture,
  hasActiveSession: true,
  activeSessionId: 'cccccccc-cccc-cccc-cccc-cccccccccccc',
}
