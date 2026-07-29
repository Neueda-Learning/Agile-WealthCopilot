# Tony — Presentation Script
**Role:** Market Data, External Access & Tests  
**Slides:** 07 Architecture & Stack · 08 Reliable Market Data · 09 Trust & Delivery Evidence · (Demo step 04)  
**Total mic time:** ≈ 3 min 35 sec

---

## Slide 07 · Architecture & Stack — *A simple full stack with clear boundaries.* (1:05)

> **[Advance to slide 07. You are receiving the handoff from Ezio.]**

"Ezio showed you how the business logic works.
My job is to explain the technical stack that makes it reliable and reproducible.

The architecture has three columns, and the boundaries between them are the key design decision.

On the left: a **React 18 and TypeScript** single-page app built with Vite.
It communicates with the backend over HTTPS, attaches a JWT on every request, and handles loading, stale-price, and error states explicitly.

In the centre: a **Spring Boot 3** application on Java 17.
It follows a strict controller → service → repository layering.
Controllers only parse HTTP and delegate.
Services enforce business rules.
Repositories talk to MySQL and are always scoped by user ID.
Schema versioning is handled by **Flyway**, which means every environment — local, test, and production — runs exactly the same migrations.

On the right: two external adapters.
**Twelve Data** supplies market quotes behind a cache layer — Ezio's engine never calls it directly.
A **DeepSeek-compatible LLM** handles AI features through a provider abstraction, so the LLM vendor is swappable without touching the service layer.

The entire application — backend plus MySQL — starts with a single `docker compose up` command.
That is how we reproduce the demo environment today."

---

## Slide 08 · Reliable Market Data — *External data never blocks the portfolio hot path.* (0:50)

> **[Advance to slide 08.]**

"This is the resilience pattern I am most proud of.

The portfolio summary calculation does **not** call Twelve Data on every request.
It reads from a local `price_cache` table in MySQL.

Here is how that cache is populated:

A **scheduled job** runs during market hours and fetches fresh quotes in batch for every symbol currently held across all portfolios.
The results are written to `price_cache` with a timestamp.

When a portfolio summary is requested, it reads the cached price.
If the cache entry is fresh — within the TTL — it is returned as-is.
If it is stale, the portfolio still returns, but the freshness indicator is flagged in the response so the UI can display it to the user.
The stale symbol is also added to a **refresh queue** so it gets updated on the next cycle.

The important consequence: if Twelve Data is slow, rate-limited, or offline, the portfolio still loads.
The user sees 'price data as of X minutes ago' — not an error page.

That is not just an infrastructure detail.
We treat graceful degradation as a **product feature** — the user's portfolio data remains accessible regardless of third-party availability."

---

## Slide 09 · Trust & Delivery Evidence — *Trust is enforced in code—not implied by the UI.* (1:05)

> **[Advance to slide 09.]**

"Security is easy to claim and hard to prove.
Here is how we prove it in this codebase.

**Identity**: passwords are stored as BCrypt hashes, never plaintext.
Authentication returns a short-lived, stateless JWT.
There is no server-side session to steal.

**Database isolation**: every repository method that touches user-owned data takes a `userId` parameter.
That ID comes from the verified JWT in the Spring Security context — not from anything in the request body.
An attacker who knows another user's transaction ID cannot read or modify it; the query will simply return nothing.

**Existence privacy**: a cross-user resource access returns 404, not 403.
This prevents an attacker from probing the system to discover whether a transaction or holding ID exists for another account.

**Repeatable quality**: our test suite covers four layers.
Pure unit tests validate `PortfolioMath` with no Spring context.
Mockito service tests cover business rules in isolation.
Spring Security integration tests verify that authentication and authorisation behave correctly end-to-end.
And Testcontainers spins up a real MySQL instance for repository tests, running the actual Flyway migrations — so we test against the same schema that production uses.

There is also a separate API-key security filter chain for the external endpoint surface, completely independent of the JWT user chain, with keys stored as hashed values."

> **[Hand off to Jasper for the demo steps — Tony returns for Demo step 04.]**

"Jasper will walk us through the frontend and both AI features, then I will close the demo."

---

## Demo Step 04 · Price Freshness & External Health (0:30)

> **[Final demo step, slide 13. You have the keyboard or browser.]**

"Two quick proof points for the reliability story.

First, look at the portfolio dashboard — you can see the price freshness indicator next to each position.
It shows either 'live' or a timestamp like 'as of 14 minutes ago'.
That is the `price_cache` TTL in action.
The portfolio loaded immediately; it did not wait for a Twelve Data call.

Second, I will open the external health endpoint in the browser.

[Navigate to: /api/v1/external/health]

You can see a JSON response confirming the service is up.
This is the health surface for the external API security chain — the same infrastructure that the API-key framework uses.

That concludes the demo.
Back to the slides for our closing summary."

---

## Q&A Guidance

**If asked about the cache refresh interval:**
- The scheduler runs every 15 minutes during market hours.
- Outside market hours (weekends and outside NYSE trading session), the job skips to avoid unnecessary API calls.

**If asked about rate limits from Twelve Data:**
- Requests are batched by symbol. A single API call retrieves multiple quotes, staying within the free-tier rate limits.
- Timeout and retry logic is implemented in `TwelveDataClient`. On timeout, the cache entry is left as-is rather than overwritten with an error state.

**If asked about the Testcontainers setup:**
- `@Testcontainers` plus `@Container` spins up a MySQL 8 Docker container for the test class.
- Flyway runs the same migration scripts as production, so schema drift between test and production is impossible.
- This applies to `MarketDataPersistenceTest`, `FlywayMigrationTest`, and the authentication integration tests.

**If asked about what is NOT in the external health endpoint:**
- The `/api/v1/external/health` endpoint exists and is demonstrated.
- There are no `/external/stats` or `/external/users` endpoints in the current codebase — do not claim these exist.
