export type SessionSet = {
  id: string
  setNumber: number
  weightKg: number | null
  repetitions: number | null
  completed: boolean
}

export type SessionExercise = {
  id: string
  exerciseId: string
  exerciseName: string
  position: number
  sets: SessionSet[]
}

export type SessionResponse = {
  id: string
  name: string
  status: string
  originRoutineId: string | null
  startedAt: string
  completedAt: string | null
  durationSeconds: number | null
  exercises: SessionExercise[]
}

export type StartSessionRequest =
  | { routineId: string; name?: undefined }
  | { name: string; routineId?: undefined }

export type AddSessionExerciseRequest = {
  exerciseId: string
  initialSetCount: number
}

export type UpdateSetRequest = {
  weightKg?: number
  repetitions?: number
  completed?: boolean
}

export type PreviousPerformanceSet = {
  setNumber: number
  weightKg: number | null
  repetitions: number | null
}

export type PreviousPerformance = {
  exerciseId: string
  exerciseName: string
  completedAt: string
  sessionId: string
  sets: PreviousPerformanceSet[]
}
