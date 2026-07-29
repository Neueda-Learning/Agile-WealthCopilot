import { useState } from 'react';
import { money, percent, signedMoney } from '../../lib/format';
import type { Holding } from '../../types/api';
import ChartLegend from './ChartLegend';

interface Props {
  holdings: Holding[];
  /** Opens that holding's transaction history. */
  onSelect?: (ticker: string) => void;
}

/**
 * Unrealized P&L per holding as a diverging bar chart: winners run right from
 * the zero line, losers run left.
 *
 * Polarity is carried by direction and by the signed value on every row, not
 * by hue alone — the gain/loss pair sits at ΔE 6.6 under deuteranopia, which
 * is inside the band that requires a second encoding channel.
 */
export default function PnlContributionChart({ holdings, onSelect }: Props) {
  const [hover, setHover] = useState<string | null>(null);
  const [asTable, setAsTable] = useState(false);

  const rows = [...holdings].sort((a, b) => b.unrealizedPnl - a.unrealizedPnl);

  if (rows.length === 0) {
    return <p className="dv-empty">No open positions to compare.</p>;
  }

  // One scale across both arms, so a $500 loss is exactly as long as a $500 gain.
  const maxGain = Math.max(0, ...rows.map((r) => r.unrealizedPnl));
  const maxLoss = Math.max(0, ...rows.map((r) => -r.unrealizedPnl));
  const span = maxGain + maxLoss;

  if (span === 0) {
    return <p className="dv-empty">Every position is exactly at its cost basis.</p>;
  }

  const zero = maxLoss / span; // 0..1 across the plot column

  if (asTable) {
    return (
      <>
        <TableToggle asTable={asTable} onToggle={setAsTable} />
        <table className="dv-table">
          <thead>
            <tr><th>Position</th><th>Cost basis</th><th>Market value</th><th>Unrealized P&amp;L</th><th>Return</th></tr>
          </thead>
          <tbody>
            {rows.map((r) => (
              <tr key={r.ticker}>
                <th scope="row">{r.ticker}</th>
                <td>{money(r.costBasis)}</td>
                <td>{money(r.marketValue)}</td>
                <td>{signedMoney(r.unrealizedPnl)}</td>
                <td>{percent(r.unrealizedPnlPct)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </>
    );
  }

  return (
    <>
      <div className="dv-head">
        <ChartLegend items={[
          { label: 'Gain', color: 'var(--gain-600)' },
          { label: 'Loss', color: 'var(--loss-600)' },
        ]} />
        <TableToggle asTable={asTable} onToggle={setAsTable} />
      </div>

      <div className="dv-body">
        {rows.map((r) => {
          const gain = r.unrealizedPnl >= 0;
          const frac = Math.abs(r.unrealizedPnl) / span;
          const left = gain ? zero : zero - frac;
          const active = hover === r.ticker;

          return (
            <div
              key={r.ticker}
              className={'dv-row' + (active ? ' is-active' : '')}
              role={onSelect ? 'button' : undefined}
              tabIndex={onSelect ? 0 : undefined}
              onClick={() => onSelect?.(r.ticker)}
              onKeyDown={(e) => {
                if (onSelect && (e.key === 'Enter' || e.key === ' ')) {
                  e.preventDefault();
                  onSelect(r.ticker);
                }
              }}
              onMouseEnter={() => setHover(r.ticker)}
              onMouseLeave={() => setHover(null)}
              onFocus={() => setHover(r.ticker)}
              onBlur={() => setHover(null)}
              aria-label={
                `${r.ticker}, ${gain ? 'gain' : 'loss'} of ${money(Math.abs(r.unrealizedPnl))}, `
                + `${percent(r.unrealizedPnlPct)} on a cost basis of ${money(r.costBasis)}`
              }
            >
              <span className="dv-row__key">{r.ticker}</span>
              <span className="dv-row__plot">
                {/* Drawn per row rather than once over the body: the rows are
                    contiguous, so the segments read as one line, and it stays
                    aligned with the bars regardless of grid gaps. */}
                <span className="dv-row__zero" style={{ left: `${zero * 100}%` }} aria-hidden="true" />
                <span
                  className="dv-row__bar"
                  style={{
                    left: `${left * 100}%`,
                    width: `${frac * 100}%`,
                    background: gain ? 'var(--gain-600)' : 'var(--loss-600)',
                    borderRadius: gain ? '0 4px 4px 0' : '4px 0 0 4px',
                  }}
                />
              </span>
              <span className={'dv-row__val ' + (gain ? 'is-gain' : 'is-loss')}>
                {signedMoney(r.unrealizedPnl)}
              </span>

              {active && (
                <span className="dv-tip" role="status">
                  <strong>{r.ticker}</strong>{r.name && r.name !== r.ticker ? ` ${r.name}` : ''}
                  <span className="dv-tip__row">
                    <span>Unrealized</span><span>{signedMoney(r.unrealizedPnl)} ({percent(r.unrealizedPnlPct)})</span>
                  </span>
                  <span className="dv-tip__row">
                    <span>Cost basis</span><span>{money(r.costBasis)}</span>
                  </span>
                  <span className="dv-tip__row">
                    <span>Market value</span><span>{money(r.marketValue)}</span>
                  </span>
                  {onSelect && <span className="dv-tip__hint">Click to see its transactions</span>}
                </span>
              )}
            </div>
          );
        })}
      </div>
    </>
  );
}

export function TableToggle({ asTable, onToggle }: { asTable: boolean; onToggle: (v: boolean) => void }) {
  return (
    <button type="button" className="dv-toggle" onClick={() => onToggle(!asTable)}>
      {asTable ? 'Show chart' : 'Show table'}
    </button>
  );
}
