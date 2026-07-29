import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Badge, Banner, Button, Card, DataTable, DeltaValue, EmptyState, Input, TickerAvatar,
} from '../design-system';
import { ApiError } from '../api/client';
import { portfolio } from '../api/endpoints';
import { useAsync } from '../hooks/useAsync';
import { money, percent, quantity } from '../lib/format';
import { dayChangeAmount } from '../lib/portfolio';
import { ErrorState, LoadingCard } from '../components/StateViews';
import type { Holding, PriceRefreshResult } from '../types/api';
import { useLocale } from '../context/LocaleContext';

export default function HoldingsPage() {
  const navigate = useNavigate();
  const { t } = useLocale();
  const [q, setQ] = useState('');
  const [refreshing, setRefreshing] = useState(false);
  const [refreshResult, setRefreshResult] = useState<PriceRefreshResult | null>(null);
  const [refreshError, setRefreshError] = useState<ApiError | null>(null);
  const { data, error, loading, reload } = useAsync(() => portfolio.holdings(), []);

  async function refreshPrices() {
    if (refreshing) return;
    setRefreshing(true);
    setRefreshResult(null);
    setRefreshError(null);
    try {
      setRefreshResult(await portfolio.refreshPrices());
    } catch (e) {
      if (e instanceof ApiError) setRefreshError(e);
    } finally {
      // The POST returns only after quotes have been persisted and the
      // derived portfolio caches have been evicted.
      reload();
      setRefreshing(false);
    }
  }

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
        <EmptyState icon="layers" title={t('No open positions', '暂无当前持仓')}
          action={<Button iconLeft="plus" onClick={() => navigate('/log')}>{t('Log transaction', '记录交易')}</Button>}>
          {t('Add a buy and WealthCopilot tracks value and P&L from that date.', '添加一笔买入后，WealthCopilot 会从该日起跟踪市值和损益。')}
        </EmptyState>
      </Card>
    );
  }

  return (
    <div className="stack">
      <div className="row-end">
        <div style={{ width: 280 }}>
          <Input
            iconLeft="search" placeholder={t('Search symbol or name', '搜索代码或名称')}
            value={q} onChange={(e) => setQ(e.target.value)}
          />
        </div>
        <span className="spacer" />
        <Button
          variant="secondary"
          iconLeft="refresh-cw"
          disabled={refreshing}
          onClick={refreshPrices}
        >
          {refreshing ? t('Refreshing…', '正在刷新…') : t('Refresh', '刷新')}
        </Button>
      </div>

      {refreshError && (
        <Banner tone="loss" title={t('Prices could not be refreshed', '无法刷新价格')}>
          {t(
            'The market data service did not complete the refresh. Please try again shortly.',
            '市场数据服务未能完成刷新，请稍后重试。',
          )}
        </Banner>
      )}

      {refreshResult && (
        <Banner
          tone={refreshResult.failedTickers.length === 0 ? 'gain' : 'caution'}
          title={refreshResult.failedTickers.length === 0
            ? t('Prices refreshed', '价格已刷新')
            : t('Some prices could not be refreshed', '部分价格无法刷新')}
        >
          {refreshResult.failedTickers.length === 0
            ? t(
              `Updated all ${refreshResult.refreshed} held symbols.`,
              `已更新全部 ${refreshResult.refreshed} 项持仓的价格。`,
            )
            : t(
              `Updated ${refreshResult.refreshed} of ${refreshResult.requested}. Still waiting for: ${refreshResult.failedTickers.join(', ')}.`,
              `已更新 ${refreshResult.refreshed}/${refreshResult.requested} 项。仍在等待：${refreshResult.failedTickers.join('、')}。`,
            )}
        </Banner>
      )}

      <Card flush>
        {rows.length === 0 ? (
          <EmptyState icon="search-x" title={t('No positions match that search', '没有符合搜索条件的持仓')}>
            {t(`Clear the search to see all ${all.length} positions.`, `清除搜索即可查看全部 ${all.length} 项持仓。`)}
          </EmptyState>
        ) : (
          <DataTable
            columns={[
              {
                key: 'ticker', header: t('Position', '持仓'),
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
                key: 'stale', header: t('Price', '价格'),
                render: (r: Holding) => (
                  <div className="row" style={{ gap: 'var(--space-3)' }}>
                    <span className="wc-num">{money(r.currentPrice)}</span>
                    {r.stale && <Badge tone="caution">{t('Stale', '已过期')}</Badge>}
                  </div>
                ),
              },
              { key: 'quantity', header: t('Shares', '股数'), align: 'right', render: (r: Holding) => quantity(r.quantity) },
              { key: 'avgCost', header: t('Avg cost', '平均成本'), align: 'right', render: (r: Holding) => money(r.avgCost) },
              { key: 'costBasis', header: t('Cost basis', '成本基础'), align: 'right', render: (r: Holding) => money(r.costBasis) },
              { key: 'marketValue', header: t('Market value', '市值'), align: 'right', render: (r: Holding) => money(r.marketValue) },
              {
                key: 'unrealizedPnl', header: t('Total P&L', '总损益'), align: 'right',
                render: (r: Holding) => (
                  <DeltaValue value={r.unrealizedPnl} percent={r.unrealizedPnlPct} currency="USD" size={13} />
                ),
              },
              {
                key: 'dayChangePct', header: t('Today', '今日'), align: 'right',
                render: (r: Holding) => (
                  r.dayChangePct == null
                    ? <span style={{ color: 'var(--text-muted)' }} title={t('No previous close available for this instrument', '此标的没有前收盘价')}>—</span>
                    : <DeltaValue value={dayChangeAmount(r) ?? 0} percent={r.dayChangePct} currency="USD" size={13} pill />
                ),
              },
              { key: 'weightPct', header: t('Weight', '占比'), align: 'right', render: (r: Holding) => percent(r.weightPct).replace('+', '') },
            ]}
            rows={rows}
          />
        )}
      </Card>
    </div>
  );
}
