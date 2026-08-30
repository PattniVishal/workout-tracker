import { useMemo } from 'react'
import { useQuery } from '@tanstack/react-query'
import { exercisesQueryKey, fetchExercises } from './exercisesApi'

export function useMuscleGroupOptions() {
  const { data } = useQuery({
    queryKey: exercisesQueryKey({}),
    queryFn: () => fetchExercises({}),
  })

  return useMemo(() => {
    if (!data?.exercises) {
      return []
    }

    return [...new Set(data.exercises.map((exercise) => exercise.primaryMuscleGroup))].sort()
  }, [data?.exercises])
}
