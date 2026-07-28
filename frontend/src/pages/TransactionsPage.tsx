import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Badge, Button, Card, DataTable, Dialog, EmptyState, IconButton, Input, Select,
} from '../design-system';
import { ApiError } from '../api/client';
import { transactions } from '../api/endpoints';
import { useAsync } from '../hooks/useAsync';
import { money, quantity, tradeDate } from '../lib/format';
import { ErrorState, LoadingCard } from '../components/StateViews';
import TransactionForm, { valuesFromTransaction } from '../components/TransactionForm';
import type { Side, Transaction } from '../types/api';

const PAGE_SIZE = 20;

export default function TransactionsPage() {
  const navigate = useNavigate();
  const [ticker, setTicker] = useState('');
  const [side, setSide] = useState<'' | Side>('');
  const [page, setPage] = useState(0);

  const [editing, setEditing] = useState<Transaction | null>(null);
  const [deleting, setDeleting] = useState<Transaction | null>(null);
  const [deleteError, setDeleteError] = useState<ApiError | null>(null);
  const [busy, setBusy] = useState(false);

  const { data, error, loading, reload } = useAsync(
    () => transactions.list({ ticker: ticker || undefined, side: side || undefined, page, size: PAGE_SIZE }),
    [ticker, side, page],
  );

  async function confirmDelete() {
    if (!deleting) return;
    setBusy(true);
    setDeleteError(null);
    try {
      await transactions.remove(deleting.id);
      setDeleting(null);
      reload();
    } catch (e) {
      // Deleting an early BUY can break a later SELL — the backend rejects it
      // with 400 and explains which position it would break.
      if (e instanceof ApiError) setDeleteError(e);
    } finally {
      setBusy(false);
    }
  }

  const rows = data?.content ?? [];
  const filtered = ticker !== '' || side !== '';

  return (
    <div className="stack">
      <div className="row-end">
        <div style={{ width: 220 }}>
          <Input
            iconLeft="search" placeholder="Filter by symbol"
            value={ticker}
            onChange={(e) => { setTicker(e.target.value.toUpperCase()); setPage(0); }}
          />
        </div>
        <div style={{ width: 180 }}>
          <Select
            value={side}
            onChange={(e) => { setSide(e.target.value as '' | Side); setPage(0); }}
            options={[
              { value: '', label: 'All types' },
              { value: 'BUY', label: 'Buy' },
              { value: 'SELL', label: 'Sell' },
            ]}
          />
        </div>
        <span className="spacer" />
        <Button iconLeft="plus" onClick={() => navigate('/log')}>Log transaction</Button>
      </div>

      {loading ? <LoadingCard lines={8} />
        : error ? <ErrorState error={error} onRetry={reload} />
        : (
          <Card flush>
            {rows.length === 0 ? (
              <EmptyState
                icon={filtered ? 'search-x' : 'receipt'}
                title={filtered ? 'No transactions match those filters' : 'No transactions yet'}
                action={!filtered
                  ? <Button iconLeft="plus" onClick={() => navigate('/log')}>Log transaction</Button>
                  : <Button variant="secondary" onClick={() => { setTicker(''); setSide(''); }}>Clear filters</Button>}
              >
                {filtered
                  ? 'Try a different symbol or type.'
                  : 'Record a buy or sell and it appears here, with cost basis and P&L updated.'}
              </EmptyState>
            ) : (
              <DataTable
                columns={[
                  { key: 'tradeDate', header: 'Date', render: (r: Transaction) => tradeDate(r.tradeDate) },
                  {
                    key: 'side', header: 'Type',
                    render: (r: Transaction) => (
                      <Badge tone={r.side === 'BUY' ? 'gain' : 'loss'}>{r.side}</Badge>
                    ),
                  },
                  {
                    key: 'ticker', header: 'Symbol',
                    render: (r: Transaction) => (
                      <div>
                        <div style={{ fontWeight: 600, color: 'var(--text-primary)' }}>{r.ticker}</div>
                        <div style={{ fontSize: 'var(--fs-sm)', color: 'var(--text-muted)' }}>{r.instrumentName}</div>
                      </div>
                    ),
                  },
                  {
                    key: 'source', header: 'Entry',
                    render: (r: Transaction) => (
                      r.source === 'AI_ASSISTED'
                        ? <Badge tone="brand" icon="sparkles">Parsed</Badge>
                        : <span style={{ color: 'var(--text-muted)', fontSize: 'var(--fs-sm)' }}>Manual</span>
                    ),
                  },
                  { key: 'quantity', header: 'Shares', align: 'right', render: (r: Transaction) => quantity(r.quantity) },
                  { key: 'price', header: 'Price', align: 'right', render: (r: Transaction) => money(r.price) },
                  { key: 'fees', header: 'Fees', align: 'right', render: (r: Transaction) => money(r.fees) },
                  {
                    key: 'total', header: 'Total', align: 'right',
                    render: (r: Transaction) => money(r.quantity * r.price + (r.side === 'BUY' ? r.fees : -r.fees)),
                  },
                  {
                    key: 'actions', header: '', width: 92,
                    render: (r: Transaction) => (
                      <div className="row" style={{ gap: 'var(--space-2)', justifyContent: 'flex-end' }}>
                        <IconButton icon="pencil" label={`Edit ${r.ticker} transaction`} size="sm"
                          onClick={() => setEditing(r)} />
                        <IconButton icon="trash-2" label={`Delete ${r.ticker} transaction`} size="sm"
                          onClick={() => { setDeleting(r); setDeleteError(null); }} />
                      </div>
                    ),
                  },
                ]}
                rows={rows}
              />
            )}
          </Card>
        )}

      {data && data.totalPages > 1 && (
        <div className="row">
          <span style={{ fontSize: 'var(--fs-sm)', color: 'var(--text-muted)' }}>
            Page {data.page + 1} of {data.totalPages} · {data.totalElements} transactions
          </span>
          <span className="spacer" />
          <Button variant="secondary" size="sm" disabled={data.page === 0}
            onClick={() => setPage((p) => Math.max(0, p - 1))}>Previous</Button>
          <Button variant="secondary" size="sm" disabled={data.page + 1 >= data.totalPages}
            onClick={() => setPage((p) => p + 1)}>Next</Button>
        </div>
      )}

      {editing && (
        <Dialog
          title="Edit transaction"
          subtitle={`${editing.ticker} · ${tradeDate(editing.tradeDate)}`}
          width={720}
          onClose={() => setEditing(null)}
        >
          <TransactionForm
            initial={valuesFromTransaction(editing)}
            source={editing.source}
            editingId={editing.id}
            submitLabel="Save changes"
            onSaved={() => { setEditing(null); reload(); }}
            onCancel={() => setEditing(null)}
          />
        </Dialog>
      )}

      {deleting && (
        <Dialog
          title="Delete this transaction?"
          subtitle={`${deleting.side} ${quantity(deleting.quantity)} ${deleting.ticker} on ${tradeDate(deleting.tradeDate)}`}
          onClose={() => setDeleting(null)}
          footer={
            <>
              <Button variant="ghost" onClick={() => setDeleting(null)}>Keep it</Button>
              <Button variant="danger" disabled={busy} onClick={confirmDelete}>
                {busy ? 'Deleting…' : 'Delete transaction'}
              </Button>
            </>
          }
        >
          <div className="stack-sm">
            <p style={{ color: 'var(--text-secondary)' }}>
              Cost basis and P&L will be recalculated. This cannot be undone.
            </p>
            {deleteError && <ErrorState error={deleteError} />}
          </div>
        </Dialog>
      )}
    </div>
  );
}
