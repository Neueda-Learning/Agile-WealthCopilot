import { useEffect, useMemo, useState } from 'react';
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

/**
 * Shortest gap between two manual refreshes. Each held symbol costs one
 * market-data credit, so an un-throttled button is a quota leak.
 */
const MIN_COOLDOWN_SECONDS = 5;

export default function HoldingsPage() {
  const navigate = useNavigate();
  const { t } = useLocale();
  const [q, setQ] = useState('');
  const [refreshing, setRefreshing] = useState(false);
  const [cooldown, setCooldown] = useState(0);
  const [refreshResult, setRefreshResult] = useState<PriceRefreshResult | null>(null);
  const [refreshError, setRefreshError] = useState<ApiError | null>(null);
  const { data, error, loading, reload } = useAsync(() => portfolio.holdings(), []);

  useEffect(() => {
    if (cooldown <= 0) return undefined;
    const timer = window.setTimeout(() => setCooldown((s) => Math.max(0, s - 1)), 1000);
    return () => window.clearTimeout(timer);
  }, [cooldown]);

  async function refreshPrices() {
    if (refreshing || cooldown > 0) return;
    setRefreshing(true);
    setRefreshResult(null);
    setRefreshError(null);
    try {
      const result = await portfolio.refreshPrices();
      setRefreshResult(result);
      // Symbols the provider's per-minute allowance could not cover come back
      // queued; block the button until those credits are actually available.
      setCooldown(Math.max(MIN_COOLDOWN_SECONDS, result.retryAfterSeconds ?? 0));
    } catch (e) {
      if (e instanceof ApiError) setRefreshError(e);
      setCooldown(MIN_COOLDOWN_SECONDS);
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

  // Only the first load gets a skeleton. The reload that follows a refresh keeps
  // the previous rows on screen, so the result banner stays readable instead of
  // the page blanking out underneath it.
  if (loading && data === null) return <LoadingCard lines={8} />;
  if (error && data === null) return <ErrorState error={error} onRetry={reload} />;

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
          disabled={refreshing || cooldown > 0}
          onClick={refreshPrices}
        >
          {refreshing
            ? t('Refreshing…', '正在刷新…')
            : cooldown > 0
              ? t(`Refresh in ${cooldown}s`, `${cooldown} 秒后可刷新`)
              : t('Refresh', '刷新')}
        </Button>
      </div>

      {error && <ErrorState error={error} onRetry={reload} />}

      {refreshError && (
        <Banner tone="loss" title={t('Prices could not be refreshed', '无法刷新价格')}>
          {t(
            'The market data service did not complete the refresh, so the prices below are the last cached quotes. Please try again shortly.',
            '市场数据服务未能完成刷新，下方显示的是最近缓存的行情，请稍后重试。',
          )}
        </Banner>
      )}

      {refreshResult && <RefreshBanner result={refreshResult} />}

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

/**
 * Reports what a manual refresh actually achieved. A partial result is the
 * normal outcome for a portfolio wider than the market data plan's per-minute
 * symbol allowance, so it reads as progress rather than as a failure.
 */
function RefreshBanner({ result }: { result: PriceRefreshResult }) {
  const { t } = useLocale();
  const queued = result.queuedTickers ?? [];
  const failed = result.failedTickers ?? [];
  const complete = queued.length === 0 && failed.length === 0;
  // Nothing fetched *and* the provider rejected symbols is a failure; nothing
  // fetched because the plan is out of credits is only a wait.
  const broken = result.refreshed === 0 && failed.length > 0;
  const wait = Math.max(1, result.retryAfterSeconds ?? 0);

  return (
    <Banner
      tone={complete ? 'gain' : broken ? 'loss' : 'caution'}
      title={complete
        ? t('Prices refreshed', '价格已刷新')
        : broken
          ? t('Prices could not be refreshed', '无法刷新价格')
          : t('Some prices are still updating', '部分价格仍在更新')}
    >
      {complete
        ? t(
          `Fetched live prices for all ${result.refreshed} held symbols.`,
          `已获取全部 ${result.refreshed} 项持仓的实时价格。`,
        )
        : t(
          `Fetched live prices for ${result.refreshed} of ${result.requested} held symbols.`,
          `已获取 ${result.requested} 项持仓中 ${result.refreshed} 项的实时价格。`,
        )}
      {queued.length > 0 && ' '}
      {queued.length > 0 && t(
        `The market data plan prices a limited number of symbols per minute, so ${queued.join(', ')} are queued and refresh automatically in about ${wait}s. Their last cached price is shown below.`,
        `市场数据套餐每分钟可查询的标的数量有限，因此 ${queued.join('、')} 已排队，将在约 ${wait} 秒后自动刷新。下方显示的是它们最近的缓存价格。`,
      )}
      {failed.length > 0 && ' '}
      {failed.length > 0 && t(
        `The provider returned no quote for ${failed.join(', ')}; the last cached price is shown below.`,
        `数据提供方未返回 ${failed.join('、')} 的行情，下方显示的是最近的缓存价格。`,
      )}
    </Banner>
  );
}
