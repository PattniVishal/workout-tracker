export type DashboardRoutineSummary = {
  id: string
  name: string
  exerciseCount: number
}

export type DashboardMostRecentCompleted = {
  id: string
  name: string
  completedAt: string
}

export type DashboardResponse = {
  displayName: string
  hasActiveSession: boolean
  activeSessionId: string | null
  routines: DashboardRoutineSummary[]
  mostRecentCompleted: DashboardMostRecentCompleted | null
  totalCompletedWorkouts: number
  totalCompletedSets: number
}
