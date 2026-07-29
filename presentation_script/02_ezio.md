# Ezio — Presentation Script
**Role:** Transactions & Portfolio Engine  
**Slides:** 04 Solution Journey · 05 Deterministic Core · 06 Core Advantages · (Demo step 02)  
**Total mic time:** ≈ 3 min 35 sec

---

## Slide 04 · How We Solve It — *One product. Three jobs to be done.* (0:55)

> **[Advance to slide 04. You are receiving the handoff from Simon.]**

"Simon described three fragmented problems.
Our solution collapses them into one linear user journey: **Record, Understand, Ask**.

First, a user **records** validated transactions — every BUY and SELL with quantity, price, and date.

Second, the system lets them **understand** their portfolio — holdings, cost basis, current value, and both realized and unrealized P&L, all derived from those transactions.

Third, they can **ask** questions in natural language and get answers backed by the exact same data.

The critical design decision here is that all three paths share one service layer.
Whether you use the web form, the AI transaction parser, or the Copilot chat, the same `TransactionService` and `PortfolioService` run the numbers.
There is no alternative calculation path that could produce a different answer."

---

## Slide 05 · The Deterministic Core — *Every portfolio number starts with a validated timeline.* (1:20)

> **[Advance to slide 05.]**

"Let me go one level deeper into how the numbers stay correct.

Every transaction passes **three validation rules** before it is persisted:
quantities must be positive,
amounts are USD-only in this version,
and a SELL cannot exceed the shares you actually held on that date.

That last rule is the important one — it is enforced against the full history, not just the current total.

From validated transactions, the **average-cost engine** replays the timeline to produce holdings.
The formula is straightforward: a BUY increases your quantity and updates your running average cost.
A SELL reduces your quantity and books realized P&L at the average cost at the time of sale.

The key behaviour on **edit or delete**: if you change an earlier transaction, the engine replays the entire timeline from that point forward and re-checks every subsequent SELL.
You cannot introduce an inconsistency by editing history — the validation catches it.

The output — holdings, cost basis, realized P&L, unrealized P&L — flows to the dashboard, the holdings table, and the AI layer from a single `PortfolioMath` class.
It is pure calculation with no side effects, which also makes it straightforward to unit test."

---

## Slide 06 · Core Advantages — *Correct first. Fast second. AI third.* (0:35)

> **[Advance to slide 06.]**

"I want to summarise the design philosophy in one sentence:
**Correct first. Fast second. AI third.**

Your portfolio is useful even when the LLM is down and even when the market data provider is timing out.
The numbers exist because they are computed from your transactions — not from an API call.

Three properties follow from this:

**One source of truth.** Every channel reuses the same service-layer rules — no silent divergence between what the UI shows and what the AI describes.

**Safe correction.** You can edit or delete a past trade and the system will tell you if that change would make a later sale invalid, rather than silently producing wrong numbers.

**Honest trade-offs.** We chose average cost over FIFO tax lots, and USD-only over multi-currency, because half-solving those problems in v1 would create a less coherent product, not a better one."

> **[Hand off to Tony.]**

"Tony will now explain how the platform around this engine is built to be resilient."

---

## Demo Step 02 · Record a BUY → Holdings → P&L (0:50)

> **[During the live demo, slide 13. You have the keyboard or screen.]**

"I am going to record a BUY transaction.
I will enter: AAPL, BUY, 10 shares, at $150, trade date today.

[Submit the form.]

The transaction is saved.
Now I will switch to the Holdings tab —

[Navigate to Holdings.]

You can see the position: 10 shares of AAPL, average cost $150.00, current value based on the cached price.
The cost basis is $1,500. The unrealized P&L is the difference between that and the current value.

Now let me switch to the Dashboard —

[Navigate to Dashboard.]

Total portfolio value, allocation breakdown, and P&L are all updated immediately.
Every number here traces back to that one BUY transaction.
No external call was needed to compute the cost basis — that is purely deterministic."

---

## Q&A Guidance

**If asked about the average cost formula:**
- New average cost after a BUY = (old quantity × old avg cost + new quantity × new price) ÷ (old quantity + new quantity).
- On a SELL, realized P&L = (sell price − avg cost at time of sale) × quantity sold. The remaining position retains the same average cost.

**If asked about multi-currency or FIFO:**
- USD-only is a v1 scope decision. Adding multi-currency requires FX rate history and complicates every calculation.
- FIFO tax lots are the natural v2 evolution — the data model already stores individual transactions so the replay is feasible.

**If asked about portfolio caching (Caffeine):**
- `portfolioSummary` and `portfolioHoldings` are cached for 60 seconds with Caffeine.
- Any write to a transaction evicts those caches immediately, so you never see stale portfolio math after a save.

**If asked what happens if a SELL edit would exceed holdings:**
- The service re-validates the entire timeline. If the edit causes an inconsistency — e.g., a sale that now exceeds remaining shares — the operation is rejected with a clear error message before any data is written.
