import type { Holding } from '../types/api';

/**
 * The day's dollar move for a holding.
 *
 * `GET /portfolio/holdings` returns `dayChangePct` but no dollar amount, and
 * the percentage is measured against the previous close — so the move is
 * derived from the current market value rather than by multiplying it
 * directly (marketValue already includes today's move).
 *
 * Returns null when the holding has no previousClose, which the spec says must
 * never be shown as a zero change.
 */
export function dayChangeAmount(h: Holding): number | null {
  if (h.dayChangePct == null) return null;
  const previousValue = h.marketValue / (1 + h.dayChangePct / 100);
  return h.marketValue - previousValue;
}
