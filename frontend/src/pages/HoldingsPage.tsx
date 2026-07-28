import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Badge, Button, Card, DataTable, DeltaValue, EmptyState, Input, TickerAvatar,
} from '../design-system';
import { portfolio } from '../api/endpoints';
import { useAsync } from '../hooks/useAsync';
import { money, percent, quantity } from '../lib/format';
import { dayChangeAmount } from '../lib/portfolio';
import { ErrorState, LoadingCard } from '../components/StateViews';
import type { Holding } from '../types/api';

export default function HoldingsPage() {
  const navigate = useNavigate();
  const [q, setQ] = useState('');
  const { data, error, loading, reload } = useAsync(() => portfolio.holdings(), []);

  const rows = useMemo(() => {
    const all = data ?? [];
    const needle = q.trim().toLowerCase();
    if (!needle) return all;
    return all.filter((h) => `${h.ticker} ${h.name}`.toLowerCase().includes(needle));
  }, [data, q]);

  if (loading) return <LoadingCard lines={8} />;
  if (error) return <ErrorState error={error} onRetry={reload} />;

  const all = data ?? [];

  if (all.length === 0) {
    return (
      <Card>
        <EmptyState icon="layers" title="No open positions"
          action={<Button iconLeft="plus" onClick={() => navigate('/log')}>Log transaction</Button>}>
          Add a buy and WealthCopilot tracks value and P&L from that date.
        </EmptyState>
      </Card>
    );
  }

  return (
    <div className="stack">
      <div className="row-end">
        <div style={{ width: 280 }}>
          <Input
            iconLeft="search" placeholder="Search symbol or name"
            value={q} onChange={(e) => setQ(e.target.value)}
          />
        </div>
        <span className="spacer" />
        <Button variant="secondary" iconLeft="refresh-cw" onClick={reload}>Refresh</Button>
      </div>

      <Card flush>
        {rows.length === 0 ? (
          <EmptyState icon="search-x" title="No positions match that search">
            Clear the search to see all {all.length} positions.
          </EmptyState>
        ) : (
          <DataTable
            columns={[
              {
                key: 'ticker', header: 'Position',
                render: (r: Holding) => (
                  <div className="row">
                    <TickerAvatar symbol={r.ticker} />
                    <div>
                      <div style={{ fontWeight: 600, color: 'var(--text-primary)' }}>{r.ticker}</div>
                      <div style={{ fontSize: 'var(--fs-sm)', color: 'var(--text-muted)' }}>{r.name}</div>
                    </div>
                  </div>
                ),
              },
              {
                key: 'stale', header: 'Price',
                render: (r: Holding) => (
                  <div className="row" style={{ gap: 'var(--space-3)' }}>
                    <span className="wc-num">{money(r.currentPrice)}</span>
                    {r.stale && <Badge tone="caution">Stale</Badge>}
                  </div>
                ),
              },
              { key: 'quantity', header: 'Shares', align: 'right', render: (r: Holding) => quantity(r.quantity) },
              { key: 'avgCost', header: 'Avg cost', align: 'right', render: (r: Holding) => money(r.avgCost) },
              { key: 'costBasis', header: 'Cost basis', align: 'right', render: (r: Holding) => money(r.costBasis) },
              { key: 'marketValue', header: 'Market value', align: 'right', render: (r: Holding) => money(r.marketValue) },
              {
                key: 'unrealizedPnl', header: 'Total P&L', align: 'right',
                render: (r: Holding) => (
                  <DeltaValue value={r.unrealizedPnl} percent={r.unrealizedPnlPct} currency="USD" size={13} />
                ),
              },
              {
                key: 'dayChangePct', header: 'Today', align: 'right',
                render: (r: Holding) => (
                  r.dayChangePct == null
                    ? <span style={{ color: 'var(--text-muted)' }} title="No previous close available for this instrument">—</span>
                    : <DeltaValue value={dayChangeAmount(r) ?? 0} percent={r.dayChangePct} currency="USD" size={13} pill />
                ),
              },
              { key: 'weightPct', header: 'Weight', align: 'right', render: (r: Holding) => percent(r.weightPct).replace('+', '') },
            ]}
            rows={rows}
          />
        )}
      </Card>
    </div>
  );
}
