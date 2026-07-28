# WealthCopilot — frontend

React 18 + TypeScript + Vite. Built against [docs/api-spec.md](../docs/api-spec.md)
and the design system in [src/design-system/](src/design-system/).

```bash
npm install
npm run dev      # http://localhost:5173
npm run build    # tsc --noEmit && vite build
```

`/api` is proxied to `http://localhost:8080` in dev, so the browser stays
same-origin and the backend's CORS allowance is a fallback rather than a
dependency. Point it elsewhere with `VITE_API_TARGET` in `.env.local`.

## Layout

| Path | What |
| --- | --- |
| `src/api/client.ts` | fetch wrapper: JWT header, `ApiError` carrying the spec's error body, global 401 handling |
| `src/api/endpoints.ts` | one function per documented endpoint |
| `src/types/api.ts` | wire types, field-for-field with the spec |
| `src/context/AuthContext.tsx` | session state; a stored token is only trusted after `/auth/me` confirms it |
| `src/lib/format.ts` | money / percent / date formatting per the design guidelines |
| `src/lib/portfolio.ts` | derived figures the API doesn't return directly |
| `src/design-system/` | synced from Claude Design — don't edit, re-sync |
| `src/pages/` | one file per screen |

## Screens

Dashboard, Holdings, Transactions (with edit + delete), Log transaction
(AI parse → confirm → save), Ask Copilot, plus Login and Register.

## Conventions worth keeping

- **Numbers are never raw.** Everything monetary goes through `lib/format.ts`;
  `DeltaValue` renders signed change with directional colour.
- **`dayChangePct: null` renders as an em dash, never 0.00%.** The spec excludes
  holdings without a `previousClose` from day-change figures, so showing them as
  flat would be a lie. Same rule anywhere else the API returns null.
- **The AI never writes.** `/ai/parse-transaction` returns a draft that pre-fills
  the form; the save is a normal `POST /transactions` the user triggers.
  `draftTransaction` from `/ai/chat` opens the same form. Don't add a path that
  saves without a confirm.
- **AI disclosure copy is functional, not fine print** — the read-only banner and
  the composer hint state what Copilot can and cannot do. Keep them visible.
- **Errors say what to do.** `ApiError.fieldIssues()` maps `details[]` onto form
  fields; issues without a `field` (timeline conflicts) render above the form.

## Not built yet

- Holding detail (`GET /portfolio/holdings/{ticker}`) — no screen consumes it.
- Conversation history (`GET /ai/conversations`) — chat is single-session; the
  endpoints are wired in `api/endpoints.ts` but nothing lists them.
- Value-over-time chart — the spec marks the series as a later enhancement, so
  `Sparkline` is currently unused on the dashboard.
- No tests. E2E is Tony's WEAL-12 and is the stretch item.
