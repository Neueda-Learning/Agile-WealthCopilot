import { useState } from 'react';
import { Banner, Button, Input, Select } from '../design-system';
import { ApiError } from '../api/client';
import { transactions } from '../api/endpoints';
import { money, todayIso } from '../lib/format';
import type { Side, Transaction, TransactionDraft, TransactionRequest, TransactionSource } from '../types/api';
import TickerSearchInput from './TickerSearchInput';
import { useLocale } from '../context/LocaleContext';

export interface TransactionFormValues {
  ticker: string;
  side: Side;
  quantity: string;
  price: string;
  fees: string;
  tradeDate: string;
  note: string;
}

export function emptyValues(): TransactionFormValues {
  return { ticker: '', side: 'BUY', quantity: '', price: '', fees: '0', tradeDate: todayIso(), note: '' };
}

/** Seeds the form from an AI draft (Feature 1) — the user still confirms. */
export function valuesFromDraft(d: TransactionDraft): TransactionFormValues {
  return {
    ticker: d.ticker, side: d.side,
    quantity: String(d.quantity), price: String(d.price),
    fees: '0', tradeDate: d.tradeDate, note: '',
  };
}

export function valuesFromTransaction(t: Transaction): TransactionFormValues {
  return {
    ticker: t.ticker, side: t.side,
    quantity: String(t.quantity), price: String(t.price), fees: String(t.fees),
    tradeDate: t.tradeDate, note: t.note ?? '',
  };
}

interface Props {
  initial: TransactionFormValues;
  /** AI_ASSISTED when the values came from the parser — advisory audit flag. */
  source: TransactionSource;
  editingId?: number;
  submitLabel?: string;
  onSaved: (t: Transaction) => void;
  onCancel?: () => void;
}

export default function TransactionForm({
  initial, source, editingId, submitLabel, onSaved, onCancel,
}: Props) {
  const { t } = useLocale();
  const resolvedSubmitLabel = submitLabel ?? t('Save transaction', '保存交易');
  const [v, setV] = useState<TransactionFormValues>(initial);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  // Timeline rejections arrive as details[] without a field — they describe a
  // conflict across the whole position, not one bad input.
  const [formErrors, setFormErrors] = useState<string[]>([]);
  const [message, setMessage] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const set = <K extends keyof TransactionFormValues>(k: K, val: TransactionFormValues[K]) =>
    setV((prev) => ({ ...prev, [k]: val }));

  const qty = Number(v.quantity);
  const price = Number(v.price);
  const total = Number.isFinite(qty) && Number.isFinite(price) ? qty * price : 0;
  const valid = v.ticker.trim() !== '' && qty > 0 && price > 0 && v.tradeDate !== '';

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setFieldErrors({});
    setFormErrors([]);
    setMessage(null);
    setBusy(true);

    const body: TransactionRequest = {
      ticker: v.ticker.trim().toUpperCase(),
      side: v.side,
      quantity: qty,
      price,
      fees: Number(v.fees) || 0,
      tradeDate: v.tradeDate,
      note: v.note.trim() || null,
      source,
    };

    try {
      const saved = editingId
        ? await transactions.update(editingId, body)
        : await transactions.create(body);
      onSaved(saved);
    } catch (err) {
      if (err instanceof ApiError) {
        setFieldErrors(err.fieldIssues());
        setFormErrors(err.details.filter((d) => !d.field).map((d) => d.issue));
        setMessage(err.message);
      } else {
        setMessage(t('Could not save the transaction.', '无法保存交易。'));
      }
    } finally {
      setBusy(false);
    }
  }

  return (
    <form onSubmit={submit} className="stack-sm">
      {message && (
        <Banner tone="loss" title={editingId ? t('Cannot update this transaction', '无法更新此交易') : t('Cannot save this transaction', '无法保存此交易')}>
          {message}
          {formErrors.length > 0 && (
            <ul style={{ margin: 'var(--space-3) 0 0', paddingLeft: 'var(--space-6)' }}>
              {formErrors.map((issue, i) => <li key={i}>{issue}</li>)}
            </ul>
          )}
        </Banner>
      )}

      <div className="grid-3">
        <Select
          label={t('Action', '操作')}
          value={v.side}
          onChange={(e) => set('side', e.target.value as Side)}
          options={[{ value: 'BUY', label: t('Buy', '买入') }, { value: 'SELL', label: t('Sell', '卖出') }]}
        />
        <TickerSearchInput
          value={v.ticker}
          onChange={(t) => set('ticker', t)}
          error={fieldErrors.ticker}
        />
        <Input
          label={t('Trade date', '交易日期')} type="date" required
          value={v.tradeDate}
          onChange={(e) => set('tradeDate', e.target.value)}
          error={fieldErrors.tradeDate}
        />
        <Input
          label={t('Quantity', '数量')} numeric inputMode="decimal" required
          value={v.quantity}
          onChange={(e) => set('quantity', e.target.value)}
          error={fieldErrors.quantity || (v.quantity !== '' && qty <= 0 ? t('Enter a quantity above 0.', '请输入大于 0 的数量。') : undefined)}
        />
        <Input
          label={t('Price per share', '每股价格')} prefix="$" numeric inputMode="decimal" required
          value={v.price}
          onChange={(e) => set('price', e.target.value)}
          error={fieldErrors.price || (v.price !== '' && price <= 0 ? t('Enter a price above 0.', '请输入大于 0 的价格。') : undefined)}
        />
        <Input
          label={t('Fees', '费用')} prefix="$" numeric inputMode="decimal"
          value={v.fees}
          onChange={(e) => set('fees', e.target.value)}
          error={fieldErrors.fees}
        />
      </div>

      <Input
        label={t('Note', '备注')} placeholder={t('Optional', '可选')}
        value={v.note}
        onChange={(e) => set('note', e.target.value)}
        error={fieldErrors.note}
      />

      <div className="row">
        <span style={{ fontSize: 'var(--fs-sm)', color: 'var(--text-muted)' }}>
          {v.side === 'BUY' ? t('Total cost', '总成本') : t('Total proceeds', '总收入')}
        </span>
        <span className="wc-num" style={{ fontWeight: 500, color: 'var(--text-primary)' }}>
          {money(total)}
        </span>
        <span className="spacer" />
        {onCancel && (
          <Button type="button" variant="ghost" onClick={onCancel}>{t('Cancel', '取消')}</Button>
        )}
        <Button type="submit" disabled={busy || !valid}>
          {busy ? t('Saving…', '正在保存…') : resolvedSubmitLabel}
        </Button>
      </div>
    </form>
  );
}
