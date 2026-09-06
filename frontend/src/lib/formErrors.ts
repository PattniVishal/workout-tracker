import type { FieldError } from '../api/types'

export function fieldErrorsToMap(fieldErrors: FieldError[]): Record<string, string> {
  const map: Record<string, string> = {}

  for (const fieldError of fieldErrors) {
    map[fieldError.field] = fieldError.message
  }

  return map
}
