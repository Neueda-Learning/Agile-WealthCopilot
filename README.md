# WealthCopilot

Personal investment tracker with an AI portfolio agent.
Spring Boot (Java 17, Maven) · React 18 + TypeScript (Vite) · MySQL 8 ·
Twelve Data market data · provider-agnostic LLM layer (DeepSeek V4 Pro default).

## Documentation

- [Requirements spec](project-requirements-spec.md)
- [System design](docs/system-design.md)
- [API specification](docs/api-spec.md)
- [Database schema](docs/database-schema.md)
- [GitHub administration](docs/github-administration.md)
- [Deployment and demo runbook](docs/deployment-runbook.md)

## Repository layout

```
backend/                          Spring Boot service
  src/main/java/com/wealthcopilot/
    config/       Spring configuration (CORS, cache, LLM/market-data props)
    security/     JWT filter, API-key filter, security chains
    controller/   REST controllers (auth, transactions, portfolio, ai, external)
    service/      Business logic; P&L math lives here as pure functions
      ai/         LlmClient interface + providers, agent tool registry
    repository/   Spring Data JPA repositories — every query user-scoped
    entity/       JPA entities mirroring docs/database-schema.md
    dto/          request/ and response/ payloads from docs/api-spec.md
    client/       TwelveDataClient and other outbound HTTP clients
    scheduler/    Price-cache refresh job
    exception/    Error codes + @RestControllerAdvice
  src/main/resources/db/migration/   Flyway migrations (V1__baseline.sql, ...)
  src/test/java/                     Unit + repository isolation tests

frontend/                         React SPA (not yet implemented)
  src/
    api/          Typed API client per docs/api-spec.md
    pages/        Login, Register, Dashboard, Transactions, Chat
    components/   Shared UI (forms, tables, charts)
    hooks/        Data-fetching hooks
    context/      Auth context (JWT storage)
    types/        Shared TypeScript types

docs/                             Design documents
```

## Local development

Prerequisites: Java 17+, Maven 3.8+, Docker with Compose.

```bash
cp .env.example .env
docker compose up -d mysql
cd backend
./mvnw spring-boot:run
```

The defaults in `.env.example` match the `dev` profile. Spring Boot listens on
`http://localhost:8080`; its health endpoint is
`http://localhost:8080/actuator/health`. The dev profile permits CORS from the
Vite origin `http://localhost:5173`.

Flyway runs automatically on startup and is the only supported way to change
the database schema. Do not edit a local schema manually. Create a new,
forward-only migration under
`backend/src/main/resources/db/migration/` instead.

Useful commands:

```bash
# Run backend tests (includes a disposable Testcontainers MySQL instance)
cd backend && ./mvnw test

# Follow MySQL logs
docker compose logs -f mysql

# Stop local services (keeps database data)
docker compose down
```

For the production/demo Compose stack, follow
[`docs/deployment-runbook.md`](docs/deployment-runbook.md). It builds the
backend as a non-root container, keeps MySQL private, requires deployment
secrets, and exposes an application health check.

Database and port settings can be overridden in `.env`; application settings
can also be supplied directly through `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`,
`SERVER_PORT`, `FRONTEND_ORIGIN`, `JWT_SECRET`, and
`JWT_EXPIRATION_SECONDS`. JWT access tokens use HS256 and expire after 3600
seconds; v1 intentionally has no refresh token. Set a unique `JWT_SECRET` of at
least 32 characters outside local development.

If port 3306 is already in use, change both sides of the mapping in `.env`, for
example:

```dotenv
MYSQL_PORT=3307
DB_URL=jdbc:mysql://localhost:3307/wealthcopilot?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
```

## Authentication

Register and log in through the API contract in `docs/api-spec.md`:

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"simon@example.com","password":"change-me","displayName":"Simon"}'

curl -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"simon@example.com","password":"change-me"}'

curl http://localhost:8080/api/v1/auth/me \
  -H 'Authorization: Bearer <accessToken>'
```

Passwords are stored only as BCrypt hashes. Controllers obtain the current
user id from the authenticated principal. For every user-owned entity,
service methods take that id as their first argument and repositories must
query by both resource id and `user_id`, for example
`findByIdAndUserId(resourceId, userId)`. A resource owned by another user is
indistinguishable from a missing resource and returns `404`.

## Status

Backend foundation is implemented on `feature_simon`: Spring Boot 3 / Java 17,
Maven, dev/test profiles, MySQL Compose, Flyway, and the `users` baseline
migration. WEAL-3 adds stateless Spring Security, HS256 JWT access tokens,
BCrypt password hashing, registration/login/current-user endpoints, uniform
API errors, and MySQL-backed user-isolation integration tests. WEAL-4/5 add
global error-path coverage, PR CI, documented GitHub protection settings, a
non-root production container stack, and a deployment/demo runbook.
