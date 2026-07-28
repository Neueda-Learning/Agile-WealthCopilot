# WealthCopilot Design System

> Copied verbatim from the Claude Design project `WealthCopilot Design System`
> (`readme.md`). Do not edit here — edit in Claude Design and re-sync, so the
> two never drift. See [README.md](README.md) for how this maps into the repo.

WealthCopilot is a personal investment tracker with an AI portfolio agent. Users log stock and ETF
buy/sell transactions; the app computes live portfolio value, cost basis, P&L and performance from
cached market data. Two AI features sit on top:

1. **Natural-language transaction parser** — the user types "bought 12 AAPL at 182.40 yesterday";
   Copilot pre-fills a transaction form and waits. It never auto-saves.
2. **Portfolio chat agent** — answers questions about the user's own holdings using read-only tools.
   It cannot trade, move money, or edit records.

**Audience:** retail investors tracking a personal portfolio — not professional traders. The product
should feel like a clean brokerage app: calm, precise, trustworthy. Not a trading terminal, not a
hype-y crypto app.

**Stack:** React 18 + TypeScript.

## Sources

**None were supplied.** No codebase, Figma file, brand guidelines, logo, font files, or decks were
attached — this system was authored from the product description above plus a stated visual
direction ("modern fintech: clean sans, bold accent colour, data-forward") and tone ("confident and
authoritative"). Everything here is therefore a **proposal**, not a recreation. Two substitutions are
flagged below and both should be replaced when real brand assets exist:

| Missing | Substitute used | Where |
| --- | --- | --- |
| Brand fonts | **Public Sans** (UI) + **IBM Plex Mono** (figures), Google Fonts | `tokens/fonts.css` |
| Icon set | **Lucide 0.446** via unpkg CDN, masked to `currentColor` | `components/core/Icon.jsx` |
| Logo / brand mark | **None drawn.** The mark is the wordmark set in type (`Wordmark`) | `components/core/Wordmark.jsx` |

No logo was provided, so none was invented. Wherever a mark belongs, `Wordmark` renders
"Wealth" in bold and "Copilot" in light with a small rotated-square accent. Replace it the moment a
real logo lands.

---

## Content fundamentals

**Voice: confident and authoritative.** WealthCopilot states facts about the user's money and then
stops. It does not cheer, hedge, or apologise.

- **Lead with the number, then the context.** "Tech is 38.2% of your portfolio — $95,086 across 5
  positions." Never "You seem to have quite a lot in tech."
- **Second person for the user, no first person for the product.** "Your largest position is VOO",
  not "I found that…" or "We think…". The product is a tool, not a companion. Copilot's chat replies
  are the one place a light "I" is tolerable, and even there prefer stating the finding directly.
- **Sentence case everywhere** — buttons, titles, nav, dialog headers. `Log transaction`, not
  `Log Transaction` or `LOG TRANSACTION`. The only uppercase in the product is the 11px eyebrow label
  (`TOTAL VALUE`) and badges (`BUY`, `DELAYED 15M`).
- **Buttons are verb phrases** naming the outcome: `Log transaction`, `Confirm & save`, `Parse`,
  `Export CSV`. Never `Submit`, `OK`, `Click here`.
- **Errors say what to do**, not what went wrong: "Enter a quantity above 0", not "Invalid input".
- **Empty states describe the consequence of filling them**: "Add a buy and WealthCopilot tracks
  value and P&L from that date."
- **No emoji. Ever.** Not in UI, not in Copilot replies, not in marketing copy. A green arrow and a
  number carry the same information with none of the tonal risk.
- **No exclamation marks, no hype adjectives** (amazing, incredible, powerful, revolutionary), no
  rocket/moon language, and no forecasts. WealthCopilot reports what happened; it does not predict.
- **AI copy is disclosure-first.** Every AI surface says what the AI did and what it cannot do:
  "Parsed from your text — review before saving", "Copilot reads your holdings. It cannot place
  trades." These lines are functional, not fine print — never hide or shrink them.
- **Data freshness is always stated.** "Prices are 15 minutes delayed. Cached at 4:02pm ET." A number
  with no timestamp is an unfinished sentence.
- **Numbers are formatted, never raw.** Currency with two decimals and thousands separators
  (`$248,913.42`), percentages with two decimals and an explicit sign (`+0.50%`), dates as
  `Jul 26, 2026`.

**Say / don't say**

| Say | Don't say |
| --- | --- |
| Tech is 38.2% of your portfolio — $95,086 across 5 positions. | Wow, you're super concentrated in tech! 🚀 |
| Review before saving. Nothing is recorded until you confirm. | We've gone ahead and added that for you! |
| Prices are cached and may be 15 minutes behind. | Live real-time market data, always up to date. |
| Enter a quantity above 0. | Invalid input. |

---

## Visual foundations

**Overall vibe:** machined, quiet, dense-but-breathing. The page is a soft cool grey; content lives
on white cards separated by hairlines. Colour is rationed — a single blue accent plus green/red that
mean *only* market direction. There is no decoration on any screen: no illustration, no gradient
backgrounds, no photography, no texture, no pattern. Everything on screen is either data, a control,
or a label.

**Colour.** Ink neutrals are cool slate (`#0b1220` stands in for black — pure black is never used).
The single brand accent is **Copilot Blue `#1f4fd8`**, used for primary buttons, active nav, focus
rings and chart series 1 — nothing else. **Green `#0e8f5e` and red `#c8372d` are reserved for the
direction of money** and are never reused for generic success/error states (that's why the "saved"
banner uses the gain tone deliberately, and validation errors use the loss tone deliberately — both
are money-adjacent). Amber `#b4700a` carries caution and low AI confidence. Six-colour categorical
chart ramp, used in fixed order, capped at six segments. No gradients anywhere except the 10%-opacity
area fill under the portfolio line. Backgrounds are flat fills only.

**Type.** Public Sans for everything human-readable; IBM Plex Mono for **every number in the
product** — tabular, lining, slashed zero, so a refreshing balance never jitters or re-flows. Display
44 / H1 32 / H2 24 / H3 18 / body 15 / small 13 / label 11-uppercase-tracked. Tracking tightens as
size grows (−0.022em at display, 0 at body). Weights: 300 display-only, 400 body, 500 figures,
600 headings and buttons, 700 wordmark only.

**Spacing & layout.** 4px grid (2px exists only for optical nudges). Card padding 20px, section gap
24px, page padding 32px. Table rows 48px, 40px compact. Control heights 30 / 38 / 46. The app is a
fixed 236px sidebar + fluid content column capped at 1240px and centred. The sidebar and 60px top bar
are fixed; only the content column scrolls. Dashboards use a 2fr/1fr two-column grid, never more than
two columns of cards.

**Corners.** Restrained: cards 12px, controls and buttons 8px, chips and badges 6px, modals 16px.
Pill radius only for switches, legend dots and avatars. Nothing is more rounded than 16px.

**Cards.** White fill, 1px `--border-subtle` hairline, 12px radius, `--shadow-xs` (barely there).
Optional 20px header with a title, a 13px grey subtitle and a right-aligned action slot, separated by
a hairline. `flush` variant removes body padding so tables meet the card edge. Cards never nest and
never carry a coloured left border.

**Elevation.** The hairline does the work; shadow only signals that a surface floats. xs = resting
card, sm = raised/menu, md = hover on an interactive card, lg = dialog. Scrim is `rgba(11,18,32,.44)`
with a 2px backdrop blur — the only place blur is used. Glass/frosted surfaces are not part of this
system.

**Borders.** 1px is the default everywhere; 1.5px only on checkbox/radio outlines and the
AI-parsed card, where the slightly heavier blue edge marks "this needs your confirmation".

**Interaction states.**
- *Hover:* one step darker for filled controls (`--blue-600` → `--blue-700`), a light grey tint for
  quiet controls and table rows, plus a border step from `--border-default` to `--border-strong`.
  Never opacity-based hover.
- *Press:* one step darker again. **No transform, no scale, no shrink** — money controls should not
  feel springy.
- *Focus:* 2px `--border-focus` outline at 2px offset; inputs additionally take the 3px blue glow
  `--shadow-focus`. Focus is never removed.
- *Disabled:* 45% opacity, `not-allowed` cursor, no other change.
- *Selected:* pale blue fill for nav items (never a left accent bar), a 2px blue underline for tabs,
  a white raised pill inside the segmented control.

**Motion.** Short and flat. 120ms for hover/press, 180ms for switches, tab underline and nav fill,
280ms `--ease-out` for a dialog rising 8px behind a fading scrim. **No bounce, no spring, no scale.**
Numbers never count up or animate — an animated figure reads as unreliable. Skeletons shimmer at
1.4s linear while cached prices refresh, and refreshes happen in place so the layout never jumps.
`prefers-reduced-motion` zeroes every duration.

**Imagery.** There is none, and that is a decision, not a gap: no stock photography, no
illustrations, no 3D renders, no company logos for tickers (a grey mono `TickerAvatar` chip stands in,
so the product never implies a relationship with a brand it doesn't have). The only "graphics" are
data: sparklines, the filled performance line, and the stacked allocation bar. Never a donut or pie
chart.

**Transparency.** Used in exactly three places: the modal scrim, the 10% chart area fill, and shadow
alpha. Content surfaces are always opaque.

---

## Iconography

**Lucide 0.446** (2px stroke, rounded caps, 24px grid), loaded per-icon from
`https://unpkg.com/lucide-static@0.446.0/icons/<name>.svg`. **This is a flagged substitution** — no
icon set was supplied, and Lucide's even stroke weight and geometric construction suit the machined
feel. Replace the CDN base in `components/core/Icon.jsx` if a real set arrives.

- Icons are rendered as a **CSS mask over `currentColor`**, so they inherit text colour exactly like
  an icon font, and there is no per-icon SVG to maintain. Always use `<Icon name="…" />`; never paste
  raw SVG into a component or screen.
- **Sizes:** 14 in dense table cells, 16 inline with body text and in buttons, 17–18 in nav and icon
  buttons, 20–24 in empty states.
- **Outline only.** No filled glyphs, no duotone, no coloured icons — an icon takes the colour of the
  text beside it and never carries meaning alone; the adjacent label does.
- **No emoji, ever**, and no unicode symbols used as icons (no ▲▼ for market direction — `DeltaValue`
  renders `arrow-up-right` / `arrow-down-right` from Lucide).
- **Working vocabulary:** `layout-dashboard`, `layers`, `receipt`, `sparkles` (the AI marker, used
  nowhere else), `wrench` (tool-call disclosure), `trending-up` / `trending-down`,
  `arrow-up-right` / `arrow-down-right`, `wallet`, `search`, `search-x`, `plus`, `plus-circle`,
  `refresh-cw`, `rotate-ccw`, `download`, `pencil`, `trash-2`, `calendar`, `clock`, `settings`,
  `info`, `alert-triangle`, `alert-octagon`, `check-circle-2`, `chevron-right`, `chevron-down`,
  `chevrons-up-down`, `x`, `user`, `paperclip`, `arrow-up`, `arrow-right`, `more-horizontal`.
- `assets/` holds no imagery because none was supplied. Do not fill it with generated art.

---

## Index

```
styles.css              # the only file consumers link — @import list, nothing else
tokens/                 # fonts, colors, typography, spacing, radius, elevation, motion, base
styles/components.css   # the wc-* class layer every component renders against
components/             # React primitives, grouped by concern
guidelines/             # 17 foundation specimen cards (Colors, Type, Spacing, Brand)
ui_kits/webapp/         # the portfolio app recreation — see its own README.md
templates/              # copyable starting folders (Portfolio dashboard)
thumbnail.html          # homepage tile
SKILL.md                # Agent Skills wrapper for use outside this project
```

### Components

Every component is `<Name>.jsx` + `<Name>.d.ts` + `<Name>.prompt.md`, with one `@dsCard` HTML per
directory. Reach them as `window.<Namespace>.<Name>`.

**`components/core/`** — `Icon`, `Button`, `IconButton`, `Badge`, `Card`, `Wordmark`
**`components/forms/`** — `Input`, `Select`, `Checkbox`, `Switch`, `SegmentedControl`
**`components/data/`** — `Stat`, `DeltaValue`, `Sparkline`, `AllocationBar`, `TickerAvatar`, `DataTable`
**`components/feedback/`** — `Banner`, `Dialog`, `EmptyState`, `Skeleton`
**`components/navigation/`** — `SidebarNav`, `Tabs`, `TopBar`
**`components/ai/`** — `ChatMessage`, `ChatComposer`, `ParsedTransactionCard`

**Intentional additions** (nothing defined an inventory, so the set was authored to the product's
needs; these are the non-obvious ones):

- `Icon` — wrapper for the Lucide glyph set, so no screen ever hand-rolls an SVG.
- `Wordmark` — stands in for the absent logo.
- `DeltaValue`, `Stat`, `Sparkline`, `AllocationBar`, `TickerAvatar` — the product is a portfolio
  tracker; signed change, KPI figures and trend lines are its actual primitives.
- `ParsedTransactionCard`, `ChatMessage`, `ChatComposer` — the two AI features are the product's
  differentiator and need first-class, disclosure-carrying components.
- No Toast, Tooltip, Avatar, Accordion or Breadcrumb: nothing in the product description calls for
  them, and disclosures that matter (freshness, AI confidence) must persist, so they are `Banner`s.

### UI kits

**`ui_kits/webapp/`** — Dashboard, Holdings/Transactions, Log transaction (AI parse → confirm), Ask
Copilot. Click-through in `index.html`.

### Templates

**`templates/portfolio-dashboard/`** — Portfolio dashboard: hero value, KPI cards, allocation and a
holdings table, wired to this system's components. Copy the folder to start a new design.
