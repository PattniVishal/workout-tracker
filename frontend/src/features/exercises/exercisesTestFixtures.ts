import type { Exercise } from './exerciseTypes'

export const benchPressExercise: Exercise = {
  id: 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
  name: 'Bench Press',
  primaryMuscleGroup: 'Chest',
  secondaryMuscleGroups: ['Triceps', 'Shoulders'],
  category: 'Barbell',
  source: 'SYSTEM',
}

export const squatExercise: Exercise = {
  id: 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
  name: 'Squat',
  primaryMuscleGroup: 'Legs',
  secondaryMuscleGroups: [],
  category: 'Barbell',
  source: 'SYSTEM',
}

export const customExerciseFixture: Exercise = {
  id: 'dddddddd-dddd-dddd-dddd-dddddddddddd',
  name: 'Cable Fly',
  primaryMuscleGroup: 'Chest',
  secondaryMuscleGroups: ['Shoulders'],
  category: 'Cable',
  source: 'CUSTOM',
}

export const exerciseListFixture = {
  exercises: [benchPressExercise, customExerciseFixture, squatExercise],
}
