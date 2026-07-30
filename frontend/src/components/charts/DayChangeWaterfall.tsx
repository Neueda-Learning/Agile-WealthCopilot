import { useState } from 'react';
import { money, percent, signedMoney } from '../../lib/format';
import { dayChangeAmount } from '../../lib/portfolio';
import type { Holding } from '../../types/api';
import ChartLegend from './ChartLegend';
import { TableToggle } from './PnlContributionChart';
import { useLocale } from '../../context/LocaleContext';

interface Step {
  key: string;
  label: string;
  amount: number;
  /** Never null: a step only exists when the day's move could be priced. */
  pct: number;
  marketValue: number;
}

/**
 * Today's portfolio move, decomposed into the holdings that produced it.
 *
 * The baseline is zero *change*, not total portfolio value: anchoring at value
 * would need a truncated axis to make a $120 move visible against a $15,000
 * balance, which overstates every bar. Every column — including the closing Net
 * column — is measured from that shared zero line, so gains rise, losses fall,
 * and bar heights are directly comparable against each other.
 *
 * Holdings whose cached quote has no previousClose are excluded and counted
 * beneath the chart — the spec forbids showing them as flat.
 *
 * Every priced holding gets its own column. Past the point where they all fit
 * at a legible width the plot scrolls sideways (see .dv-fall-scroll) rather
 * than squeezing bars and ticker labels into an unreadable sliver.
 */
export default function DayChangeWaterfall({ holdings }: { holdings: Holding[] }) {
  const { isChinese, t } = useLocale();
  const [hover, setHover] = useState<string | null>(null);
  const [asTable, setAsTable] = useState(false);

  const priced: Step[] = [];
  let excluded = 0;

  for (const h of holdings) {
    const amount = dayChangeAmount(h);
    // The second half is what makes the amount null in the first place; it is
    // spelled out so the percentage narrows to a number for the column.
    if (amount == null || h.dayChangePct == null) {
      excluded += 1;
      continue;
    }
    priced.push({
      key: h.ticker, label: h.ticker, amount,
      pct: h.dayChangePct, marketValue: h.marketValue,
    });
  }

  if (priced.length === 0) {
    return (
      <p className="dv-empty">
        {t("No holding has a previous close cached yet, so today's move cannot be attributed.", '尚无持仓缓存了前收盘价，因此无法分析今日变动来源。')}
        {excluded > 0 && (isChinese ? ` ${excluded} 项持仓正在等待行情。` : ` ${excluded} position${excluded === 1 ? '' : 's'} waiting on a quote.`)}
      </p>
    );
  }

  const steps = [...priced].sort((a, b) => Math.abs(b.amount) - Math.abs(a.amount));

  // Every column is measured from the same zero line: gains rise, losses fall.
  // (This was previously a running cumulative, which floated each column at the
  // previous one's endpoint. That is a legitimate waterfall, but it reads as
  // inverted — a run of losses drags the baseline down, so the gains that
  // follow get drawn *below* them and no two bars share a starting edge, which
  // makes contributions impossible to compare by eye.)
  const bars = steps.map((s) => ({ ...s, start: 0, end: s.amount }));
  const total = priced.reduce((sum, s) => sum + s.amount, 0);

  const levels = [0, total, ...bars.map((b) => b.end)];
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
            <tr><th>{t('Position', '持仓')}</th><th>{t('Market value', '市值')}</th><th>{t('Today %', '今日 %')}</th><th>{t('Contribution', '贡献')}</th></tr>
          </thead>
          <tbody>
            {bars.map((b) => (
              <tr key={b.key}>
                <th scope="row">{b.label}</th>
                <td>{money(b.marketValue)}</td>
                <td>{percent(b.pct)}</td>
                <td>{signedMoney(b.amount)}</td>
              </tr>
            ))}
            <tr className="dv-table__total">
              <th scope="row">{t('Net change', '净变动')}</th><td /><td /><td>{signedMoney(total)}</td>
            </tr>
          </tbody>
        </table>
        {excluded > 0 && <p className="dv-note">{isChinese ? `${excluded} 项持仓因没有缓存前收盘价而被排除。` : `${excluded} position${excluded === 1 ? '' : 's'} excluded — no previous close cached.`}</p>}
      </>
    );
  }

  const columns = bars.length + 1; // + the closing total

  return (
    <>
      <div className="dv-head">
        <ChartLegend items={[
          { label: t('Added', '增加'), color: 'var(--gain-600)' },
          { label: t('Subtracted', '减少'), color: 'var(--loss-600)' },
          { label: t('Net change', '净变动'), color: 'var(--ink-500)' },
        ]} />
        <TableToggle asTable={asTable} onToggle={setAsTable} />
      </div>

      {/* Only this box scrolls sideways — the page itself never does. */}
      <div className="dv-fall-scroll">
        {/* The columns share the width evenly while they fit and stop shrinking
            at --dv-col-min; min-width keeps the grid box itself as wide as its
            tracks, so the zero line spans the whole scrollable plot. */}
        <div
          className="dv-fall"
          style={{
            gridTemplateColumns: `repeat(${columns}, minmax(var(--dv-col-min), 1fr))`,
            minWidth: `calc(${columns} * var(--dv-col-min) + ${columns - 1} * var(--space-3))`,
          }}
        >
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
                  isChinese
                    ? `${b.label} 今日${up ? '增加' : '减少'} ${money(Math.abs(b.amount))}，${percent(b.pct)}`
                    : `${b.label} ${up ? 'added' : 'subtracted'} ${money(Math.abs(b.amount))} today, ${percent(b.pct)}`
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
                <span
                  className={'dv-fall__val ' + (up ? 'is-gain' : 'is-loss')}
                  style={up ? { top: `calc(${topPct(hi)}% - 18px)` } : { top: `calc(${topPct(lo)}% + 4px)` }}
                >
                  {signedMoney(b.amount, 0)}
                </span>
                <span className="dv-fall__key">{b.label}</span>

                {active && (
                  /* Pinned to the plot edges on the outermost columns: a
                     centred tooltip there would spill out of the scroll box,
                     which either clips it or invents a scrollbar. */
                  <span
                    className={'dv-tip dv-tip--col'
                      + (i === 0 ? ' is-start' : '')
                      + (i === bars.length - 1 ? ' is-end' : '')}
                    role="status"
                  >
                    <strong>{b.label}</strong>
                    <span className="dv-tip__row">
                      <span>{t('Contribution', '贡献')}</span><span>{signedMoney(b.amount)}</span>
                    </span>
                    <span className="dv-tip__row">
                      <span>{t('Today', '今日')}</span><span>{percent(b.pct)}</span>
                    </span>
                    <span className="dv-tip__row">
                      <span>{t('Market value', '市值')}</span><span>{money(b.marketValue)}</span>
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
            <span className="dv-fall__key">{t('Net', '净额')}</span>
          </div>
        </div>
      </div>

      {excluded > 0 && (
        <p className="dv-note">
          {isChinese
            ? `${excluded} 项持仓因没有缓存前收盘价而被排除，因此无法确定它们对今日变动的影响。`
            : `${excluded} position${excluded === 1 ? '' : 's'} excluded — no previous close cached, so their share of today's move is unknown.`}
        </p>
      )}
    </>
  );
}
