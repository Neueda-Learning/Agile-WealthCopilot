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
import PnlContributionChart from '../components/charts/PnlContributionChart';
import DayChangeWaterfall from '../components/charts/DayChangeWaterfall';
import type { Holding, PerformanceRange } from '../types/api';
import { useLocale } from '../context/LocaleContext';

const RANGES: PerformanceRange[] = ['1M', '3M', '6M', '1Y', 'ALL'];

export default function DashboardPage() {
  const navigate = useNavigate();
  const { isChinese, t } = useLocale();
  const [range, setRange] = useState<PerformanceRange>('1M');
  const suggestions = isChinese
    ? ['我的最大持仓是什么？', '哪项持仓的亏损最多？', '我上个月投入了多少钱？']
    : ['What is my biggest position?', 'Which holding has lost me the most?', 'How much did I invest last month?'];

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
        <EmptyState icon="wallet" title={t('No positions yet', '暂无持仓')}
          action={<Button iconLeft="plus" onClick={() => navigate('/log')}>{t('Log transaction', '记录交易')}</Button>}>
          {t('Record a buy and WealthCopilot tracks value, cost basis and P&L from that date.', '记录一笔买入后，WealthCopilot 会从该日起跟踪市值、成本基础和损益。')}
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
            label={t('Total value', '总市值')} size="xl" value={money(s?.totalValue)}
            foot={
              <>
                <DeltaValue value={s?.dayChange ?? 0} percent={s?.dayChangePct ?? undefined} currency="USD" size={13} />
                <span>{t('today', '今日')}</span>
              </>
            }
          />
          <div style={{ marginTop: 'var(--space-7)' }} className="grid-3">
            <Stat label={t('Cost basis', '成本基础')} size="md" value={money(s?.totalCostBasis)} />
            <Stat label={t('Unrealized P&L', '未实现损益')} size="md" value={money(s?.unrealizedPnl)}
              foot={<DeltaValue value={s?.unrealizedPnl ?? 0} percent={s?.unrealizedPnlPct} currency="USD" size={13} showArrow={false} />} />
            <Stat label={t('Realized P&L', '已实现损益')} size="md" value={money(s?.realizedPnl)} />
          </div>
        </Card>

        <Card title={t('Allocation', '资产配置')} subtitle={isChinese ? `${all.length} 项持仓` : `${all.length} position${all.length === 1 ? '' : 's'}`}>
          {allocation.length > 0
            ? <AllocationBar segments={allocation} />
            : <span style={{ color: 'var(--text-muted)', fontSize: 'var(--fs-sm)' }}>{t('No open positions.', '暂无当前持仓。')}</span>}
        </Card>
      </div>

      <Card
        title={t('Activity', '交易活动')}
        subtitle={isChinese
          ? `${range === 'ALL' ? '全部时间' : `最近 ${range}`}的投入和已实现收益`
          : `Invested and realized over ${range === 'ALL' ? 'all time' : `the last ${range}`}`}
        action={
          <SegmentedControl
            options={RANGES.map((value) => ({
              value,
              label: isChinese
                ? ({ '1M': '1个月', '3M': '3个月', '6M': '6个月', '1Y': '1年', ALL: '全部' } as Record<PerformanceRange, string>)[value]
                : value,
            }))}
            value={range}
            onChange={(r: string) => setRange(r as PerformanceRange)}
          />
        }
      >
        {performance.error ? (
          <ErrorState error={performance.error} onRetry={performance.reload} />
        ) : (
          <div className="grid-3">
            <Stat label={t('Invested', '投入金额')} size="md" value={money(performance.data?.investedAmount)}
              foot={<span>{isChinese ? `${performance.data?.buyCount ?? 0} 笔买入` : `${performance.data?.buyCount ?? 0} buys`}</span>} />
            <Stat label={t('Proceeds', '卖出收入')} size="md" value={money(performance.data?.proceedsAmount)}
              foot={<span>{isChinese ? `${performance.data?.sellCount ?? 0} 笔卖出` : `${performance.data?.sellCount ?? 0} sells`}</span>} />
            <Stat label={t('Realized in range', '区间已实现损益')} size="md" value={money(performance.data?.realizedPnl)} />
          </div>
        )}
      </Card>

      <Card
        title={t('P&L contribution', '损益贡献')}
        subtitle={t('Unrealized gain and loss by position, largest winner first', '各持仓的未实现收益和亏损，按收益从高到低排列')}
      >
        <PnlContributionChart
          holdings={all}
          onSelect={(ticker) => navigate('/transactions', { state: { ticker } })}
        />
      </Card>

      <div className="grid-2">
        <Card
          title={t('Top movers', '涨跌幅榜')} subtitle={t('Today', '今日')} flush
          action={
            <Button variant="ghost" size="sm" iconRight="chevron-right" onClick={() => navigate('/holdings')}>
              {t('All holdings', '全部持仓')}
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
                  key: 'ticker', header: t('Position', '持仓'),
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
                { key: 'quantity', header: t('Shares', '股数'), align: 'right', render: (r: Holding) => quantity(r.quantity) },
                { key: 'marketValue', header: t('Market value', '市值'), align: 'right', render: (r: Holding) => money(r.marketValue) },
                {
                  key: 'dayChangePct', header: t('Today', '今日'), align: 'right',
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
          title={t("Today's change by holding", '各持仓今日变动')}
          subtitle={t('What actually moved the portfolio — a big percentage on a small position barely registers', '查看每项持仓对组合变动的实际影响，小额持仓的大幅波动影响仍可能很小')}
        >
          {holdings.error
            ? <ErrorState error={holdings.error} onRetry={holdings.reload} />
            : <DayChangeWaterfall holdings={all} />}
        </Card>
      </div>

      <div className="grid-2">
        <Card
          title={t('Ask Copilot', '咨询智能助手')} subtitle={t('Nothing is saved until you confirm it.', '只有您确认后才会保存。')}
          action={<Badge tone="brand" icon="sparkles">{t('Beta', '测试版')}</Badge>}
        >
          <div className="stack-sm" style={{ gap: 'var(--space-3)' }}>
            {suggestions.map((q) => (
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
