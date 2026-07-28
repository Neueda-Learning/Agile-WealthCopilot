/* Formatting rules come from the design system's content guidelines:
   currency with two decimals and separators, percentages with two decimals and
   an explicit sign, dates as "Jul 26, 2026". Numbers are never shown raw. */

const money0 = new Intl.NumberFormat('en-US', {
  style: 'currency', currency: 'USD',
  minimumFractionDigits: 0, maximumFractionDigits: 0,
});
const money2 = new Intl.NumberFormat('en-US', {
  style: 'currency', currency: 'USD',
  minimumFractionDigits: 2, maximumFractionDigits: 2,
});

export function money(n: number | null | undefined, digits: 0 | 2 = 2): string {
  if (n == null || Number.isNaN(n)) return '—';
  return digits === 0 ? money0.format(n) : money2.format(n);
}

/** Always signed, always two decimals. `null` renders as an em dash, never 0.00%. */
export function percent(n: number | null | undefined): string {
  if (n == null || Number.isNaN(n)) return '—';
  const sign = n > 0 ? '+' : n < 0 ? '-' : '';
  return `${sign}${Math.abs(n).toFixed(2)}%`;
}

export function quantity(n: number | null | undefined): string {
  if (n == null || Number.isNaN(n)) return '—';
  return new Intl.NumberFormat('en-US', { maximumFractionDigits: 4 }).format(n);
}

/** "2026-07-26" → "Jul 26, 2026". Date-only strings are parsed as UTC. */
export function tradeDate(iso: string | null | undefined): string {
  if (!iso) return '—';
  const d = new Date(iso.length === 10 ? `${iso}T00:00:00Z` : iso);
  if (Number.isNaN(d.getTime())) return iso;
  return d.toLocaleDateString('en-US', {
    month: 'short', day: '2-digit', year: 'numeric', timeZone: 'UTC',
  });
}

/** Freshness stamp for the price banner — a figure with no timestamp is unfinished. */
export function asOf(iso: string | null | undefined): string {
  if (!iso) return 'unknown';
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  return d.toLocaleString('en-US', {
    month: 'short', day: 'numeric', year: 'numeric',
    hour: 'numeric', minute: '2-digit',
  });
}

/** Today in the ISO date form the API expects for tradeDate. */
export function todayIso(): string {
  return new Date().toISOString().slice(0, 10);
}
