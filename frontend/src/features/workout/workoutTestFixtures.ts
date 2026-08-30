import type { SessionResponse } from './workoutTypes'

export const activeSessionFixture: SessionResponse = {
  id: 'session-1111-1111-1111-111111111111',
  name: 'Push Day',
  status: 'IN_PROGRESS',
  originRoutineId: '11111111-1111-1111-1111-111111111111',
  startedAt: '2026-08-25T10:00:00.000Z',
  completedAt: null,
  durationSeconds: null,
  exercises: [
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
        {
          id: 'set-2222-2222-2222-222222222222',
          setNumber: 2,
          weightKg: null,
          repetitions: null,
          completed: false,
        },
      ],
    },
  ],
}

export const completedSessionFixture: SessionResponse = {
  ...activeSessionFixture,
  status: 'COMPLETED',
  completedAt: '2026-08-25T11:00:00.000Z',
  durationSeconds: 3600,
}
