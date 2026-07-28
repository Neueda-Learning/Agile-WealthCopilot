import { Banner, Button, Card, Skeleton } from '../design-system';
import type { ApiError } from '../api/client';

/**
 * Error surface for a failed read. Uses the loss tone and, per the content
 * guidelines, leads with what to do rather than what went wrong.
 */
export function ErrorState({ error, onRetry }: { error: ApiError; onRetry?: () => void }) {
  return (
    <Banner
      tone="loss"
      title={error.code === 'NETWORK_ERROR' ? 'Cannot reach the server' : 'Something went wrong'}
      action={onRetry ? <Button size="sm" variant="secondary" iconLeft="rotate-ccw" onClick={onRetry}>Retry</Button> : undefined}
    >
      {error.message}
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
  return stale ? (
    <Banner tone="caution" title="Prices may be out of date">
      Showing the last cached quotes, from {asOfLabel}. A refresh has been queued.
    </Banner>
  ) : (
    <Banner tone="info" title="Prices are delayed">
      Cached quotes as of {asOfLabel}. Values may differ from your broker.
    </Banner>
  );
}
