export type Exercise = {
  id: string
  name: string
  primaryMuscleGroup: string
  secondaryMuscleGroups: string[]
  category: string
  source: string
}

export type ExerciseListResponse = {
  exercises: Exercise[]
}

export type ExerciseQueryParams = {
  q?: string
  muscleGroup?: string
}

export type SaveExerciseRequest = {
  name: string
  primaryMuscleGroup: string
  secondaryMuscleGroups: string[]
  category: string
}

export function isCustomExercise(exercise: Exercise): boolean {
  return exercise.source === 'CUSTOM'
}

export function getExerciseSourceLabel(source: string): string {
  return source === 'CUSTOM' ? 'My exercise' : 'System'
}
