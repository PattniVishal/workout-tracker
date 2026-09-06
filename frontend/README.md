# Workout Tracker Frontend

React + TypeScript + Vite frontend for the Workout Tracker MVP.

## Prerequisites

- Node.js 20+
- Backend API running locally (default `http://localhost:8080`)

## Setup

```bash
npm install
cp .env.example .env
```

## Environment variables

| Variable | Description | Default |
| --- | --- | --- |
| `VITE_API_BASE_URL` | Backend API base URL (includes `/api`) | `http://localhost:8080/api` |

## Scripts

```bash
npm run dev      # Start Vite dev server (http://localhost:5173)
npm run build    # Typecheck and production build
npm run test     # Run unit tests
npm run lint     # Oxlint
```

## Architecture (F0)

```
src/
  app/           # App-level providers (QueryClient, AuthProvider)
  api/           # Shared API transport, CSRF, error parsing
  auth/          # Session auth state (/api/auth/me)
  components/    # Shared UI primitives (ProtectedRoute, LoadingState)
  lib/           # Environment helpers
  routes/        # Router, layouts, placeholder pages
```

Authentication uses **server-side HTTP sessions** with `credentials: 'include'`. CSRF tokens are bootstrapped from `GET /api/auth/csrf` and sent as `X-XSRF-TOKEN` on unsafe requests.

Do not store session credentials in `localStorage` or `sessionStorage`.

## Local development

1. Start PostgreSQL and the backend (`backend/mvnw spring-boot:run`).
2. Start the frontend (`npm run dev`).
3. Visit `http://localhost:5173` — unauthenticated users are redirected to `/login`.
