import { beforeEach, describe, expect, it, vi } from 'vitest'

const client = {
  get: vi.fn(),
  interceptors: {
    request: { use: vi.fn() },
    response: { use: vi.fn() },
  },
}

vi.mock('axios', () => ({
  default: {
    create: vi.fn(() => client),
  },
}))

describe('reviewsAPI', () => {
  beforeEach(() => {
    client.get.mockReset()
  })

  it('calls dashboard, recent, detail, repository, list, and health endpoints', async () => {
    client.get.mockResolvedValue({ data: { ok: true } })
    const { reviewsAPI } = await import('./api')

    await expect(reviewsAPI.getStats()).resolves.toEqual({ ok: true })
    await expect(reviewsAPI.getRecent()).resolves.toEqual({ ok: true })
    await expect(reviewsAPI.getById(42)).resolves.toEqual({ ok: true })
    await expect(reviewsAPI.getAll(2, 25)).resolves.toEqual({ ok: true })
    await expect(reviewsAPI.getByRepository('codesage', 'backend', 1, 5)).resolves.toEqual({ ok: true })
    await expect(reviewsAPI.healthCheck()).resolves.toEqual({ ok: true })

    expect(client.get).toHaveBeenCalledWith('/reviews/stats')
    expect(client.get).toHaveBeenCalledWith('/reviews/42')
    expect(client.get).toHaveBeenCalledWith('/reviews', { params: { page: 2, size: 25 } })
  })

  it('normalizes request and response interceptor failures', async () => {
    await import('./api')
    const [requestSuccess, requestFailure] = client.interceptors.request.use.mock.calls[0]
    const [responseSuccess, responseFailure] = client.interceptors.response.use.mock.calls[0]

    expect(requestSuccess({ method: 'get', url: '/reviews' })).toEqual({ method: 'get', url: '/reviews' })
    await expect(requestFailure(new Error('request failed'))).rejects.toThrow('request failed')
    expect(responseSuccess({ data: 42 })).toEqual({ data: 42 })
    expect(() => responseFailure({ response: { status: 404 } })).toThrow('Resource not found')
    expect(() => responseFailure({ response: { status: 500 } })).toThrow('Server error')
    expect(() => responseFailure({ response: { status: 400, data: { message: 'Bad request' } } }))
      .toThrow('Bad request')
    expect(() => responseFailure({ request: {} })).toThrow('Unable to connect')
  })
})
