# Simon — Presentation Script
**Role:** Platform, Authentication & Security  
**Slides:** 01 Title · 02 Team & Ownership · 03 User Problem · (Demo step 01)  
**Total mic time:** ≈ 3 min 35 sec

---

## Slide 01 · Title — *WealthCopilot* (0:25)

> **[Open on the title slide. Pause one beat, then speak.]**

"WealthCopilot is not a trading platform.
It is an explainable, verifiable personal investment recorder.

Three things make it different:
portfolio math that is deterministic and auditable,
market data that never blocks the user even when the provider is slow,
and an AI layer that always waits for human confirmation before writing anything.

In the next twelve minutes we will walk through the value story.
The final three minutes will be a live demonstration of a real user path."

---

## Slide 02 · Team & Ownership — *Four owners. One integrated delivery path.* (1:00)

> **[Advance to slide 02.]**

"Our team built this along four clear ownership boundaries.

I'm Simon — I own the **platform foundation**: the Spring Boot application, JWT authentication, BCrypt password storage, and per-user data isolation.
My work was the first to land, which let the other three modules develop in parallel against stable API and service interfaces.

Ezio owns the **domain core** — the transaction lifecycle and the portfolio engine that derives every P&L number.

Tony owns **reliability** — market data, the external API security framework, and the test infrastructure that validates every integration point.

Jasper owns the **user experience** — the React frontend and both AI features.

Every module eventually calls the same service layer.
That is intentional, and I will show you why the boundaries hold when we reach the security slide."

---

## Slide 03 · The User Problem — *Investment information is fragmented—and hard to trust.* (1:45)

> **[Advance to slide 03.]**

"The problem we are solving is not 'missing a stock screen.'
It is that investment information is fragmented across tools that do not trust each other.

**Records** — most people track trades in spreadsheets or notes.
There is no validation that a sale cannot exceed what you actually held on that date.
There is no safe edit that re-checks history.

**Prices** — live market data is slow, rate-limited, or offline.
If your portfolio view depends entirely on a real-time API call, a flaky provider takes down your entire experience.

**Questions** — users want plain-language answers about their own money.
But handing an AI agent unrestricted write access to financial records is not safe.

That gap — between what users need and what fragmented tools give them — is exactly what WealthCopilot closes.

And I want to be explicit about the **product boundary**: we record, we calculate, we explain.
We do not move money, we do not predict prices, and we do not output buy or sell advice.
That boundary is a deliberate design decision, not a missing feature."

> **[Hand off to Ezio.]**

"Ezio will now show you how we solve these three problems in one product."

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
