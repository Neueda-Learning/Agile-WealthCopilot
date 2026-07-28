# WealthCopilot — System Design

**Status:** Approved for scaffolding (branch `init`)
**Source spec:** [project-requirements-spec.md](../project-requirements-spec.md)

---

## 1. Overview

WealthCopilot is a personal investment tracking web app. Users record buy/sell
transactions for stocks and ETFs; the system computes live portfolio value,
unrealized/realized P&L, and performance from cached market data (Twelve Data).
An AI layer provides (1) natural-language transaction entry and (2) a
conversational portfolio agent driven by tool-calling against the app's own
service layer.

### Decisions locked in

| Decision | Choice | Rationale |
|---|---|---|
| Backend | Java 17, Spring Boot 3, Maven | Spec constraint |
| Frontend | React 18 + TypeScript, Vite | Spec constraint (React); TS/Vite chosen for DX |
| Database | MySQL 8, Flyway migrations | Spec constraint; Flyway for reproducible schema |
| Auth (users) | Spring Security + stateless JWT | Standard for SPA + REST; also cleanly separable from external API auth |
| Auth (external/instructor API) | Static API keys (`X-API-Key`), read-only scope | Simple to issue and demo |
| LLM | Provider-agnostic `LlmClient` interface; default impl targets **DeepSeek V4 Pro** (OpenAI-compatible API), swappable to Claude | Spec is contradictory (header says DeepSeek, Feature 1 says Claude); abstraction resolves it |
| Market data | Twelve Data, accessed only through a DB-backed price cache | Spec: never call the external API in a hot path |
| Currency | **USD-only in v1** — non-USD instruments rejected at transaction creation | Avoids half-designed FX conversion; multi-currency is a listed future enhancement |

---

## 2. High-Level Architecture

```
                ┌─────────────────────────────────────────────┐
                │                 React SPA (Vite/TS)          │
                │  Login/Register · Dashboard · Transactions   │
                │  NL entry form · Agent chat                  │
                └───────────────┬─────────────────────────────┘
                                │ HTTPS + JWT (Authorization: Bearer)
                                ▼
┌──────────────────────────────────────────────────────────────────────┐
│                        Spring Boot Backend                           │
│                                                                      │
│  Security filter chains (ordered)                                    │
│   1. ApiKeyAuthFilter  → /api/v1/external/** (health = permitAll)    │
│   2. JwtAuthFilter     → /api/v1/** except /external/**              │
│                                                                      │
│  Controller layer   →   Service layer   →   Repository layer (JPA)   │
│                          │        │                                  │
│        AI services ──────┘        └────── PriceCacheService          │
│         │  LlmClient (interface)            │  read-through + TTL    │
│         │   ├─ DeepSeekLlmClient            │  scheduled refresh     │
│         │   └─ ClaudeLlmClient (alt)        ▼                        │
│         │                            TwelveDataClient                │
│         ▼                                   │                        │
│   Agent tool registry                       │                        │
│   (read-only tools over services)           │                        │
└─────────┼───────────────────────────────────┼────────────────────────┘
          ▼                                   ▼
   DeepSeek / Claude API              Twelve Data API
          
                          MySQL 8
   users · instruments · transactions · price_cache ·
   conversations · chat_messages · api_keys
```

### Request flows

**Portfolio summary (hot path — no external calls):**
`GET /portfolio/summary` → PortfolioService loads the user's transactions,
derives holdings (average-cost basis), joins latest prices from `price_cache`
→ returns totals. If a cached price is stale past TTL, the response still uses
it (marked `stale: true`) and a refresh is queued; the hot path never blocks
on Twelve Data.

**Price refresh (background):** a scheduler runs every 15 minutes during
market hours, collects the distinct instruments held by any user, and fetches
quotes in batches (Twelve Data supports multi-symbol quote calls), respecting
the free-tier rate limit (~8 credits/min). Results upsert into `price_cache`.

**NL transaction entry (AI Feature 1):**
`POST /ai/parse-transaction` → prompt with strict JSON schema → `LlmClient`
→ parse/validate (known ticker? positive quantity? date resolvable?) →
return a **draft** to the frontend, which pre-fills the manual form. Saving
only happens when the user submits the normal `POST /transactions`.
**The parse endpoint never writes to the database.**

**Portfolio agent (AI Feature 2):**
`POST /ai/chat` → AgentService builds the message history + tool definitions
→ tool-calling loop (max N iterations): LLM requests a tool → backend executes
it **scoped to the authenticated user** → result appended → repeat until the
LLM produces a final text answer. All registered tools are read-only; the only
mutation-shaped tool (`draft_transaction`) returns a draft for UI confirmation,
mirroring Feature 1. Messages persist to `conversations`/`chat_messages`.

Agent tool set (v1):

| Tool | Backs onto | Notes |
|---|---|---|
| `get_portfolio_summary` | PortfolioService | totals, unrealized/realized P&L |
| `get_holdings` | PortfolioService | per-instrument position, cost basis, P&L |
| `get_transactions` | TransactionService | filters: ticker, side, date range |
| `get_quote` | PriceCacheService | cached quote only |
| `get_invested_amount` | TransactionService | net cash in over a period |
| `draft_transaction` | (pure) | returns draft JSON; never saves |

Guardrails: system prompt forbids price prediction and buy/sell advice
(spec's explicit out-of-scope items); the tool registry contains no
unmediated write tools, so the constraint is structural, not just prompt-level.

---

## 3. Security & Data Isolation

- **JWT:** short-lived access token (60 min) signed HS256, issued at login.
  Password hashing with BCrypt. No refresh tokens in v1 (re-login), noted as a
  future enhancement.
- **DB-level user isolation (spec requirement):** every user-owned table
  carries a `user_id` FK; **every repository query includes `user_id` taken
  from the authenticated principal** — never from a request parameter. Service
  methods take `userId` as their first argument; controllers resolve it from
  the security context only. Agent tools go through the same service methods,
  so the AI cannot cross user boundaries.
- **External API:** separate `/api/v1/external/**` chain authenticated by
  hashed API keys (SHA-256 stored, plaintext shown once at creation),
  read-only scope, no mutation endpoints mounted under it. The external
  chain is registered **first** (`@Order`) with `/external/health` as
  `permitAll`; the JWT chain explicitly excludes `/external/**` so the two
  never overlap.
- LLM providers receive only the user's message text and tool results —
  never credentials or other users' data.

## 4. Caching Strategy

| Layer | What | TTL / trigger |
|---|---|---|
| MySQL `price_cache` | Latest quote per instrument | Upserted by 15-min scheduler + on-demand refresh when read past TTL |
| Caffeine (in-process) | Hot portfolio computations, instrument lookups | 60s, invalidated on transaction write |

Twelve Data **quote** endpoints are only called from `TwelveDataClient` via
`PriceCacheService` and the scheduler — enforced by package structure.
**Symbol search** (`/market/search`, unknown-ticker resolution on
transaction create) also goes through `TwelveDataClient` per-request; this
is acceptable because it is not a hot path, results are cached in-process,
and the frontend debounces search-as-you-type (300 ms).

## 5. Error Handling

Single `@RestControllerAdvice` mapping to a uniform error body
(`code`, `message`, `details[]`, `timestamp`, `path`). LLM failures
(timeout, malformed JSON, rate limit) degrade gracefully: parse endpoint
returns `422 AI_PARSE_FAILED` with a hint to use the manual form; the agent
returns a fallback message. External API failures never surface Twelve Data
errors to users — stale cache is served instead.

## 6. Testing Strategy (per spec minimums)

- **Unit:** P&L / cost-basis math (pure functions in `service`), agent tool
  logic, NL-parse validation. JUnit 5 + Mockito.
- **Integration:** repository user-scoping tests against Testcontainers MySQL;
  security tests proving cross-user access returns **404** (the API contract —
  never 403, to avoid leaking resource existence). Timeline re-validation on
  transaction edit/delete gets dedicated unit tests.
- **E2E (time permitting):** Playwright against the running stack with a
  stubbed LLM and market-data client.

## 7. Trade-offs & Revisit Later

- **Average-cost basis** for P&L (simpler, matches most broker displays) over
  FIFO lots. Revisit if tax-lot reporting is ever needed.
- **Synchronous chat responses** (single JSON reply) over SSE streaming —
  simpler; streaming is a UI polish item later.
- **Layer-based packaging** (`controller/service/repository`) as requested;
  at larger scale feature-based packaging would scale better.
- **No refresh tokens / no rate limiting on auth** in v1 — acceptable for a
  course project, listed as hardening work.
- **USD-only** — non-USD instruments rejected in v1; multi-currency needs an
  FX-rate source and converted summaries. Revisit if non-US listings matter.
- **Single deployable + single DB** — correct for this scale; the price
  scheduler and AI calls are the only components that would ever split out.
