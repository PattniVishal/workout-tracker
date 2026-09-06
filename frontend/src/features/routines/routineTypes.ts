export type RoutineSummary = {
  id: string
  name: string
  description: string | null
  exerciseCount: number
  updatedAt: string
}

export type RoutineListResponse = {
  routines: RoutineSummary[]
}

export type RoutineExercise = {
  id: string
  exerciseId: string
  exerciseName: string
  plannedSetCount: number
  position: number
}

export type RoutineDetail = {
  id: string
  name: string
  description: string | null
  exercises: RoutineExercise[]
}

export type SaveRoutineExerciseRequest = {
  exerciseId: string
  plannedSetCount: number
}

export type SaveRoutineRequest = {
  name: string
  description?: string | null
  exercises: SaveRoutineExerciseRequest[]
}

export type RoutineFormExercise = {
  exerciseId: string
  exerciseName: string
  plannedSetCount: number
}

export type {
  Exercise,
  ExerciseListResponse,
  ExerciseQueryParams,
  SaveExerciseRequest,
} from '../exercises/exerciseTypes'
