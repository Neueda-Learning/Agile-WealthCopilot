# Project Requirements & Specification
## Personal Investment Tracker with AI Portfolio Agent

**Status:** Draft for system design

---

## 1. Project Summary

A personal investment tracking web application. Users record buy/sell transactions for stocks and ETFs, and the system calculates live portfolio value, unrealized P&L, and performance using an external market-data API. Layered on top is an AI agent that lets users query and manage their portfolio in natural language, backed by tool-calling against the app's own service layer.

**Working name:** WealthCopilot

**Tech stack constraints:**
- Backend: **Java** (Spring Boot)
- Frontend: React
- Database: MySQL
- AI: DeepSeek V4 Pro

---

## 2. Base Requirements

- Users record purchases and sales of investments.
- System calculates current portfolio value, profit/loss, and performance using live market data.
- Consumer-grade UI/UX.
- Strict user authentication; individual user data segregated **at the database level** (every query scoped to the authenticated user).
- market-data APIs: Twelve Data
-  expose an API to external users as well (instructors could use this API to check some information from the system during the final presentation).

## AI Features
### AI Feature 1 — Natural-language transaction entry (build first)

- User types e.g. "Bought 15 Nvidia at 142 last Tuesday."
- Backend sends this to Claude with a prompt instructing it to extract a structured transaction (ticker, quantity, price, date) as JSON.
- Parsed result is shown to the user in the existing manual-entry form, pre-filled, for confirmation/correction before saving.
- No auto-save without user confirmation.
- Lower risk, small scope — validates the AI plumbing (API call, JSON parsing, error handling) before building the larger agent.

### AI Feature 2 — Conversational portfolio agent (headline feature)

- Chat interface where the user asks questions in plain English: "What's my total unrealized gain?", "Which holding has lost me the most?", "How much did I invest last month?" and other possible interactions

### Explicitly out of scope

- Price prediction / forecasting of any kind.
- Investment advice or recommendations ("should I buy X") — do not build anything that outputs a buy/sell recommendation framed as advice.
- Any agent tool that mutates data without a human confirmation step.

## Non-Functional Requirements

- Per-user data isolation enforced at the database query level (not just at the UI).
- Price data cached; external API called on a schedule or on-demand with caching, never per-request in a hot path.
- Version control: GitHub, single repo, instructors added as viewers.
- Project management: Jira board tracking tasks.
- Testing: unit tests minimum on P&L calculation and any agent tool logic; end-to-end tests if time allows.
