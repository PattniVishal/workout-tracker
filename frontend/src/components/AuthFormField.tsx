type AuthFormFieldProps = {
  id: string
  label: string
  type?: 'text' | 'email' | 'password'
  value: string
  onChange: (value: string) => void
  error?: string
  autoComplete?: string
  disabled?: boolean
}

export function AuthFormField({
  id,
  label,
  type = 'text',
  value,
  onChange,
  error,
  autoComplete,
  disabled = false,
}: AuthFormFieldProps) {
  return (
    <div>
      <label htmlFor={id} className="block text-sm font-medium text-slate-700">
        {label}
      </label>
      <input
        id={id}
        name={id}
        type={type}
        value={value}
        onChange={(event) => onChange(event.target.value)}
        autoComplete={autoComplete}
        disabled={disabled}
        aria-invalid={error ? true : undefined}
        aria-describedby={error ? `${id}-error` : undefined}
        className="app-input mt-1 disabled:bg-slate-100"
      />
      {error ? (
        <p id={`${id}-error`} className="mt-1 text-sm text-red-600" role="alert">
          {error}
        </p>
      ) : null}
    </div>
  )
}
