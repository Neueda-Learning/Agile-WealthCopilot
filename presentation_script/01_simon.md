# Simon — Presentation Script
**Role:** Platform, Authentication & Security  
**Slides:** 01 Product Positioning · 02 Team · 03 Project Management & CI · 04 Target Users & Needs · (Demo step 01)
**Total mic time:** ≈ 3 min 25 sec

---

## Slide 01 · Product Positioning — *WealthCopilot* (0:40)

> **[Open on the title slide. Pause one beat, then speak.]**

"WealthCopilot is not a trading platform.
It is an explainable personal investment tracker.

It records transactions, calculates portfolio performance, and helps users
understand the results.

Compared with a spreadsheet, it validates the investment history.
Compared with a brokerage app, it explains the portfolio but does not execute
trades. Compared with general AI, it uses verified portfolio records and keeps
the user in control.

In short, we record, calculate and explain.
We do not move money, predict prices or give investment advice."

---

## Slide 02 · Team — *Four people. Clear ownership.* (0:35)

> **[Advance to slide 02.]**

"Our team built the product with four clear areas of ownership.

I am Simon. I own the platform foundation, authentication and security.
Ezio owns the transaction flow and portfolio calculation engine.
Tony owns market data, the external API and testing.
Jasper owns the React frontend and both AI features.

Each person had a clear module, but all modules work together through the same
backend services."

---

## Slide 03 · Project Management & CI — *Visible work. Reviewed changes.* (0:40)

> **[Advance to slide 03.]**

"We used Jira to manage the project.
Every task had an owner and moved from To Do, to In Progress, to In Review and
then Done. This made our progress and blockers visible.

For the code, each change was made on a branch and opened as a pull request.
GitHub Actions then ran Backend CI and Frontend CI. The backend ran Maven tests
and verification. The frontend ran its type-check and production build.

CI checked the code automatically, and one teammate reviewed it before the
change was merged."

---

## Slide 04 · Target Users & Needs — *Clarity for self-directed investors.* (1:05)

> **[Advance to slide 04.]**

"Our target users are everyday, self-directed investors.
They may own a few stocks or ETFs and keep their own records. They are not
professional traders.

The problem is not a missing stock screen.
Investment information is fragmented across different tools.

Users need four things: a reliable transaction history, portfolio numbers they
can understand, plain-language answers, and control when AI is involved.

Today, these needs are split across spreadsheets, market-data pages, brokerage
apps and general AI tools. This creates manual work and makes the numbers harder
to trust."

> **[Hand off to Ezio.]**

"Ezio will now show you how WealthCopilot brings these needs into one clear
portfolio journey."

---

## Demo Step 01 · Sign In (0:25)

> **[Return to keyboard or browser during the live demo. Slide 13.]**

"I am signing in now with a registered test account.
The login call returns a short-lived JWT — you can see it in the network tab if we need to verify.

Notice that the token is signed with a secure secret and carries only the user ID as the subject claim.
From this point forward, every API call the frontend makes attaches that token in the Authorization header.
The user ID is extracted server-side from the verified token — it never comes from a request parameter.

Authentication is established. Over to Ezio."

---

## Q&A Guidance

**If asked about security architecture:**
- JWT is stateless; no session store required.
- Tokens are short-lived. Refresh is handled on the frontend via an interceptor.
- BCrypt work factor is tuned to slow brute-force without impacting normal login latency.

**If asked about per-user isolation:**
- Every `Repository` method that queries user-owned data takes `userId` as a parameter sourced from the Spring Security context — not from the HTTP request body.
- Cross-user access returns 404, not 403, to avoid confirming that a resource exists (existence privacy).

**If asked about the API-key framework:**
- There is a separate security filter chain for the external API surface, with hashed API keys stored in the database — completely independent of the JWT user chain.
