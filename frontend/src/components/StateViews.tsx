import { Banner, Button, Card, Skeleton } from '../design-system';
import type { ApiError } from '../api/client';
import { useLocale } from '../context/LocaleContext';

/**
 * Error surface for a failed read. Uses the loss tone and, per the content
 * guidelines, leads with what to do rather than what went wrong.
 */
export function ErrorState({ error, onRetry }: { error: ApiError; onRetry?: () => void }) {
  const { t } = useLocale();
  return (
    <Banner
      tone="loss"
      title={error.code === 'NETWORK_ERROR' ? t('Cannot reach the server', '无法连接服务器') : t('Something went wrong', '出现了问题')}
      action={onRetry ? <Button size="sm" variant="secondary" iconLeft="rotate-ccw" onClick={onRetry}>{t('Retry', '重试')}</Button> : undefined}
    >
      {error.code === 'NETWORK_ERROR'
        ? t('Could not reach the server. Check that the backend is running.', '无法连接服务器，请确认后端服务正在运行。')
        : error.message}
      {error.details.length > 0 && (
        <ul style={{ margin: 'var(--space-3) 0 0', paddingLeft: 'var(--space-6)' }}>
          {error.details.map((d, i) => (
            <li key={i}>{d.field ? `${d.field}: ${d.issue}` : d.issue}</li>
          ))}
        </ul>
      )}
    </Banner>
  );
}

/** Card-shaped placeholder while a read is in flight. */
export function LoadingCard({ lines = 3 }: { lines?: number }) {
  return (
    <Card>
      <Skeleton lines={lines} height={14} />
    </Card>
  );
}

/**
 * Price freshness. The guidelines treat a figure without a timestamp as an
 * unfinished sentence, so this renders on every screen showing valuations.
 */
export function PriceFreshness({ asOfLabel, stale }: { asOfLabel: string; stale: boolean }) {
  const { t } = useLocale();
  return stale ? (
    <Banner tone="caution" title={t('Prices may be out of date', '价格可能不是最新的')}>
      {t(`Showing the last cached quotes, from ${asOfLabel}. A refresh has been queued.`, `当前显示 ${asOfLabel} 的最近缓存行情，系统已安排刷新。`)}
    </Banner>
  ) : (
    <Banner tone="info" title={t('Prices are delayed', '价格存在延迟')}>
      {t(`Cached quotes as of ${asOfLabel}. Values may differ from your broker.`, `缓存行情截至 ${asOfLabel}，数值可能与您的券商不同。`)}
    </Banner>
  );
}
