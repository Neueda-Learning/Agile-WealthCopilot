# WealthCopilot

Personal investment tracker with an AI portfolio agent.
Spring Boot (Java 17, Maven) · React 18 + TypeScript (Vite) · MySQL 8 ·
Twelve Data market data · provider-agnostic LLM layer (DeepSeek V4 Pro default).

## Documentation

- [Requirements spec](project-requirements-spec.md)
- [System design](docs/system-design.md)
- [API specification](docs/api-spec.md)
- [Database schema](docs/database-schema.md)

## Repository layout

```
backend/                          Spring Boot service (not yet implemented)
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

## Status

Branch `init` holds the approved design and empty scaffold.
Implementation has not started.
