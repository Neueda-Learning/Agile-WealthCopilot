import { useState } from 'react';
import { money, percent, signedMoney } from '../../lib/format';
import { dayChangeAmount } from '../../lib/portfolio';
import type { Holding } from '../../types/api';
import ChartLegend from './ChartLegend';
import { TableToggle } from './PnlContributionChart';

/** Beyond this the columns get too narrow to label; the tail folds into "Other". */
const MAX_CONTRIBUTORS = 7;

interface Step {
  key: string;
  label: string;
  amount: number;
  pct: number | null;
  marketValue: number;
  count: number;
}

/**
 * Today's portfolio move, decomposed into the holdings that produced it.
 *
 * The baseline is zero *change*, not total portfolio value: anchoring at value
 * would need a truncated axis to make a $120 move visible against a $15,000
 * balance, which overstates every bar. Each column therefore floats from the
 * running total to the next, and the closing column is the day's net change
 * measured from zero.
 *
 * Holdings whose cached quote has no previousClose are excluded and counted
 * beneath the chart — the spec forbids showing them as flat.
 */
export default function DayChangeWaterfall({ holdings }: { holdings: Holding[] }) {
  const [hover, setHover] = useState<string | null>(null);
  const [asTable, setAsTable] = useState(false);

  const priced: Step[] = [];
  let excluded = 0;

  for (const h of holdings) {
    const amount = dayChangeAmount(h);
    if (amount == null) {
      excluded += 1;
      continue;
    }
    priced.push({
      key: h.ticker, label: h.ticker, amount,
      pct: h.dayChangePct, marketValue: h.marketValue, count: 1,
    });
  }

  if (priced.length === 0) {
    return (
      <p className="dv-empty">
        No holding has a previous close cached yet, so today&apos;s move cannot be attributed.
        {excluded > 0 && ` ${excluded} position${excluded === 1 ? '' : 's'} waiting on a quote.`}
      </p>
    );
  }

  const ranked = [...priced].sort((a, b) => Math.abs(b.amount) - Math.abs(a.amount));
  const steps = ranked.slice(0, MAX_CONTRIBUTORS);
  const tail = ranked.slice(MAX_CONTRIBUTORS);
  if (tail.length > 0) {
    steps.push({
      key: '__other', label: `Other ${tail.length}`,
      amount: tail.reduce((sum, t) => sum + t.amount, 0),
      pct: null,
      marketValue: tail.reduce((sum, t) => sum + t.marketValue, 0),
      count: tail.length,
    });
  }

  // Running cumulative, so each column floats where the previous one ended.
  let running = 0;
  const bars = steps.map((s) => {
    const start = running;
    running += s.amount;
    return { ...s, start, end: running };
  });
  const total = running;

  const levels = [0, total, ...bars.map((b) => b.start), ...bars.map((b) => b.end)];
  let yMax = Math.max(...levels);
  let yMin = Math.min(...levels);
  if (yMax === yMin) { yMax += 1; yMin -= 1; } // a perfectly flat day still needs an axis
  const pad = (yMax - yMin) * 0.12;
  yMax += pad;
  yMin -= pad;
  const range = yMax - yMin;

  const topPct = (v: number) => ((yMax - v) / range) * 100;

  if (asTable) {
    return (
      <>
        <TableToggle asTable={asTable} onToggle={setAsTable} />
        <table className="dv-table">
          <thead>
            <tr><th>Position</th><th>Market value</th><th>Today %</th><th>Contribution</th></tr>
          </thead>
          <tbody>
            {bars.map((b) => (
              <tr key={b.key}>
                <th scope="row">{b.label}</th>
                <td>{money(b.marketValue)}</td>
                <td>{b.pct == null ? '—' : percent(b.pct)}</td>
                <td>{signedMoney(b.amount)}</td>
              </tr>
            ))}
            <tr className="dv-table__total">
              <th scope="row">Net change</th><td /><td /><td>{signedMoney(total)}</td>
            </tr>
          </tbody>
        </table>
        {excluded > 0 && <p className="dv-note">{excluded} position{excluded === 1 ? '' : 's'} excluded — no previous close cached.</p>}
      </>
    );
  }

  const columns = bars.length + 1; // + the closing total

  return (
    <>
      <div className="dv-head">
        <ChartLegend items={[
          { label: 'Added', color: 'var(--gain-600)' },
          { label: 'Subtracted', color: 'var(--loss-600)' },
          { label: 'Net change', color: 'var(--ink-500)' },
        ]} />
        <TableToggle asTable={asTable} onToggle={setAsTable} />
      </div>

      <div className="dv-fall" style={{ gridTemplateColumns: `repeat(${columns}, 1fr)` }}>
        <div className="dv-fall__zero" style={{ top: `${topPct(0)}%` }} aria-hidden="true" />

        {bars.map((b, i) => {
          const up = b.amount >= 0;
          const hi = Math.max(b.start, b.end);
          const lo = Math.min(b.start, b.end);
          const active = hover === b.key;
          return (
            <div
              key={b.key}
              className={'dv-fall__col' + (active ? ' is-active' : '')}
              onMouseEnter={() => setHover(b.key)}
              onMouseLeave={() => setHover(null)}
              onFocus={() => setHover(b.key)}
              onBlur={() => setHover(null)}
              tabIndex={0}
              aria-label={
                `${b.label} ${up ? 'added' : 'subtracted'} ${money(Math.abs(b.amount))} today`
                + (b.pct == null ? '' : `, ${percent(b.pct)}`)
              }
            >
              <span
                className="dv-fall__bar"
                style={{
                  top: `${topPct(hi)}%`,
                  height: `${((hi - lo) / range) * 100}%`,
                  background: up ? 'var(--gain-600)' : 'var(--loss-600)',
                  borderRadius: up ? '4px 4px 0 0' : '0 0 4px 4px',
                }}
              />
              {/* Connector to the next column's starting level. */}
              {i < bars.length && (
                <span className="dv-fall__link" style={{ top: `${topPct(b.end)}%` }} aria-hidden="true" />
              )}
              <span
                className={'dv-fall__val ' + (up ? 'is-gain' : 'is-loss')}
                style={up ? { top: `calc(${topPct(hi)}% - 18px)` } : { top: `calc(${topPct(lo)}% + 4px)` }}
              >
                {signedMoney(b.amount, 0)}
              </span>
              <span className="dv-fall__key">{b.label}</span>

              {active && (
                <span className="dv-tip dv-tip--col" role="status">
                  <strong>{b.label}</strong>
                  <span className="dv-tip__row">
                    <span>Contribution</span><span>{signedMoney(b.amount)}</span>
                  </span>
                  <span className="dv-tip__row">
                    <span>Today</span><span>{b.pct == null ? 'mixed' : percent(b.pct)}</span>
                  </span>
                  <span className="dv-tip__row">
                    <span>Market value</span><span>{money(b.marketValue)}</span>
                  </span>
                </span>
              )}
            </div>
          );
        })}

        <div className="dv-fall__col dv-fall__col--total">
          <span
            className="dv-fall__bar"
            style={{
              top: `${topPct(Math.max(total, 0))}%`,
              height: `${(Math.abs(total) / range) * 100}%`,
              background: 'var(--ink-500)',
              borderRadius: total >= 0 ? '4px 4px 0 0' : '0 0 4px 4px',
            }}
          />
          <span
            className="dv-fall__val is-total"
            style={total >= 0
              ? { top: `calc(${topPct(Math.max(total, 0))}% - 18px)` }
              : { top: `calc(${topPct(Math.min(total, 0))}% + 4px)` }}
          >
            {signedMoney(total, 0)}
          </span>
          <span className="dv-fall__key">Net</span>
        </div>
      </div>

      {excluded > 0 && (
        <p className="dv-note">
          {excluded} position{excluded === 1 ? '' : 's'} excluded — no previous close cached, so
          their share of today&apos;s move is unknown.
        </p>
      )}
    </>
  );
}
