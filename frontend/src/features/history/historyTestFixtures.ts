import type { SessionResponse } from '../workout/workoutTypes'
import { completedSessionFixture } from '../workout/workoutTestFixtures'
import type { HistoryListResponse } from './historyTypes'

export const historyListFixture: HistoryListResponse = {
  workouts: [
    {
      id: 'session-2222-2222-2222-222222222222',
      name: 'Push Day',
      completedAt: '2026-08-25T11:00:00.000Z',
      durationSeconds: 3600,
      exerciseCount: 5,
      completedSetCount: 14,
    },
    {
      id: 'session-1111-1111-1111-111111111111',
      name: 'Pull Day',
      completedAt: '2026-08-24T11:00:00.000Z',
      durationSeconds: 300,
      exerciseCount: 4,
      completedSetCount: 12,
    },
  ],
}

export const emptyHistoryFixture: HistoryListResponse = {
  workouts: [],
}

export const historyDetailFixture: SessionResponse = {
  ...completedSessionFixture,
  exercises: [
    {
      id: 'workout-exercise-2222-2222-222222222222',
      exerciseId: 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
      exerciseName: 'Squat',
      position: 2,
      sets: [
        {
          id: 'set-3333-3333-3333-333333333333',
          setNumber: 1,
          weightKg: 100,
          repetitions: 5,
          completed: true,
        },
        {
          id: 'set-4444-4444-4444-444444444444',
          setNumber: 2,
          weightKg: null,
          repetitions: null,
          completed: false,
        },
      ],
    },
    {
      id: 'workout-exercise-1111-1111-111111111111',
      exerciseId: 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
      exerciseName: 'Bench Press',
      position: 1,
      sets: [
        {
          id: 'set-1111-1111-1111-111111111111',
          setNumber: 1,
          weightKg: 60,
          repetitions: 8,
          completed: true,
        },
      ],
    },
  ],
}
