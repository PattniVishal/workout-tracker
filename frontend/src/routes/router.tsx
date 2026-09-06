import { createBrowserRouter, Navigate } from 'react-router-dom'
import { GuestRoute } from '../components/GuestRoute'
import { ProtectedRoute } from '../components/ProtectedRoute'
import { LoginPage } from '../features/auth/LoginPage'
import { RegisterPage } from '../features/auth/RegisterPage'
import { DashboardPage } from '../features/dashboard/DashboardPage'
import { RoutineDetailPage } from '../features/routines/RoutineDetailPage'
import { RoutineFormPage } from '../features/routines/RoutineFormPage'
import { RoutineListPage } from '../features/routines/RoutineListPage'
import { ExerciseFormPage } from '../features/exercises/ExerciseFormPage'
import { ExerciseListPage } from '../features/exercises/ExerciseListPage'
import { WorkoutHistoryDetailPage } from '../features/history/WorkoutHistoryDetailPage'
import { WorkoutHistoryListPage } from '../features/history/WorkoutHistoryListPage'
import { WorkoutPage } from '../features/workout/WorkoutPage'
import { AppLayout } from './AppLayout'
import { PublicLayout } from './PublicLayout'

export const router = createBrowserRouter([
  {
    element: <PublicLayout />,
    children: [
      {
        element: <GuestRoute />,
        children: [
          {
            path: '/login',
            element: <LoginPage />,
          },
          {
            path: '/register',
            element: <RegisterPage />,
          },
        ],
      },
    ],
  },
  {
    element: <ProtectedRoute />,
    children: [
      {
        element: <AppLayout />,
        children: [
          {
            path: '/',
            element: <DashboardPage />,
          },
          {
            path: '/routines',
            element: <RoutineListPage />,
          },
          {
            path: '/routines/new',
            element: <RoutineFormPage mode="create" />,
          },
          {
            path: '/routines/:routineId/edit',
            element: <RoutineFormPage mode="edit" />,
          },
          {
            path: '/routines/:routineId',
            element: <RoutineDetailPage />,
          },
          {
            path: '/exercises/new',
            element: <ExerciseFormPage mode="create" />,
          },
          {
            path: '/exercises/:exerciseId/edit',
            element: <ExerciseFormPage mode="edit" />,
          },
          {
            path: '/exercises',
            element: <ExerciseListPage />,
          },
          {
            path: '/workout',
            element: <WorkoutPage />,
          },
          {
            path: '/history',
            element: <WorkoutHistoryListPage />,
          },
          {
            path: '/history/:sessionId',
            element: <WorkoutHistoryDetailPage />,
          },
        ],
      },
    ],
  },
  {
    path: '*',
    element: <Navigate to="/" replace />,
  },
])

