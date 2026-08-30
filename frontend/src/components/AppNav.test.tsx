import { cleanup, render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { AppNav } from './AppNav'

describe('AppNav', () => {
  afterEach(() => {
    cleanup()
  })

  it('renders all navigation items', () => {
    render(
      <MemoryRouter initialEntries={['/']}>
        <AppNav />
      </MemoryRouter>,
    )

    expect(screen.getByRole('link', { name: 'Dashboard' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Routines' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Exercises' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Workout' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'History' })).toBeInTheDocument()
  })

  it('marks the active route', () => {
    render(
      <MemoryRouter initialEntries={['/routines']}>
        <AppNav />
      </MemoryRouter>,
    )

    expect(screen.getByRole('link', { name: 'Routines' })).toHaveClass('border-slate-900')
    expect(screen.getByRole('link', { name: 'Dashboard' })).toHaveClass('border-transparent')
  })
})
