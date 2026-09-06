import type { ApiErrorBody } from './types'

export class ApiError extends Error {
  readonly status: number
  readonly code: string
  readonly fieldErrors: ApiErrorBody['fieldErrors']

  constructor(message: string, body: ApiErrorBody) {
    super(message)
    this.name = 'ApiError'
    this.status = body.status
    this.code = body.code
    this.fieldErrors = body.fieldErrors ?? []
  }
}

export async function parseApiError(response: Response): Promise<ApiError> {
  try {
    const body = (await response.json()) as ApiErrorBody
    return new ApiError(body.message ?? 'Request failed.', {
      status: body.status ?? response.status,
      code: body.code ?? 'UNKNOWN',
      message: body.message ?? 'Request failed.',
      fieldErrors: body.fieldErrors ?? [],
    })
  } catch {
    return new ApiError(response.statusText || 'Request failed.', {
      status: response.status,
      code: 'UNKNOWN',
      message: response.statusText || 'Request failed.',
      fieldErrors: [],
    })
  }
}
