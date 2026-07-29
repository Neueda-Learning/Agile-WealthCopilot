import { useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
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
import { useLocale } from '../context/LocaleContext';

const PAGE_SIZE = 20;

export default function TransactionsPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { isChinese, t } = useLocale();
  // Arriving from a chart bar pre-filters to that holding's history.
  const [ticker, setTicker] = useState((location.state as { ticker?: string } | null)?.ticker ?? '');
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
            iconLeft="search" placeholder={t('Filter by symbol', '按代码筛选')}
            value={ticker}
            onChange={(e) => { setTicker(e.target.value.toUpperCase()); setPage(0); }}
          />
        </div>
        <div style={{ width: 180 }}>
          <Select
            value={side}
            onChange={(e) => { setSide(e.target.value as '' | Side); setPage(0); }}
            options={[
              { value: '', label: t('All types', '全部类型') },
              { value: 'BUY', label: t('Buy', '买入') },
              { value: 'SELL', label: t('Sell', '卖出') },
            ]}
          />
        </div>
        <span className="spacer" />
        <Button iconLeft="plus" onClick={() => navigate('/log')}>{t('Log transaction', '记录交易')}</Button>
      </div>

      {loading ? <LoadingCard lines={8} />
        : error ? <ErrorState error={error} onRetry={reload} />
        : (
          <Card flush>
            {rows.length === 0 ? (
              <EmptyState
                icon={filtered ? 'search-x' : 'receipt'}
                title={filtered ? t('No transactions match those filters', '没有符合筛选条件的交易') : t('No transactions yet', '暂无交易记录')}
                action={!filtered
                  ? <Button iconLeft="plus" onClick={() => navigate('/log')}>{t('Log transaction', '记录交易')}</Button>
                  : <Button variant="secondary" onClick={() => { setTicker(''); setSide(''); }}>{t('Clear filters', '清除筛选')}</Button>}
              >
                {filtered
                  ? t('Try a different symbol or type.', '请尝试其他代码或交易类型。')
                  : t('Record a buy or sell and it appears here, with cost basis and P&L updated.', '记录买入或卖出后，交易会显示在这里，并更新成本基础和损益。')}
              </EmptyState>
            ) : (
              <DataTable
                columns={[
                  { key: 'tradeDate', header: t('Date', '日期'), render: (r: Transaction) => tradeDate(r.tradeDate) },
                  {
                    key: 'side', header: t('Type', '类型'),
                    render: (r: Transaction) => (
                      <Badge tone={r.side === 'BUY' ? 'gain' : 'loss'}>{r.side === 'BUY' ? t('BUY', '买入') : t('SELL', '卖出')}</Badge>
                    ),
                  },
                  {
                    key: 'ticker', header: t('Symbol', '代码'),
                    render: (r: Transaction) => (
                      <div>
                        <div style={{ fontWeight: 600, color: 'var(--text-primary)' }}>{r.ticker}</div>
                        <div style={{ fontSize: 'var(--fs-sm)', color: 'var(--text-muted)' }}>{r.instrumentName}</div>
                      </div>
                    ),
                  },
                  {
                    key: 'source', header: t('Entry', '录入方式'),
                    render: (r: Transaction) => (
                      r.source === 'AI_ASSISTED'
                        ? <Badge tone="brand" icon="sparkles">{t('Parsed', 'AI 解析')}</Badge>
                        : <span style={{ color: 'var(--text-muted)', fontSize: 'var(--fs-sm)' }}>{t('Manual', '手动')}</span>
                    ),
                  },
                  { key: 'quantity', header: t('Shares', '股数'), align: 'right', render: (r: Transaction) => quantity(r.quantity) },
                  { key: 'price', header: t('Price', '价格'), align: 'right', render: (r: Transaction) => money(r.price) },
                  { key: 'fees', header: t('Fees', '费用'), align: 'right', render: (r: Transaction) => money(r.fees) },
                  {
                    key: 'total', header: t('Total', '总额'), align: 'right',
                    render: (r: Transaction) => money(r.quantity * r.price + (r.side === 'BUY' ? r.fees : -r.fees)),
                  },
                  {
                    key: 'actions', header: '', width: 92,
                    render: (r: Transaction) => (
                      <div className="row" style={{ gap: 'var(--space-2)', justifyContent: 'flex-end' }}>
                        <IconButton icon="pencil" label={t(`Edit ${r.ticker} transaction`, `编辑 ${r.ticker} 交易`)} size="sm"
                          onClick={() => setEditing(r)} />
                        <IconButton icon="trash-2" label={t(`Delete ${r.ticker} transaction`, `删除 ${r.ticker} 交易`)} size="sm"
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
            {isChinese
              ? `第 ${data.page + 1} 页，共 ${data.totalPages} 页 · ${data.totalElements} 笔交易`
              : `Page ${data.page + 1} of ${data.totalPages} · ${data.totalElements} transactions`}
          </span>
          <span className="spacer" />
          <Button variant="secondary" size="sm" disabled={data.page === 0}
            onClick={() => setPage((p) => Math.max(0, p - 1))}>{t('Previous', '上一页')}</Button>
          <Button variant="secondary" size="sm" disabled={data.page + 1 >= data.totalPages}
            onClick={() => setPage((p) => p + 1)}>{t('Next', '下一页')}</Button>
        </div>
      )}

      {editing && (
        <Dialog
          title={t('Edit transaction', '编辑交易')}
          subtitle={`${editing.ticker} · ${tradeDate(editing.tradeDate)}`}
          width={720}
          onClose={() => setEditing(null)}
        >
          <TransactionForm
            initial={valuesFromTransaction(editing)}
            source={editing.source}
            editingId={editing.id}
            submitLabel={t('Save changes', '保存更改')}
            onSaved={() => { setEditing(null); reload(); }}
            onCancel={() => setEditing(null)}
          />
        </Dialog>
      )}

      {deleting && (
        <Dialog
          title={t('Delete this transaction?', '删除这笔交易？')}
          subtitle={isChinese
            ? `${deleting.side === 'BUY' ? '买入' : '卖出'} ${quantity(deleting.quantity)} 股 ${deleting.ticker}，日期 ${tradeDate(deleting.tradeDate)}`
            : `${deleting.side} ${quantity(deleting.quantity)} ${deleting.ticker} on ${tradeDate(deleting.tradeDate)}`}
          onClose={() => setDeleting(null)}
          footer={
            <>
              <Button variant="ghost" onClick={() => setDeleting(null)}>{t('Keep it', '保留')}</Button>
              <Button variant="danger" disabled={busy} onClick={confirmDelete}>
                {busy ? t('Deleting…', '正在删除…') : t('Delete transaction', '删除交易')}
              </Button>
            </>
          }
        >
          <div className="stack-sm">
            <p style={{ color: 'var(--text-secondary)' }}>
              {t('Cost basis and P&L will be recalculated. This cannot be undone.', '成本基础和损益将重新计算。此操作无法撤销。')}
            </p>
            {deleteError && <ErrorState error={deleteError} />}
          </div>
        </Dialog>
      )}
    </div>
  );
}
