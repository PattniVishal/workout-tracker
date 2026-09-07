# Temporary Render deployment guide

> **Note:** Render is used temporarily for deployment validation while AWS account provisioning/review is pending. The finalized target architecture remains **EC2 + Docker Compose + Nginx + PostgreSQL** (see `deployment-architecture.md`).

This document describes how to deploy the existing Docker images to Render **manually**. Nothing in this repository deploys to Render automatically.

## Target layout on Render

```
Browser (HTTPS)
   |
   v
Render Web Service — Frontend (Nginx + React)
   |  same origin: /api/*
   v
Render Web Service — Backend (Spring Boot)
   |
   v
Render PostgreSQL
```

The browser must only talk to the **frontend** URL. Nginx proxies `/api/*` to the backend using environment variables.

## Services to create (manual, in Render dashboard)

1. **PostgreSQL** — Render managed database
2. **Backend** — Docker web service (`backend/Dockerfile`)
3. **Frontend** — Docker web service (`frontend/Dockerfile`)

Create the database first, then the backend, then the frontend.

## Frontend web service

| Setting | Value |
|---------|--------|
| Root directory | `frontend` (or build from repo with Docker context `./frontend`) |
| Dockerfile | `frontend/Dockerfile` |
| Build arg | `VITE_API_BASE_URL=/api` |

### Environment variables

| Variable | Example / notes |
|----------|-----------------|
| `NGINX_BACKEND_HOST` | Render **internal** hostname of the backend service (e.g. `workout-tracker-backend.onrender.com` or private service URL Render provides) |
| `NGINX_BACKEND_PORT` | `443` if using Render HTTPS internal URL, or `10000` / platform port — **use the port Render documents for service-to-service calls** |
| `PORT` | Set automatically by Render; entrypoint maps this to `NGINX_LISTEN_PORT` |

Do **not** set `VITE_API_BASE_URL` to the backend URL at runtime. It is a **build-time** argument and must remain `/api`.

### Health check

Path: `/`

Nginx serves the React app at `/` (`index.html` with SPA fallback). There is no separate frontend health endpoint; use `/` to verify the Nginx web service is responding. Do **not** use `/actuator/health` here — that is a Spring Boot backend endpoint (Nginx only proxies it to the backend when requested through the frontend URL).

## Backend web service

| Setting | Value |
|---------|--------|
| Root directory | `backend` |
| Dockerfile | `backend/Dockerfile` |

### Environment variables

| Variable | Example / notes |
|----------|-----------------|
| `PORT` | Set automatically by Render; Spring Boot binds via `server.port` |
| `SPRING_PROFILES_ACTIVE` | `prod` (enables secure cookies + forwarded headers) |
| `DATABASE_URL` | JDBC URL, e.g. `jdbc:postgresql://<host>:5432/<db>` — convert from Render's connection string if needed |
| `DATABASE_USERNAME` | From Render database credentials |
| `DATABASE_PASSWORD` | From Render database credentials |
| `SERVER_FORWARD_HEADERS_STRATEGY` | `framework` |
| `SESSION_COOKIE_SECURE` | `true` (HTTPS) |
| `SESSION_COOKIE_SAME_SITE` | `lax` (same-origin via frontend Nginx) |
| `CORS_ALLOWED_ORIGINS` | `https://<your-frontend-service>.onrender.com` (optional; same-origin API does not require CORS for normal browser traffic) |

Render may provide `DATABASE_URL` as `postgres://...`. Spring Boot requires JDBC format:

```
jdbc:postgresql://<host>:<port>/<database>
```

Flyway runs automatically on backend startup when the database is reachable.

### Health check

Path: `/actuator/health`

This is the Spring Boot actuator endpoint exposed by the backend service directly (not via the frontend Nginx proxy).

## Session, CSRF, and cookies

- Authentication remains **session-based** (`JSESSIONID`) with **CSRF** (`XSRF-TOKEN`).
- The browser calls `https://<frontend>/api/...` — same origin.
- Nginx forwards requests to the backend; `SERVER_FORWARD_HEADERS_STRATEGY=framework` preserves proxy headers.
- Use `SESSION_COOKIE_SECURE=true` and `SESSION_COOKIE_SAME_SITE=lax` for HTTPS same-origin deployment.
- Do **not** disable CSRF or switch to JWT.

## Local Docker Compose (unchanged)

Local development continues to use root `compose.yaml`:

```bash
cp .env.example .env
docker compose up --build
```

Defaults: `NGINX_BACKEND_HOST=backend`, `NGINX_BACKEND_PORT=8080`, PostgreSQL service `postgres:5432`.

## Render-specific assumptions

1. **Internal networking** — Frontend must reach the backend on Render's internal/private hostname. Confirm the correct host and port in Render's service linking docs.
2. **HTTPS** — Render terminates TLS at the edge; cookies must be `Secure` in production.
3. **Cold starts** — Free/starter tiers may sleep; first request can be slow.
4. **No Compose on Render** — Each service is deployed separately; `compose.yaml` remains for local/EC2 use only.
5. **Database SSL** — Render PostgreSQL may require `?sslmode=require` on the JDBC URL; add if connection fails.

## What is not included

- No `render.yaml` or automated Render provisioning in this repo
- No changes to the primary AWS architecture document beyond this temporary note
- No CD pipeline to Render
