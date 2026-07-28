import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  AllocationBar, Badge, Button, Card, DataTable, DeltaValue, EmptyState,
  Icon, SegmentedControl, Stat, TickerAvatar,
} from '../design-system';
import { portfolio } from '../api/endpoints';
import { useAsync } from '../hooks/useAsync';
import { asOf, money, quantity } from '../lib/format';
import { dayChangeAmount } from '../lib/portfolio';
import { ErrorState, LoadingCard, PriceFreshness } from '../components/StateViews';
import type { Holding, PerformanceRange } from '../types/api';

const RANGES: PerformanceRange[] = ['1M', '3M', '6M', '1Y', 'ALL'];

const SUGGESTIONS = [
  'What is my biggest position?',
  'Which holding has lost me the most?',
  'How much did I invest last month?',
];

export default function DashboardPage() {
  const navigate = useNavigate();
  const [range, setRange] = useState<PerformanceRange>('1M');

  const summary = useAsync(() => portfolio.summary(), []);
  const holdings = useAsync(() => portfolio.holdings(), []);
  const performance = useAsync(() => portfolio.performance(range), [range]);

  if (summary.loading || holdings.loading) {
    return <div className="stack"><LoadingCard lines={4} /><LoadingCard lines={6} /></div>;
  }
  if (summary.error) return <ErrorState error={summary.error} onRetry={summary.reload} />;

  const s = summary.data;
  const all = holdings.data ?? [];

  if (s && s.totalValue === 0 && all.length === 0) {
    return (
      <Card>
        <EmptyState icon="wallet" title="No positions yet"
          action={<Button iconLeft="plus" onClick={() => navigate('/log')}>Log transaction</Button>}>
          Record a buy and WealthCopilot tracks value, cost basis and P&L from that date.
        </EmptyState>
      </Card>
    );
  }

  // Largest absolute day moves first; holdings with no previousClose report a
  // null dayChangePct and sort last rather than being treated as flat.
  const movers = [...all]
    .sort((a, b) => Math.abs(b.dayChangePct ?? -1) - Math.abs(a.dayChangePct ?? -1))
    .slice(0, 5);

  const allocation = all.map((h) => ({ label: h.ticker, value: h.marketValue }));

  return (
    <div className="stack">
      {s && <PriceFreshness asOfLabel={asOf(s.pricesAsOf)} stale={s.stale} />}

      <div className="grid-2">
        <Card>
          <Stat
            label="Total value" size="xl" value={money(s?.totalValue)}
            foot={
              <>
                <DeltaValue value={s?.dayChange ?? 0} percent={s?.dayChangePct ?? undefined} currency="USD" size={13} />
                <span>today</span>
              </>
            }
          />
          <div style={{ marginTop: 'var(--space-7)' }} className="grid-3">
            <Stat label="Cost basis" size="md" value={money(s?.totalCostBasis)} />
            <Stat label="Unrealized P&L" size="md" value={money(s?.unrealizedPnl)}
              foot={<DeltaValue value={s?.unrealizedPnl ?? 0} percent={s?.unrealizedPnlPct} currency="USD" size={13} showArrow={false} />} />
            <Stat label="Realized P&L" size="md" value={money(s?.realizedPnl)} />
          </div>
        </Card>

        <Card title="Allocation" subtitle={`${all.length} position${all.length === 1 ? '' : 's'}`}>
          {allocation.length > 0
            ? <AllocationBar segments={allocation} />
            : <span style={{ color: 'var(--text-muted)', fontSize: 'var(--fs-sm)' }}>No open positions.</span>}
        </Card>
      </div>

      <Card
        title="Activity" subtitle={`Invested and realized over ${range === 'ALL' ? 'all time' : `the last ${range}`}`}
        action={<SegmentedControl options={RANGES} value={range} onChange={(r: string) => setRange(r as PerformanceRange)} />}
      >
        {performance.error ? (
          <ErrorState error={performance.error} onRetry={performance.reload} />
        ) : (
          <div className="grid-3">
            <Stat label="Invested" size="md" value={money(performance.data?.investedAmount)}
              foot={<span>{performance.data?.buyCount ?? 0} buys</span>} />
            <Stat label="Proceeds" size="md" value={money(performance.data?.proceedsAmount)}
              foot={<span>{performance.data?.sellCount ?? 0} sells</span>} />
            <Stat label="Realized in range" size="md" value={money(performance.data?.realizedPnl)} />
          </div>
        )}
      </Card>

      <div className="grid-2">
        <Card
          title="Top movers" subtitle="Today" flush
          action={
            <Button variant="ghost" size="sm" iconRight="chevron-right" onClick={() => navigate('/holdings')}>
              All holdings
            </Button>
          }
        >
          {holdings.error ? (
            <div style={{ padding: 'var(--card-padding)' }}>
              <ErrorState error={holdings.error} onRetry={holdings.reload} />
            </div>
          ) : (
            <DataTable
              compact
              onRowClick={() => navigate('/holdings')}
              columns={[
                {
                  key: 'ticker', header: 'Position',
                  render: (r: Holding) => (
                    <div className="row">
                      <TickerAvatar symbol={r.ticker} size={30} />
                      <div>
                        <div style={{ fontWeight: 600, color: 'var(--text-primary)' }}>{r.ticker}</div>
                        <div style={{ fontSize: 'var(--fs-sm)', color: 'var(--text-muted)' }}>{r.name}</div>
                      </div>
                    </div>
                  ),
                },
                { key: 'quantity', header: 'Shares', align: 'right', render: (r: Holding) => quantity(r.quantity) },
                { key: 'marketValue', header: 'Market value', align: 'right', render: (r: Holding) => money(r.marketValue) },
                {
                  key: 'dayChangePct', header: 'Today', align: 'right',
                  render: (r: Holding) => (
                    r.dayChangePct == null
                      ? <span style={{ color: 'var(--text-muted)' }}>—</span>
                      : <DeltaValue value={dayChangeAmount(r) ?? 0} percent={r.dayChangePct} currency="USD" size={13} pill />
                  ),
                },
              ]}
              rows={movers}
            />
          )}
        </Card>

        <Card
          title="Ask Copilot" subtitle="Read-only. It cannot trade."
          action={<Badge tone="brand" icon="sparkles">Beta</Badge>}
        >
          <div className="stack-sm" style={{ gap: 'var(--space-3)' }}>
            {SUGGESTIONS.map((q) => (
              <button
                key={q} type="button" className="wc-btn wc-btn--secondary wc-btn--sm"
                style={{ justifyContent: 'space-between', width: '100%', fontWeight: 500 }}
                onClick={() => navigate('/copilot', { state: { seed: q } })}
              >
                {q}<Icon name="arrow-right" size={14} />
              </button>
            ))}
          </div>
        </Card>
      </div>
    </div>
  );
}
