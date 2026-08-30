import { describe, expect, it } from 'vitest'
import { ApiError, parseApiError } from './errors'

describe('parseApiError', () => {
  it('parses the backend error shape', async () => {
    const response = new Response(
      JSON.stringify({
        status: 400,
        code: 'VALIDATION_ERROR',
        message: 'Request is invalid.',
        fieldErrors: [{ field: 'email', message: 'must not be blank' }],
      }),
      {
        status: 400,
        headers: { 'Content-Type': 'application/json' },
      },
    )

    const error = await parseApiError(response)

    expect(error).toBeInstanceOf(ApiError)
    expect(error.status).toBe(400)
    expect(error.code).toBe('VALIDATION_ERROR')
    expect(error.message).toBe('Request is invalid.')
    expect(error.fieldErrors).toEqual([{ field: 'email', message: 'must not be blank' }])
  })

  it('falls back when the response body is not JSON', async () => {
    const response = new Response('not json', {
      status: 500,
      statusText: 'Internal Server Error',
    })

    const error = await parseApiError(response)

    expect(error.status).toBe(500)
    expect(error.code).toBe('UNKNOWN')
    expect(error.message).toBe('Internal Server Error')
    expect(error.fieldErrors).toEqual([])
  })
})
