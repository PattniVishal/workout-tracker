export type FieldError = {
  field: string
  message: string
}

export type ApiErrorBody = {
  status: number
  code: string
  message: string
  fieldErrors: FieldError[]
}

export type UserResponse = {
  id: string
  displayName: string
  email: string
}

export type RegisterRequest = {
  displayName: string
  email: string
  password: string
}

export type LoginRequest = {
  email: string
  password: string
}

export type CsrfTokenResponse = {
  headerName: string
  token: string
}
