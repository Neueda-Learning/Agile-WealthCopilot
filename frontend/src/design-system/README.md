# WealthCopilot design system — synced from Claude Design

Source project: **WealthCopilot Design System** on https://claude.ai/design
(`f1060438-96b8-471a-94d7-93a28df67ebd`) · synced 2026-07-27.

The design tool is the source of truth. Change things there and re-sync rather than
editing these files, or the two drift apart.

## What's here

| Path | What |
| --- | --- |
| `styles.css` | The only stylesheet to import. `@import`s everything below it. |
| `tokens/` | 8 token files: fonts, colours, typography, spacing, radius, elevation, motion, base |
| `styles/components.css` | The `wc-*` class layer every component renders against |
| `components/<group>/` | 27 React components as ES modules, each with a `.d.ts` |
| `ui_kits/webapp/` | The five designed screens — reference, [not runnable as-is](ui_kits/webapp/README.md) |
| `index.js` | Barrel export (added by the sync, not from the design tool) |
| `DESIGN-GUIDELINES.md` | The full design system readme: voice, colour, type, motion, icon rules |

Components by group: **core** (Icon, Button, IconButton, Badge, Card, Wordmark) ·
**forms** (Input, Select, Checkbox, Switch, SegmentedControl) ·
**data** (Stat, DeltaValue, Sparkline, AllocationBar, TickerAvatar, DataTable) ·
**feedback** (Banner, Dialog, EmptyState, Skeleton) ·
**navigation** (SidebarNav, Tabs, TopBar) ·
**ai** (ChatMessage, ChatComposer, ParsedTransactionCard).

## Using it

Once the Vite app exists (WEAL-13), import the stylesheet once at the root and pull components
from the barrel:

```jsx
import './design-system/styles.css';
import { Card, Stat, DeltaValue } from './design-system';

<Card title="Total value">
  <Stat label="Total value" size="xl" value="$248,913.42"
        foot={<DeltaValue value={1243.18} percent={0.5} currency="USD" size={13} />} />
</Card>
```

The components are dependency-free apart from React — no CSS-in-JS, no UI library. They style
themselves with the `wc-*` classes in `styles/components.css`, so the stylesheet import is not
optional.

Three rules worth knowing before writing any screen (the rest are in
[DESIGN-GUIDELINES.md](DESIGN-GUIDELINES.md)):

- **Every number renders in tabular mono.** Use `DeltaValue` for signed change, `Stat` for figures,
  and `align: 'right'` on a `DataTable` column — that flag switches the cell to mono as well as
  right-aligning it.
- **Green and red mean market direction only.** Never reuse them for generic success/error.
- **AI surfaces state what the AI did and cannot do.** The disclosure lines on `ChatComposer` and
  `ParsedTransactionCard` are functional copy, not fine print — don't remove or shrink them.

## Not copied, and why

- **`_ds_bundle.js`** — the compiled UMD bundle. It's build output; the component sources here are
  the real thing. Its absence is why `ui_kits/webapp/index.html` won't render locally.
- **`*.card.html`, `guidelines/*.card.html`, `thumbnail.html`, `_ds_manifest.json`** — preview cards
  and metadata for the Claude Design browser UI. No use in a Vite app; view them in the design tool.
- **`templates/portfolio-dashboard/`** — a copyable starting point for *new designs* in the tool,
  not app source.
- **`*.prompt.md`** — 27 per-component usage guides written for the design agent. Useful reading,
  but they duplicate the `.d.ts` plus `DESIGN-GUIDELINES.md` for a human audience. Pull them if the
  team wants them.

## Substitutions to replace when real brand assets exist

The design system was authored without any supplied brand material, and flags three stand-ins:

| Missing | Stand-in | Where |
| --- | --- | --- |
| Brand fonts | Public Sans + IBM Plex Mono, from Google Fonts | `tokens/fonts.css` |
| Icon set | Lucide 0.446, loaded per-icon from unpkg | `components/core/Icon.jsx` |
| Logo | None drawn — the mark is the wordmark set in type | `components/core/Wordmark.jsx` |

Both the fonts and the icons load from a **CDN at runtime**. That's fine for a demo and a problem
for anything else: it costs a round trip per icon and breaks offline. Before the presentation,
consider vendoring the two font files and the ~30 icons actually used (listed in
`DESIGN-GUIDELINES.md`).

## Re-syncing

Ask Claude Code to sync the design project again. Files here are byte-for-byte copies of the
remote ones, so a re-sync is a plain overwrite — the only hand-authored files are `index.js`,
this README, and `ui_kits/webapp/README.md`.
