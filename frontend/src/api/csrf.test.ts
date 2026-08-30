import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { clearCsrfToken, ensureCsrfToken, getCsrfToken, isUnsafeMethod } from './csrf'

describe('csrf', () => {
  beforeEach(() => {
    clearCsrfToken()
    vi.stubEnv('VITE_API_BASE_URL', 'http://localhost:8080/api')
  })

  afterEach(() => {
    vi.unstubAllGlobals()
    vi.restoreAllMocks()
    clearCsrfToken()
  })

  it('identifies unsafe HTTP methods', () => {
    expect(isUnsafeMethod('GET')).toBe(false)
    expect(isUnsafeMethod('post')).toBe(true)
    expect(isUnsafeMethod('DELETE')).toBe(true)
  })

  it('bootstraps the CSRF token once and reuses it', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(
        JSON.stringify({ headerName: 'X-XSRF-TOKEN', token: 'token-123' }),
        {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        },
      ),
    )
    vi.stubGlobal('fetch', fetchMock)

    await ensureCsrfToken()
    await ensureCsrfToken()

    expect(fetchMock).toHaveBeenCalledTimes(1)
    expect(getCsrfToken()).toBe('token-123')
  })
})
