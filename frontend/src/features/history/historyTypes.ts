export type HistoryWorkoutSummary = {
  id: string
  name: string
  completedAt: string
  durationSeconds: number
  exerciseCount: number
  completedSetCount: number
}

export type HistoryListResponse = {
  workouts: HistoryWorkoutSummary[]
}
