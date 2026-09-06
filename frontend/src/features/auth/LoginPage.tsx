import type { FormEvent } from 'react'
import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { ApiError } from '../../api/errors'
import { login } from '../../auth/authApi'
import { useAuth } from '../../auth/useAuth'
import { AuthFormField } from '../../components/AuthFormField'
import { fieldErrorsToMap } from '../../lib/formErrors'

type LoginLocationState = {
  from?: string
  registered?: boolean
}

export function LoginPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const { setUser } = useAuth()
  const locationState = (location.state as LoginLocationState | null) ?? {}

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

    const trimmedEmail = email.trim()
    const clientErrors: Record<string, string> = {}

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
      const user = await login({ email: trimmedEmail, password })
      setUser(user)

      const redirectTo =
        typeof locationState.from === 'string' && locationState.from !== '/login'
          ? locationState.from
          : '/'

      navigate(redirectTo, { replace: true })
    } catch (error) {
      if (error instanceof ApiError) {
        setFieldErrors(fieldErrorsToMap(error.fieldErrors))
        setFormError(error.message)
      } else {
        setFormError('Unable to sign in. Please try again.')
      }
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <section className="w-full rounded-lg border border-slate-200 bg-white p-6 shadow-sm">
      <h1 className="text-xl font-semibold">Sign in</h1>
      <p className="mt-1 text-sm text-slate-600">Sign in to your Workout Tracker account.</p>

      {locationState.registered ? (
        <p className="mt-4 rounded-md border border-green-200 bg-green-50 px-3 py-2 text-sm text-green-800" role="status">
          Account created successfully. Please sign in.
        </p>
      ) : null}

      <form className="mt-6 space-y-4" onSubmit={handleSubmit} noValidate>
        {formError ? (
          <p className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700" role="alert">
            {formError}
          </p>
        ) : null}

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
          autoComplete="current-password"
          disabled={isSubmitting}
        />

        <button
          type="submit"
          disabled={isSubmitting}
          className="w-full rounded-md bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-800 disabled:cursor-not-allowed disabled:bg-slate-400"
        >
          {isSubmitting ? 'Signing in…' : 'Sign in'}
        </button>
      </form>

      <p className="mt-4 text-center text-sm text-slate-600">
        Don&apos;t have an account?{' '}
        <Link className="font-medium text-slate-900 underline" to="/register">
          Create one
        </Link>
      </p>
    </section>
  )
}
