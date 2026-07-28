# WealthCopilot — Team Roles & Task Distribution

**Team size:** 4 · **Timeline:** 3 days
**Source:** [project-requirements-spec.md](../project-requirements-spec.md), [system-design.md](system-design.md)

| Member | Role |
|---|---|
| **Simon** | Backend — Platform, Auth & Security |
| **Ezio** | Backend — Transactions & Portfolio Engine |
| **Jasper** | Full-Stack — Frontend + AI Engineer |
| **Tony** | Backend — Market Data, External API & Test Infrastructure |

**Shape of the split:** Jasper is the single full-stack AI/frontend engineer —
the entire React SPA plus both AI features, backend and UI. The other three
divide the backend by concern: **Simon** owns the platform and everything about
identity, **Ezio** owns domain logic and the money math, **Tony** owns outbound
integrations, the instructor-facing API, and test infrastructure.

---

## Simon — Backend · Platform, Auth & Security

The foundation. S1–S3 gate the whole team, so they land on day 1 morning before
anything else is attempted.

| # | Task | Spec source |
|---|------|-------------|
| S1 | Spring Boot project setup: Maven, profiles, Flyway wiring, MySQL connection, local dev compose | Tech stack constraints |
| S2 | User registration/login with Spring Security + JWT, BCrypt password hashing | "Strict user authentication" |
| S3 | DB-level user isolation: `user_id` scoping convention in every repository, security context → service `userId` argument (never from a request parameter) | "individual user data segregated at the database level" |
| S4 | `users` schema + Flyway baseline migration | Base requirements |
| S5 | Global error handling: `@RestControllerAdvice`, uniform error body (`code`, `message`, `details[]`, `timestamp`, `path`) | System design §5 |
| S6 | Security integration tests: cross-user access returns **404**, not 403 | Testing minimums |
| S7 | GitHub repo administration: branch protection, PR checks, instructors added as viewers | Non-functional: version control |
| S8 | CI pipeline: build + tests on every PR | Non-functional: version control |
| S9 | Deployment/runbook for the final presentation | Presentation readiness |

## Ezio — Backend · Transactions & Portfolio Engine

The core product logic and the hardest maths in the project. Ezio consumes
Tony's price cache through an interface, so both start in parallel against a stub.

| # | Task | Spec source |
|---|------|-------------|
| E1 | Flyway schema: `instruments`, `transactions` | Base requirements |
| E2 | Transaction CRUD: record buy/sell, edit, delete, with validation and the USD-only rule | "Users record purchases and sales" |
| E3 | Portfolio engine: holdings derivation, average-cost-basis tracking, unrealized and realized P&L | "calculates current portfolio value, profit/loss" |
| E4 | Performance summary endpoint (totals, per-holding breakdown, stale-price flag pass-through) | "and performance" |
| E5 | Invested-amount-over-period query (net cash in, date-range filtered) | Agent tool + dashboard need |
| E6 | Timeline re-validation on transaction edit/delete (a sell must never exceed holdings at that date) | System design §6 |
| E7 | Caffeine caching of hot portfolio computations, invalidated on transaction write | System design §4 |
| E8 | Service interfaces that Jasper's agent tools call — one method per tool, each taking `userId` first | AI Feature 2 |
| E9 | Unit tests on P&L and cost-basis math | **Spec-mandated minimum** |

## Tony — Backend · Market Data, External API & Test Infrastructure

Everything outbound plus the instructor-facing surface. Tony's Twelve Data work
is the same shape as an LLM client (external HTTP, JSON parsing, rate limits,
graceful degradation), which makes Tony the natural backup for Jasper's AI backend.

| # | Task | Spec source |
|---|------|-------------|
| T1 | Flyway schema: `price_cache`, `api_keys` | System design |
| T2 | `TwelveDataClient`: quote and symbol-search calls, retry and timeout handling, never called outside this package | "market-data APIs: Twelve Data" |
| T3 | `PriceCacheService`: read-through cache with TTL, stale-price marking, refresh queued rather than blocking | "never called per-request in a hot path" |
| T4 | Scheduled price refresh: 15-min batch job over distinct held instruments, multi-symbol calls, free-tier rate limit (~8 credits/min) | "called on a schedule … with caching" |
| T5 | Symbol search / ticker resolution endpoint with in-process result caching | Transaction entry needs |
| T6 | External instructor API: `X-API-Key` filter chain registered first, hashed key storage (SHA-256, plaintext shown once), read-only scope, `/external/health` public | "expose an API to external users" |
| T7 | Read-model DTOs for the external API, over Ezio's services | Same |
| T8 | Graceful degradation: Twelve Data failures never surface to users — stale cache served instead | System design §5 |
| T9 | Test infrastructure: stubbed LLM and market-data clients, seed data | Testing strategy |
| T10 | E2E tests (Playwright against the running stack) — stretch, only if time allows | "end-to-end tests if time allows" |

## Jasper — Full-Stack · Frontend + AI Engineer

The entire React SPA and both AI features end to end. Sequenced so the shared
UI layer exists before feature screens, and Feature 1 proves the LLM plumbing
before the agent is attempted.

**Frontend — application shell and core screens**

| # | Task | Spec source |
|---|------|-------------|
| J1 | Vite + React 18 + TS scaffold, routing, API client with JWT handling and error surfacing | Tech stack constraints |
| J2 | Shared UI layer: component primitives, form controls, loading and error states | "Consumer-grade UI/UX" |
| J3 | Login / registration pages, token storage, auth guard | Strict authentication |
| J4 | Dashboard: portfolio value, unrealized/realized P&L, performance, stale-price indicator | Base requirements |
| J5 | Holdings table: per-instrument position, cost basis, P&L | Base requirements |
| J6 | Transaction pages: manual entry form, history list, edit/delete | Base requirements |
| J7 | Ticker search-as-you-type with 300 ms debounce | System design §4 |
| J8 | Responsive layout and UX polish pass before the presentation | "Consumer-grade UI/UX" |

**AI — Feature 1, then Feature 2**

| # | Task | Spec source |
|---|------|-------------|
| J9 | `LlmClient` interface + DeepSeek V4 Pro implementation (OpenAI-compatible) | Tech stack: AI |
| J10 | Feature 1 backend: `POST /ai/parse-transaction` — strict JSON-schema prompt, parse and validate, returns a **draft only and never writes to the database** | AI Feature 1 |
| J11 | Feature 1 UI: free-text box that pre-fills the manual entry form (J6) for confirm/correct before save | "No auto-save without user confirmation" |
| J12 | AI error handling: timeout / malformed JSON / rate limit → `422 AI_PARSE_FAILED`, fall back to the manual form | AI Feature 1 "error handling" |
| J13 | Feature 2: agent tool registry — `get_portfolio_summary`, `get_holdings`, `get_transactions`, `get_quote`, `get_invested_amount`, `draft_transaction` | AI Feature 2 |
| J14 | Feature 2: tool-calling loop with max-iteration cap, every tool scoped to the authenticated user through Ezio's services | AI Feature 2 |
| J15 | Conversation persistence (`conversations`, `chat_messages`) | System design |
| J16 | Feature 2 UI: chat interface, message history, draft-transaction confirmation card | AI Feature 2 |
| J17 | Guardrails: system prompt forbidding price prediction and buy/sell advice; verify the registry contains no unmediated write tools | "Explicitly out of scope" |
| J18 | Unit tests on agent tool logic and NL-parse validation | **Spec-mandated minimum** |

---

## Load balance — read this before committing to it

Jasper holds 18 of the project's 46 tasks, including both spec-mandated
headline features plus every screen — roughly 40% of the work on one person,
in 3 days. Three things keep it viable:

1. **Simon, Ezio and Tony each publish interfaces on day 1.** Jasper builds
   against stubs and is never blocked waiting for a backend endpoint.
2. **E8 exists specifically to serve Jasper.** Ezio writes the per-tool service
   methods so the agent layer is thin glue, not re-implemented business logic.
3. **A pre-agreed hand-off, decided now rather than in a panic on day 3.** If
   Feature 1 (J9–J12) has not shipped by the end of day 2:
   - **Tony takes J9 (`LlmClient`)** — same external-HTTP-client shape as T2,
     and Tony already owns the stubs.
   - **Simon takes J14 (tool-calling loop)** — Simon owns the user-scoping
     convention and is already reviewing it for boundary correctness.

   That leaves Jasper the UI and prompt work, which no one else can absorb cheaply.

## Cut line for 3 days

If the schedule slips, drop in this order — everything below the line is
explicitly optional in the spec or the design:

1. T10 (E2E tests) — spec says "if time allows".
2. J8 (polish pass) — keep only what the demo screens need.
3. E7 (Caffeine layer) — the DB price cache alone already satisfies the
   "no external call in a hot path" requirement.
4. S9 (deployment) — demo from a local stack if necessary.

**Never cut:** S3 (user isolation), E9 and J18 (the two spec-mandated test
minimums), J17 (guardrails), T6 (instructor API). These are graded requirements.

## Jira board

Project **WEAL** — https://agileteamjira.atlassian.net/browse/WEAL

Tickets are deliberately coarse: one per work area, not one per task above.
Three days does not justify fine-grained tracking, and the detail lives here.

| Ticket | Work area | Owner | Plan ref |
|---|---|---|---|
| WEAL-2 | Project setup and database foundation | Simon | S1, S4 |
| WEAL-3 | Authentication and per-user data isolation | Simon | S2, S3, S6 |
| WEAL-4 | Error handling, CI and GitHub setup | Simon | S5, S7, S8 |
| WEAL-5 | Deployment and demo runbook | Simon | S9 |
| WEAL-6 | Transaction schema and CRUD | Ezio | E1, E2, E6 |
| WEAL-7 | Portfolio engine and P&L calculation | Ezio | E3–E5, E7, E9 |
| WEAL-8 | Service interfaces for AI agent tools | Ezio | E8 |
| WEAL-9 | Twelve Data client and price cache | Tony | T1–T3, T8 |
| WEAL-10 | Scheduled price refresh and symbol search | Tony | T4, T5 |
| WEAL-11 | External instructor API | Tony | T6, T7 |
| WEAL-12 | Test infrastructure and E2E tests | Tony | T9, T10 |
| WEAL-13 | Frontend scaffold, shared UI and auth pages | Jasper | J1–J3 |
| WEAL-14 | Dashboard, holdings and transaction screens | Jasper | J4–J8 |
| WEAL-15 | AI Feature 1: natural-language transaction entry | Jasper | J9–J12 |
| WEAL-16 | AI Feature 2: conversational portfolio agent | Jasper | J13–J18 |

Tony's four tickets are unassigned — no matching Jira account exists on the
site yet. Invite Tony, then assign WEAL-9 through WEAL-12 and drop the
`[Tony]` prefix from their summaries.

## Coordination (no scrum master)

The spec requires a Jira board, so the duty is shared rather than owned. Tickets
are deliberately coarse — one per work area, not per task — because 3 days does
not justify fine-grained tracking:

- Each member moves their own tickets and adds a comment when something blocks.
- Board hygiene is checked at a short daily sync; no one person chases it.
- Simon owns the GitHub side (branch protection, CI) as tooling, not project
  management.

**Reviews:** every PR reviewed by one other member. Jasper's AI PRs are
additionally reviewed by Simon for user-scoping correctness — agent tools are
the easiest place to accidentally cross a user boundary, and the spec's
isolation requirement is non-negotiable.

## 3-day schedule

**Day 1** — Simon: S1–S5 (skeleton, auth, isolation, error handling).
Jasper: J1–J3 (scaffold, shared UI, login). Ezio: E1–E2 (schema, transaction
CRUD) once S1 lands. Tony: T1–T3 (price cache, Twelve Data client).
*End of day: interfaces published, everyone unblocked.*

**Day 2** — Ezio: E3–E5 (portfolio engine, the critical path). Tony: T4–T6
(scheduler, search, instructor API). Jasper: J4–J7 (dashboard, holdings,
transactions) then J9–J12 (Feature 1).
*End of day: the app works without AI; Feature 1 shipped or handed off.*

**Day 3** — Jasper: J13–J17 (the agent) — deliberately last so it builds on
finished services. Ezio: E6–E9. Tony: T7–T9. Simon: S6–S9 (security tests, CI,
deployment). Afternoon: freeze, integration pass, demo rehearsal.
