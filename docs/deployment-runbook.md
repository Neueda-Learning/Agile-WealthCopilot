# WealthCopilot Deployment and Demo Runbook

This runbook deploys the Spring Boot backend and MySQL as a reproducible Docker
Compose stack. Run it on a Linux VM or other Docker host reachable by the
instructors.

## 1. Host prerequisites

- Docker Engine 24+ with Docker Compose v2
- Repository checkout
- Inbound TCP access to `APP_PORT` (default `8080`)
- At least 2 GB RAM and 5 GB free disk

Do not expose MySQL publicly. The deployment Compose file publishes only the
backend port.

## 2. Configure secrets

```bash
cp .env.deploy.example .env.deploy
openssl rand -base64 48
```

Put the generated value in `JWT_SECRET`, replace both database passwords, and
keep `.env.deploy` out of Git.

## 3. Build and start

```bash
docker compose \
  --env-file .env.deploy \
  -f compose.deploy.yaml \
  up -d --build
```

Flyway runs automatically before the application accepts traffic. Check status:

```bash
docker compose --env-file .env.deploy -f compose.deploy.yaml ps
curl --fail http://localhost:${APP_PORT:-8080}/actuator/health
```

Expected response:

```json
{"status":"UP","groups":["liveness","readiness"]}
```

## 4. Authentication smoke test

```bash
BASE_URL=http://localhost:${APP_PORT:-8080}
DEMO_EMAIL=demo@example.com

curl -i -X POST "$BASE_URL/api/v1/auth/register" \
  -H 'Content-Type: application/json' \
  -d '{"email":"demo@example.com","password":"demo-password","displayName":"Demo User"}'

curl -X POST "$BASE_URL/api/v1/auth/login" \
  -H 'Content-Type: application/json' \
  -d '{"email":"demo@example.com","password":"demo-password"}'
```

Copy `accessToken` from the login response:

```bash
curl "$BASE_URL/api/v1/auth/me" \
  -H 'Authorization: Bearer <accessToken>'
```

## 5. Presentation checklist

1. Confirm both containers are healthy 30 minutes before the demo.
2. Confirm the instructor-facing URL is reachable from a non-corporate
   network.
3. Register a fresh user to demonstrate onboarding.
4. Log in and retain the JWT for the frontend/API demonstration.
5. Enter the first BUY transaction through the normal confirmation flow once
   the transaction module is integrated.
6. Show that another user cannot access the first user's records.
7. Keep a local deployment running as the fallback.

## 6. Logs and recovery

```bash
docker compose --env-file .env.deploy -f compose.deploy.yaml logs --tail=200 backend
docker compose --env-file .env.deploy -f compose.deploy.yaml restart backend
```

Deploy a newer revision:

```bash
git pull --ff-only
docker compose \
  --env-file .env.deploy \
  -f compose.deploy.yaml \
  up -d --build
```

Roll back by checking out the previously demonstrated Git tag or commit and
running the same `up -d --build` command.

## 7. Shutdown and data

Stop the application while preserving MySQL data:

```bash
docker compose --env-file .env.deploy -f compose.deploy.yaml down
```

The named volume stores the database. Removing it deletes demo data, so never
use `down --volumes` during the presentation.
