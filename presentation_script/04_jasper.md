# Jasper — Presentation Script
**Role:** Frontend & AI Features  
**Slides:** 10 Consumer Experience · 11 AI Transaction Draft · 12 Portfolio Agent & Guardrails · (Demo step 03)  
**Total mic time:** ≈ 3 min 35 sec

---

## Slide 10 · Consumer Experience — *The UI explains the portfolio at a glance.* (0:35)

> **[Advance to slide 10. You are receiving the handoff from Tony.]**

"Tony described the reliability of the backend.
My job is to show what a user actually sees.

The frontend is a **React 18 and TypeScript** single-page app.
Five routes cover the full user journey: Dashboard, Holdings, Transactions, the transaction log form, and Ask Copilot.

The **Dashboard** gives you three things immediately:
total portfolio value, your cost basis, and your unrealized P&L.
You do not need to know how average cost works — the numbers are presented so you can verify them yourself.

There is also an **allocation bar** showing how your value is distributed across symbols, and a live positions table.

Every data fetch has a visible state — loading, loaded with a fresh price, or loaded with a stale price and a timestamp.
Empty states and API errors have explicit messages.
Nothing silently shows zero or blank when data is unavailable.

The UI is honest about what it knows and when it knew it."

---

## Slide 11 · AI Feature 1 — *AI accelerates entry—but never bypasses confirmation.* (0:45)

> **[Advance to slide 11.]**

"The first AI feature is transaction entry by natural language.

The flow has exactly three steps, and the safety guarantee is in the middle step.

**Describe** — the user types something like: 'Bought 3 NVDA at 135 yesterday.'
That sentence is sent to a parse endpoint on the backend.

**Review** — the LLM extracts a structured draft: ticker NVDA, side BUY, quantity 3, price 135, date yesterday resolved to a calendar date.
That draft is displayed to the user as a form they can correct before doing anything.
The parse endpoint **never writes to the database**. It only returns structured JSON.

**Confirm** — only when the user explicitly clicks Confirm does the frontend call the normal transaction write endpoint.
The save path is identical to the manual form — same validation, same service, same database write.

Two safety properties:
If the LLM returns malformed JSON, the UI falls back to the manual form with no data loss.
If the provider is unavailable, the user never sees an error blocking the workflow — the manual form is always the fallback."

---

## Slide 12 · Portfolio Agent & Guardrails — *The agent can explain and prepare—never decide.* (1:00)

> **[Advance to slide 12.]**

"The second AI feature is the Portfolio Copilot — a conversational agent backed by an LLM with tool-calling.

The agent has eight capabilities today:
it can fetch your **portfolio summary**, your **holdings list**, your **transaction history**, a **cached price quote** for a symbol, your **total invested amount**, and the latest **stock news** for a ticker.
It can also **create a transaction draft** and **update a draft** — but those drafts still require user confirmation to save.

What the agent **cannot** do is equally important:

There is no write tool in the registry.
The agent cannot call the transaction save endpoint directly.
It can only produce a draft that the user must confirm.

Every tool call is scoped to the authenticated user — the agent cannot read another user's portfolio even if prompted to try.

The system prompt explicitly prohibits price predictions and investment advice.
But notice: we do not rely solely on the prompt.
The **structural guardrail** is the tool registry itself.
If a capability is not in the registry, no prompt injection can add it.

Conversations are persisted per user in `conversations` and `chat_messages` tables.
Chat history does not bleed between accounts.

The tool loop has an iteration limit, and the full tool call disclosure is returned to the frontend alongside the final response — so the user can see exactly which data sources the agent consulted."

> **[Hand off to Demo — you take step 03.]**

"Let me show you both AI features live."

---

## Demo Step 03 · AI Parse → Confirm → Copilot Question (1:15)

> **[During the live demo, slide 13. You have the keyboard or browser.]**

"I will type a sentence into the AI transaction entry field:
'Bought 3 NVDA at 135 yesterday.'

[Type the sentence and submit.]

The parse endpoint returns a structured draft — NVDA, BUY, 3 shares, $135, and yesterday's date.
I can correct any field here.
I will leave it as-is and click Confirm.

[Click Confirm.]

The transaction is saved through the normal write path.
If I now check Holdings, NVDA appears in the list.

Now I will switch to Ask Copilot.

[Navigate to Ask Copilot.]

I will ask one specific question: 'What is my total unrealized gain?'

[Type and submit the question.]

The agent calls the portfolio summary tool — you can see the tool disclosure in the response — and returns the calculated unrealized gain.
The number matches exactly what the Dashboard shows.

Same data, same service, different interface.

Tony will now close the demo."

---

## Q&A Guidance

**If asked about the AI provider:**
- The backend uses a DeepSeek-compatible endpoint. The provider is abstracted behind a configuration interface — changing the base URL and model name is sufficient to switch vendors.

**If asked about what happens if the LLM hallucinates a ticker or date:**
- The structured draft is shown to the user before any save. The ticker is validated against the symbol resolution service when the user confirms — an invalid symbol is rejected at the service layer with a clear error.

**If asked about prompt injection in the Copilot:**
- The tool registry is the primary defence. The agent does not have access to tools that write data, so even a successful prompt injection cannot persist anything.
- The system prompt also scopes the agent to the authenticated user's context and prohibits financial advice.

**If asked about conversation history:**
- Each conversation is linked to a `userId`. The frontend sends a `conversationId` header; if none exists, a new conversation is created for that user.
- There is no cross-user conversation access in the repository layer.

**If asked about the Vite / TypeScript build:**
- The frontend uses Vite for development and build. TypeScript strict mode is enabled.
- API calls go through a typed `client.ts` that attaches the JWT from `localStorage` on every request and handles 401 responses by redirecting to the login page.
