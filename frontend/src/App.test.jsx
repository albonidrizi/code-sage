import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import App from './App'
import { reviewsAPI } from './services/api'

vi.mock('./services/api', () => ({
  reviewsAPI: {
    getStats: vi.fn(),
    getRecent: vi.fn(),
  },
}))

const review = {
  id: 1,
  prNumber: 42,
  prTitle: 'Fix authentication bug',
  repositoryOwner: 'codesage',
  repositoryName: 'backend',
  status: 'COMPLETED',
  qualityScore: 8.5,
  issues: [{ severity: 'HIGH', title: 'Session token persists', filePath: 'AuthService.java', lineNumber: 84 }],
  createdAt: new Date().toISOString(),
}

describe('App', () => {
  beforeEach(() => {
    reviewsAPI.getStats.mockResolvedValue({
      totalReviews: 1,
      activePRs: 0,
      avgQualityScore: 8.5,
      issuesFound: 1,
      highIssues: 1,
    })
    reviewsAPI.getRecent.mockResolvedValue([review])
  })

  it('loads dashboard data, filters reviews, and opens review details', async () => {
    render(<App />)

    expect(await screen.findByText(/Fix authentication bug/)).toBeInTheDocument()
    fireEvent.change(screen.getByPlaceholderText('Search queue'), { target: { value: 'missing' } })
    expect(screen.getByText('No reviews match your search.')).toBeInTheDocument()
    fireEvent.change(screen.getByPlaceholderText('Search queue'), { target: { value: 'authentication' } })
    fireEvent.click(screen.getByText(/Fix authentication bug/))

    expect(screen.getByText('AI findings')).toBeInTheDocument()
    expect(screen.getByText('Session token persists')).toBeInTheDocument()
    fireEvent.click(screen.getByLabelText('Close details'))
    fireEvent.click(screen.getByLabelText('Toggle menu'))
    fireEvent.click(screen.getByLabelText('Refresh dashboard'))
    await waitFor(() => expect(reviewsAPI.getStats).toHaveBeenCalledTimes(2))
  })

  it('uses deterministic demo data when the API is unavailable', async () => {
    reviewsAPI.getStats.mockRejectedValue(new Error('offline'))
    reviewsAPI.getRecent.mockRejectedValue(new Error('offline'))

    render(<App />)

    expect(await screen.findByText('Demo data')).toBeInTheDocument()
    await waitFor(() => expect(screen.getByText('247')).toBeInTheDocument())
  })
})
