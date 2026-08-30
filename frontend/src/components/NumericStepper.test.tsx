import { cleanup, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { NumericStepper } from './NumericStepper'

describe('NumericStepper', () => {
  afterEach(() => {
    cleanup()
    vi.restoreAllMocks()
  })

  it('increases and decreases the value with buttons', async () => {
    const user = userEvent.setup()
    const onChange = vi.fn()

    render(
      <NumericStepper
        id="reps"
        label="Reps"
        value="5"
        onChange={onChange}
        min={0}
      />,
    )

    await user.click(screen.getByRole('button', { name: 'Increase reps' }))
    expect(onChange).toHaveBeenLastCalledWith('6')

    await user.click(screen.getByRole('button', { name: 'Decrease reps' }))
    expect(onChange).toHaveBeenLastCalledWith('4')
  })

  it('enforces the minimum value', async () => {
    const user = userEvent.setup()
    const onChange = vi.fn()

    render(
      <NumericStepper
        id="count"
        label="Initial set count"
        value="1"
        onChange={onChange}
        min={1}
      />,
    )

    await user.click(screen.getByRole('button', { name: 'Decrease initial set count' }))
    expect(onChange).toHaveBeenLastCalledWith('1')
  })

  it('supports manual entry', async () => {
    const user = userEvent.setup()
    let value = '3'
    const onChange = vi.fn((next: string) => {
      value = next
    })

    const { rerender } = render(
      <NumericStepper
        id="count"
        label="Initial set count"
        value={value}
        onChange={onChange}
        min={1}
      />,
    )

    const input = screen.getByLabelText('Initial set count')
    await user.clear(input)
    await user.type(input, '7')

    expect(onChange).toHaveBeenCalled()
    rerender(
      <NumericStepper
        id="count"
        label="Initial set count"
        value="7"
        onChange={onChange}
        min={1}
      />,
    )

    expect(screen.getByLabelText('Initial set count')).toHaveValue(7)
  })
})
