import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { apiFetch, apiRequest } from './client'
import { clearCsrfToken } from './csrf'

describe('api client', () => {
  beforeEach(() => {
    clearCsrfToken()
    vi.stubEnv('VITE_API_BASE_URL', 'http://localhost:8080/api')
  })

  afterEach(() => {
    vi.unstubAllGlobals()
    vi.restoreAllMocks()
    clearCsrfToken()
  })

  it('sends credentials include on requests', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ ok: true }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }),
    )
    vi.stubGlobal('fetch', fetchMock)

    await apiFetch('/auth/me')

    expect(fetchMock).toHaveBeenCalledWith(
      'http://localhost:8080/api/auth/me',
      expect.objectContaining({
        credentials: 'include',
      }),
    )
  })

  it('handles 204 responses as empty results', async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(
        new Response(
          JSON.stringify({ headerName: 'X-XSRF-TOKEN', token: 'csrf-token-value' }),
          {
            status: 200,
            headers: { 'Content-Type': 'application/json' },
          },
        ),
      )
      .mockResolvedValueOnce(new Response(null, { status: 204 }))

    vi.stubGlobal('fetch', fetchMock)

    const result = await apiRequest('/auth/logout', { method: 'POST' })

    expect(result).toBeUndefined()
  })

  it('bootstraps CSRF and sends the header on unsafe requests', async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(
        new Response(
          JSON.stringify({ headerName: 'X-XSRF-TOKEN', token: 'csrf-token-value' }),
          {
            status: 200,
            headers: { 'Content-Type': 'application/json' },
          },
        ),
      )
      .mockResolvedValueOnce(new Response(null, { status: 204 }))

    vi.stubGlobal('fetch', fetchMock)

    await apiRequest('/auth/logout', { method: 'POST' })

    expect(fetchMock).toHaveBeenNthCalledWith(
      1,
      'http://localhost:8080/api/auth/csrf',
      expect.objectContaining({
        method: 'GET',
        credentials: 'include',
      }),
    )

    expect(fetchMock).toHaveBeenNthCalledWith(
      2,
      'http://localhost:8080/api/auth/logout',
      expect.objectContaining({
        method: 'POST',
        credentials: 'include',
        headers: expect.any(Headers),
      }),
    )

    const unsafeHeaders = fetchMock.mock.calls[1][1].headers as Headers
    expect(unsafeHeaders.get('X-XSRF-TOKEN')).toBe('csrf-token-value')
  })
})
