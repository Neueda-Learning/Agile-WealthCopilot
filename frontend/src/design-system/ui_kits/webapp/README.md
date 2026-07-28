# WealthCopilot web app — UI kit (reference)

Copied from the Claude Design project. This is the designed recreation of the product —
**reference material for building the real app, not the real app itself.**

## Screens

| File | View | What it shows |
| --- | --- | --- |
| `AppShell.jsx` | Chrome | 236px `SidebarNav` (wordmark, destinations, account footer) + 60px `TopBar` + scrolling content column capped at `--content-max` |
| `DashboardScreen.jsx` | Dashboard | Delayed-price banner, hero portfolio value + filled `Sparkline` with range `SegmentedControl`, total-return and allocation cards, top-movers table, Copilot prompt launcher |
| `HoldingsScreen.jsx` | Holdings / Transactions | `Tabs`, search + account filter, full holdings table (price, market value, total P&L, today), transactions table with Buy/Sell and AI-parsed badges, empty state when filters match nothing |
| `LogTransactionScreen.jsx` | Log transaction | Natural-language `ChatComposer` → `ParsedTransactionCard` confirm gate → editable pre-filled form → saved confirmation banner |
| `CopilotScreen.jsx` | Ask Copilot | Read-only disclosure banner, `ChatMessage` turns with tool-call disclosure, suggestion chips, composer |
| `data.js` | — | Fake portfolio: 8 holdings, 5 transactions, derived totals, price series, currency formatter |

## These files do not run as-is

They were written for the Claude Design browser harness, not for a bundler:

- They read components off a global (`window.WealthCopilotDesignSystem_f10604`) instead of importing.
- They publish themselves with `Object.assign(window, { … })` instead of exporting.
- `index.html` loads React, ReactDOM and Babel from a CDN and expects `../../_ds_bundle.js` — the
  compiled bundle, which was **deliberately not copied** into the repo (it is build output, and the
  component sources next to it are the real thing).

To click through the working prototype, open the project at
https://claude.ai/design — it renders there.

## Turning these into real pages

The components in `../../components/` are already plain ES modules, so porting a screen is
mechanical:

1. Replace the `const { … } = window.WealthCopilotDesignSystem_f10604;` line with
   `import { … } from '../../design-system';`
2. Add `import React, { useState } from 'react';` and drop the `React.` prefixes if you prefer.
3. Replace `Object.assign(window, { X });` with `export default X;`
4. Swap `window.WC_DATA` for the real API client.

`data.js` is fixture data shaped like the API responses these screens expect — useful for building
screens before the backend endpoints land (WEAL-6, WEAL-7).

## Interactions the prototype demonstrates

- Sidebar navigation between all four views (Transactions deep-links to the Holdings tab).
- Dashboard prompt chips seed a real conversation on the Copilot screen.
- Holdings search + account filter, with the empty state.
- Type or pick a sentence on Log transaction → **Parse** → **Confirm & save** (or **Edit** to open the
  pre-filled form) → success banner. Nothing is ever saved without a confirm.
- Copilot answers three canned questions with real numbers derived from `data.js`; anything else
  gets the portfolio-summary fallback.

## Deliberately not designed

No login, settings, holding-detail page, or account-linking flow — no source material described them.
Note that the app **does** need login (WEAL-3), so those screens still have to be designed or built.
