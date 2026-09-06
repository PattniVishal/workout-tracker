# Workout Tracker

MVP workout tracking application with a Spring Boot backend and React frontend.

## Prerequisites

- **JDK 21** (backend)
- **Node.js 22+** and npm (frontend)
- **PostgreSQL 16+** (when running the backend against a database outside Docker)
- **Docker** and **Docker Compose** (optional, for full-stack containerized local development)

## Quick start (Docker Compose — production-like)

From the repository root:

```bash
cp .env.example .env
# Edit .env and set POSTGRES_PASSWORD / DATABASE_PASSWORD

docker compose up --build
```

Open the application at **http://localhost:8080** (or the port set by `COMPOSE_HTTP_PORT`).

All browser traffic goes through **Nginx** on a single origin:

- React app: `/`
- API: `/api/...`
- Health: `/actuator/health`
- Swagger: `/swagger-ui/index.html`

The frontend image is built with `VITE_API_BASE_URL=/api` so API requests stay same-origin. Session cookies and CSRF work through the Nginx reverse proxy.

Stop containers:

```bash
docker compose down
```

Remove containers and database volume:

```bash
docker compose down -v
```

### Optional debugging overrides

To expose PostgreSQL or the backend directly on the host (e.g. for local tools), copy `compose.override.example.yaml` to `compose.override.yaml` (gitignored). Docker Compose merges override files automatically.

### Local URLs (Docker Compose)

| Resource | URL |
|----------|-----|
| Application | http://localhost:8080 |
| API (via Nginx) | http://localhost:8080/api |
| Swagger UI | http://localhost:8080/swagger-ui/index.html |
| Health check | http://localhost:8080/actuator/health |

Backend and PostgreSQL are **not** published to the host by default.

---

## Local development (without Docker)

### Backend

```bash
cd backend
./mvnw spring-boot:run
```

Windows:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

API default: `http://localhost:8080`

Copy `backend/.env.example` to `backend/.env` (or export variables). PostgreSQL must be running with database `workout_tracker`.

### Frontend

```bash
cd frontend
npm install
npm run dev
```

Dev server: `http://localhost:5173`

Copy `frontend/.env.example` to `frontend/.env` if you need a non-default API URL.

---

## Configuration

### Backend environment variables

| Variable | Description |
|----------|-------------|
| `DATABASE_URL` | JDBC URL (default local: `jdbc:postgresql://localhost:5432/workout_tracker`) |
| `DATABASE_USERNAME` | Database user |
| `DATABASE_PASSWORD` | Database password (**required** in production) |
| `SERVER_PORT` | HTTP port (default `8080`) |
| `SPRING_PROFILES_ACTIVE` | Spring profile (`prod` for production HTTPS settings) |
| `CORS_ALLOWED_ORIGINS` | Comma-separated browser origins (default `http://localhost:5173`) |
| `SESSION_COOKIE_SECURE` | `true` for HTTPS production |
| `SESSION_COOKIE_SAME_SITE` | `lax` (local/same-site) or `none` (cross-subdomain HTTPS SPA) |

Schema is managed by **Flyway** under `backend/src/main/resources/db/migration/`.

### Frontend environment variables

| Variable | When | Description |
|----------|------|-------------|
| `VITE_API_BASE_URL` | Build time | API base URL including `/api` path |

See `frontend/.env.production.example` for production build guidance.

---

## Testing

### Backend

```bash
cd backend
./mvnw test
```

Windows: `.\mvnw.cmd test`

### Frontend

```bash
cd frontend
npm run test
```

---

## Building

### Backend JAR

```bash
cd backend
./mvnw -DskipTests package
```

### Frontend static assets

```bash
cd frontend
npm run build
```

Output: `frontend/dist/`

---

## Containers

### Backend image only

```bash
cd backend
docker build -t workout-tracker-backend .
```

Run with environment variables:

```bash
docker run --rm -p 8080:8080 \
  -e DATABASE_URL=jdbc:postgresql://host.docker.internal:5432/workout_tracker \
  -e DATABASE_USERNAME=postgres \
  -e DATABASE_PASSWORD=change-me \
  -e CORS_ALLOWED_ORIGINS=http://localhost:5173 \
  workout-tracker-backend
```

View logs: Docker prints stdout (includes `requestId` from MDC).

### Frontend image only (Nginx + React build)

```bash
cd frontend
docker build --build-arg VITE_API_BASE_URL=/api -t workout-tracker-nginx .
docker run --rm -p 8080:80 workout-tracker-nginx
```

Note: the standalone image serves static files only. API proxying requires the backend on the Compose network.

### Full stack

Use `docker compose up --build` from the repository root (see above).

Validate Compose file:

```bash
docker compose config
```

---

## CI (GitHub Actions)

| Workflow | Triggers | What runs |
|----------|----------|-----------|
| `.github/workflows/backend-ci.yml` | PR (any branch → any branch) | `pr-source-tests` — source branch tests only |
| | Push to `main` (e.g. merge) | Full pipeline: tests, package, Docker build, Compose config |
| `.github/workflows/frontend-ci.yml` | PR (any branch → any branch) | `pr-source-tests` — source branch tests only |
| | Push to `main` (e.g. merge) | Full pipeline: `npm ci`, tests, build, Docker build |

**Push to feature branches does not run CI.** Only pull requests (tests) and merges to `main` (full pipeline) trigger workflows.

Deployments to AWS are **not** configured yet. Future workflows should use GitHub Secrets / AWS OIDC — never commit credentials.

---

## Production deployment

See **[docs/deployment-architecture.md](docs/deployment-architecture.md)** for:

- EC2 + Docker Compose production target
- Cookie/CORS/CSRF production considerations
- Environment variable checklist
- Deployment roadmap (phases B16–B20)

---

## API documentation

When the backend is running:

- OpenAPI JSON: `/v3/api-docs`
- Swagger UI: `/swagger-ui/index.html`

Authentication uses **session cookies**, not JWT. Unsafe requests require the CSRF header from `GET /api/auth/csrf`.
