import type { Exercise, RoutineDetail, RoutineListResponse } from './routineTypes'

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

export const archivedRoutineExercise = {
  id: 'slot-archived-1111-1111-111111111111',
  exerciseId: 'cccccccc-cccc-cccc-cccc-cccccccccccc',
  exerciseName: 'Archived Custom Press',
  plannedSetCount: 3,
  position: 1,
}

export const routineListFixture: RoutineListResponse = {
  routines: [
    {
      id: '11111111-1111-1111-1111-111111111111',
      name: 'Push Day',
      description: 'Chest and triceps',
      exerciseCount: 2,
      updatedAt: '2026-08-25T10:00:00.000Z',
    },
  ],
}

export const routineDetailFixture: RoutineDetail = {
  id: '11111111-1111-1111-1111-111111111111',
  name: 'Push Day',
  description: 'Chest and triceps',
  exercises: [
    {
      id: 'slot-1111-1111-1111-111111111111',
      exerciseId: benchPressExercise.id,
      exerciseName: benchPressExercise.name,
      plannedSetCount: 3,
      position: 1,
    },
    {
      id: 'slot-2222-2222-2222-222222222222',
      exerciseId: squatExercise.id,
      exerciseName: squatExercise.name,
      plannedSetCount: 4,
      position: 2,
    },
  ],
}

export const routineWithArchivedExerciseFixture: RoutineDetail = {
  id: '22222222-2222-2222-2222-222222222222',
  name: 'Legacy Routine',
  description: null,
  exercises: [archivedRoutineExercise],
}
