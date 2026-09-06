type NumericStepperProps = {
  id: string
  label: string
  value: string
  onChange: (value: string) => void
  min?: number
  step?: number
  disabled?: boolean
}

function clampToMin(value: number, min: number): number {
  return Math.max(min, value)
}

function parseValue(value: string): number | null {
  const trimmed = value.trim()
  if (!trimmed) {
    return null
  }

  const parsed = Number(trimmed)
  return Number.isNaN(parsed) ? null : parsed
}

export function NumericStepper({
  id,
  label,
  value,
  onChange,
  min = 0,
  step = 1,
  disabled = false,
}: NumericStepperProps) {
  function adjust(delta: number) {
    const current = parseValue(value)
    const next = clampToMin((current ?? min) + delta, min)
    onChange(String(next))
  }

  return (
    <div>
      <label htmlFor={id} className="block text-xs font-medium text-slate-600">
        {label}
      </label>
      <div className="mt-1 flex min-w-0 items-stretch">
        <button
          type="button"
          aria-label={`Decrease ${label.toLowerCase()}`}
          onClick={() => adjust(-step)}
          disabled={disabled}
          className="rounded-l-md border border-slate-300 bg-white px-3 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50 disabled:opacity-60"
        >
          −
        </button>
        <input
          id={id}
          type="number"
          min={min}
          step={step}
          value={value}
          onChange={(event) => onChange(event.target.value)}
          disabled={disabled}
          className="app-input min-w-0 flex-1 rounded-none border-x-0 text-center"
        />
        <button
          type="button"
          aria-label={`Increase ${label.toLowerCase()}`}
          onClick={() => adjust(step)}
          disabled={disabled}
          className="rounded-r-md border border-slate-300 bg-white px-3 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50 disabled:opacity-60"
        >
          +
        </button>
      </div>
    </div>
  )
}
