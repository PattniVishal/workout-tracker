import type { FormEvent } from 'react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { ApiError } from '../../api/errors'
import { register } from '../../auth/authApi'
import { AuthFormField } from '../../components/AuthFormField'
import { fieldErrorsToMap } from '../../lib/formErrors'

export function RegisterPage() {
  const navigate = useNavigate()

  const [displayName, setDisplayName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [formError, setFormError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    if (isSubmitting) {
      return
    }

    const trimmedDisplayName = displayName.trim()
    const trimmedEmail = email.trim()
    const clientErrors: Record<string, string> = {}

    if (!trimmedDisplayName) {
      clientErrors.displayName = 'Display name is required.'
    }

    if (!trimmedEmail) {
      clientErrors.email = 'Email is required.'
    }

    if (!password) {
      clientErrors.password = 'Password is required.'
    }

    if (Object.keys(clientErrors).length > 0) {
      setFieldErrors(clientErrors)
      setFormError(null)
      return
    }

    setIsSubmitting(true)
    setFieldErrors({})
    setFormError(null)

    try {
      await register({
        displayName: trimmedDisplayName,
        email: trimmedEmail,
        password,
      })

      navigate('/login', { replace: true, state: { registered: true } })
    } catch (error) {
      if (error instanceof ApiError) {
        setFieldErrors(fieldErrorsToMap(error.fieldErrors))
        setFormError(error.message)
      } else {
        setFormError('Unable to create account. Please try again.')
      }
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <section className="w-full rounded-lg border border-slate-200 bg-white p-6 shadow-sm">
      <h1 className="text-xl font-semibold">Create account</h1>
      <p className="mt-1 text-sm text-slate-600">Register to start tracking your workouts.</p>

      <form className="mt-6 space-y-4" onSubmit={handleSubmit} noValidate>
        {formError ? (
          <p className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700" role="alert">
            {formError}
          </p>
        ) : null}

        <AuthFormField
          id="displayName"
          label="Display name"
          value={displayName}
          onChange={setDisplayName}
          error={fieldErrors.displayName}
          autoComplete="name"
          disabled={isSubmitting}
        />

        <AuthFormField
          id="email"
          label="Email"
          type="email"
          value={email}
          onChange={setEmail}
          error={fieldErrors.email}
          autoComplete="email"
          disabled={isSubmitting}
        />

        <AuthFormField
          id="password"
          label="Password"
          type="password"
          value={password}
          onChange={setPassword}
          error={fieldErrors.password}
          autoComplete="new-password"
          disabled={isSubmitting}
        />

        <button
          type="submit"
          disabled={isSubmitting}
          className="w-full rounded-md bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-800 disabled:cursor-not-allowed disabled:bg-slate-400"
        >
          {isSubmitting ? 'Creating account…' : 'Create account'}
        </button>
      </form>

      <p className="mt-4 text-center text-sm text-slate-600">
        Already have an account?{' '}
        <Link className="font-medium text-slate-900 underline" to="/login">
          Sign in
        </Link>
      </p>
    </section>
  )
}
