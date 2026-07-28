# WealthCopilot — API Specification

Base URL: `/api/v1`
Content type: `application/json` (UTF-8) everywhere.

## Authentication

| Surface | Mechanism | Header |
|---|---|---|
| User endpoints (`/api/v1/**`) | JWT access token (60 min, HS256) | `Authorization: Bearer <token>` |
| External endpoints (`/api/v1/external/**`) | Static API key, read-only | `X-API-Key: <key>` |
| `POST /auth/register`, `POST /auth/login` | none | — |

Every user endpoint operates on the authenticated user's data only. There is
no way to pass another user's id — user identity always comes from the token.

## Standard error body

All errors (4xx/5xx) share:

```json
{
  "code": "VALIDATION_FAILED",
  "message": "quantity must be greater than 0",
  "details": [{ "field": "quantity", "issue": "must be > 0" }],
  "timestamp": "2026-07-27T14:03:00Z",
  "path": "/api/v1/transactions"
}
```

Common codes: `VALIDATION_FAILED` (400), `UNAUTHORIZED` (401),
`FORBIDDEN` (403), `NOT_FOUND` (404), `CONFLICT` (409),
`AI_PARSE_FAILED` (422), `AI_UNAVAILABLE` (503), `INTERNAL_ERROR` (500).
Resources belonging to other users return `404` (not `403`) to avoid
existence leaks.

Paginated list responses share:

```json
{ "content": [ ... ], "page": 0, "size": 20, "totalElements": 57, "totalPages": 3 }
```

---

## 1. Auth

### POST /auth/register
```json
// request
{ "email": "a@b.com", "password": "min 8 chars", "displayName": "Jasper" }
// 201 response
{ "id": 1, "email": "a@b.com", "displayName": "Jasper" }
```
Errors: 400 validation, 409 `CONFLICT` email taken.

### POST /auth/login
```json
// request
{ "email": "a@b.com", "password": "..." }
// 200 response
{ "accessToken": "eyJ...", "tokenType": "Bearer", "expiresInSeconds": 3600 }
```
Errors: 401 bad credentials.

### GET /auth/me
`200` → `{ "id": 1, "email": "a@b.com", "displayName": "Jasper" }`

---

## 2. Transactions

### GET /transactions
Query params: `ticker?`, `side?` (BUY|SELL), `from?`, `to?` (ISO dates),
`page=0`, `size=20`, `sort=tradeDate,desc`.

`200` → paginated list of:
```json
{
  "id": 42, "ticker": "NVDA", "instrumentName": "NVIDIA Corp",
  "side": "BUY", "quantity": 15, "price": 142.00, "fees": 0,
  "tradeDate": "2026-07-21", "note": null, "source": "AI_ASSISTED",
  "createdAt": "2026-07-27T14:03:00Z"
}
```

### POST /transactions
```json
// request
{ "ticker": "NVDA", "side": "BUY", "quantity": 15, "price": 142.00,
  "fees": 0, "tradeDate": "2026-07-21", "note": "optional", "source": "MANUAL" }
```
`201` → the created transaction (shape above). Unknown tickers are resolved
via instrument lookup (Twelve Data symbol search) and rejected with 400 if
not found. **v1 is USD-only:** instruments with a non-USD currency are
rejected with 400 (`details[].issue = "only USD instruments supported in v1"`).

**Timeline validation (applies to POST, PUT, and DELETE):** after any write,
the instrument's full transaction timeline for the user is re-validated in
trade-date order; if the position would go negative at any point — including
a *later* SELL broken by editing or deleting an earlier BUY — the write is
rejected with 400
(`details[].issue = "sell of 10 NVDA on 2026-07-25 would exceed position (5) after this change"`).

### GET /transactions/{id} → 200 | 404
### PUT /transactions/{id}
Same body as POST; full replace. `200` | 400 (validation / timeline) | 404.
### DELETE /transactions/{id} → 204 | 400 (timeline) | 404

---

## 3. Portfolio

### GET /portfolio/summary
```json
{
  "totalValue": 15234.50,
  "totalCostBasis": 12000.00,
  "unrealizedPnl": 3234.50,
  "unrealizedPnlPct": 26.95,
  "realizedPnl": 410.20,
  "dayChange": -120.40,
  "dayChangePct": -0.78,
  "currency": "USD",
  "pricesAsOf": "2026-07-27T13:45:00Z",
  "stale": false
}
```

`currency` is always `USD` in v1 (non-USD instruments are rejected at
transaction creation). `dayChange`/`dayChangePct` are computed only over
holdings whose cached quote has a `previousClose`; holdings without one are
excluded from the day-change figures (and report `dayChangePct: null`
individually), never treated as zero-change.

### GET /portfolio/holdings
`200` → array of:
```json
{
  "ticker": "NVDA", "name": "NVIDIA Corp", "type": "STOCK",
  "quantity": 15, "avgCost": 142.00, "costBasis": 2130.00,
  "currentPrice": 181.10, "marketValue": 2716.50,
  "unrealizedPnl": 586.50, "unrealizedPnlPct": 27.53,
  "dayChangePct": 1.2, "weightPct": 17.8,
  "priceAsOf": "2026-07-27T13:45:00Z", "stale": false
}
```

### GET /portfolio/holdings/{ticker}
Single holding (shape above) plus its transaction list. 404 if no position.

### GET /portfolio/performance?range=1M|3M|6M|1Y|ALL
```json
{
  "range": "1M",
  "from": "2026-06-27", "to": "2026-07-27",
  "investedAmount": 3500.00,
  "proceedsAmount": 1200.00,
  "netInvested": 2300.00,
  "realizedPnl": 150.75,
  "buyCount": 4, "sellCount": 1,
  "currency": "USD"
}
```
`investedAmount` = sum of BUY cost (incl. fees) in range; `proceedsAmount` =
sum of SELL proceeds (net of fees). Value-over-time series is a later
enhancement (needs price history).

---

## 4. Market data (cache-backed)

### GET /market/quote/{ticker}
`200` → `{ "ticker": "NVDA", "price": 181.10, "previousClose": 179.00, "asOf": "...", "stale": false }`
Served from `price_cache`; triggers async refresh if stale. 404 unknown ticker.

### GET /market/search?query=nvid
`200` → `[{ "ticker": "NVDA", "name": "NVIDIA Corp", "exchange": "NASDAQ", "type": "STOCK", "currency": "USD" }]`
Backed by Twelve Data symbol search with in-process caching.

---

## 5. AI

### POST /ai/parse-transaction  (Feature 1 — never saves)
```json
// request
{ "text": "Bought 15 Nvidia at 142 last Tuesday" }
// 200 response
{
  "draft": {
    "ticker": "NVDA", "side": "BUY", "quantity": 15,
    "price": 142.00, "tradeDate": "2026-07-21"
  },
  "confidence": "HIGH",
  "warnings": ["Resolved 'Nvidia' to NVDA", "Resolved 'last Tuesday' to 2026-07-21"]
}
```
Errors: 422 `AI_PARSE_FAILED` (unintelligible input / LLM returned invalid
JSON after retry), 503 `AI_UNAVAILABLE`. The frontend pre-fills the manual
form with `draft`; the user confirms via normal `POST /transactions`.

### POST /ai/chat  (Feature 2 — read-only agent)
```json
// request
{ "conversationId": 7, "message": "Which holding has lost me the most?" }
// 200 response
{
  "conversationId": 7,
  "reply": "Your biggest loser is PYPL: down $412.30 (-18.2%) on a cost basis of $2,265.",
  "toolCalls": [ { "name": "get_holdings", "durationMs": 12 } ],
  "draftTransaction": null
}
```
Omit `conversationId` to start a new conversation (id returned).
`draftTransaction` is non-null only when the agent used `draft_transaction`;
the UI then opens the pre-filled form — the agent itself never writes.
Errors: 404 conversation not found (or not yours), 503 `AI_UNAVAILABLE`.

### GET /ai/conversations → paginated `{ id, title, updatedAt }`
### GET /ai/conversations/{id}/messages → ordered `{ role, content, createdAt }`
### DELETE /ai/conversations/{id} → 204

---

## 6. External API (instructor, `X-API-Key`, read-only)

### GET /external/health
`200` → `{ "status": "UP", "version": "1.0.0", "time": "..." }` (no key required)

### GET /external/stats
```json
{ "userCount": 4, "transactionCount": 182, "instrumentCount": 23,
  "aiParsedTransactionCount": 57, "conversationCount": 12,
  "lastPriceRefreshAt": "2026-07-27T13:45:00Z" }
```
`aiParsedTransactionCount` counts rows with `source=AI_ASSISTED`; the flag is
set by the frontend when a saved transaction originated from the NL parser,
so it is advisory (audit/demo stat), not a server-verified fact.

### GET /external/users
`200` → `[{ "id": 1, "displayName": "Jasper", "transactionCount": 45 }]`
(no emails exposed)

### GET /external/users/{id}/portfolio
Portfolio summary + holdings for a given user (same shapes as §3) — lets
instructors verify computed values during the demo. All endpoints under
`/external` are GET-only; keys with `scope=READ_ONLY` can reach nothing else.
Errors: 401 missing/invalid/revoked key.

---

## Versioning & conventions

- Path-versioned (`/api/v1`); breaking changes go to `/api/v2`.
- Dates are ISO-8601; timestamps UTC with `Z`.
- Monetary values are JSON numbers in the instrument currency (USD default).
- CORS: allow the Vite dev origin in dev profile only.
