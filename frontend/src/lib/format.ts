/* Formatting rules come from the design system's content guidelines:
   currency with two decimals and separators, percentages with two decimals and
   an explicit sign, dates as "Jul 26, 2026". Numbers are never shown raw. */

function displayLocale(): string {
  return document.documentElement.lang === 'zh-CN' ? 'zh-CN' : 'en-US';
}

export function money(n: number | null | undefined, digits: 0 | 2 = 2): string {
  if (n == null || Number.isNaN(n)) return '—';
  return new Intl.NumberFormat(displayLocale(), {
    style: 'currency', currency: 'USD',
    minimumFractionDigits: digits, maximumFractionDigits: digits,
  }).format(n);
}

/**
 * Money with an explicit leading sign. Charts encode direction with hue, and
 * the gain/loss pair is close to indistinguishable under deuteranopia, so the
 * sign has to be visible in the text rather than implied by color.
 */
export function signedMoney(n: number | null | undefined, digits: 0 | 2 = 2): string {
  if (n == null || Number.isNaN(n)) return '—';
  if (n === 0) return money(0, digits);
  return `${n > 0 ? '+' : '-'}${money(Math.abs(n), digits)}`;
}

/** Always signed, always two decimals. `null` renders as an em dash, never 0.00%. */
export function percent(n: number | null | undefined): string {
  if (n == null || Number.isNaN(n)) return '—';
  const sign = n > 0 ? '+' : n < 0 ? '-' : '';
  return `${sign}${Math.abs(n).toFixed(2)}%`;
}

export function quantity(n: number | null | undefined): string {
  if (n == null || Number.isNaN(n)) return '—';
  return new Intl.NumberFormat(displayLocale(), { maximumFractionDigits: 4 }).format(n);
}

/** "2026-07-26" → "Jul 26, 2026". Date-only strings are parsed as UTC. */
export function tradeDate(iso: string | null | undefined): string {
  if (!iso) return '—';
  const d = new Date(iso.length === 10 ? `${iso}T00:00:00Z` : iso);
  if (Number.isNaN(d.getTime())) return iso;
  return d.toLocaleDateString(displayLocale(), {
    month: 'short', day: '2-digit', year: 'numeric', timeZone: 'UTC',
  });
}

/** Freshness stamp for the price banner — a figure with no timestamp is unfinished. */
export function asOf(iso: string | null | undefined): string {
  if (!iso) return document.documentElement.lang === 'zh-CN' ? '未知' : 'unknown';
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  return d.toLocaleString(displayLocale(), {
    month: 'short', day: 'numeric', year: 'numeric',
    hour: 'numeric', minute: '2-digit',
  });
}

/** Today in the ISO date form the API expects for tradeDate. */
export function todayIso(): string {
  const now = new Date();
  const month = String(now.getMonth() + 1).padStart(2, '0');
  const day = String(now.getDate()).padStart(2, '0');
  return `${now.getFullYear()}-${month}-${day}`;
}
