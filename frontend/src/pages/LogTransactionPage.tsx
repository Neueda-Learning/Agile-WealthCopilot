import { useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import {
  Badge, Banner, Button, Card, ChatComposer, ParsedTransactionCard,
} from '../design-system';
import { ApiError } from '../api/client';
import { ai, transactions as txApi } from '../api/endpoints';
import { money, quantity, tradeDate } from '../lib/format';
import TransactionForm, { emptyValues, valuesFromDraft } from '../components/TransactionForm';
import type { Confidence, Transaction, TransactionDraft } from '../types/api';

const EXAMPLES = [
  'Bought 15 Nvidia at 142 last Tuesday',
  'Sold 20 AAPL at 182.40 yesterday',
];

/** Copilot's own confidence, lower-cased for the badge. */
const CONFIDENCE_LABEL: Record<Confidence, 'high' | 'medium' | 'low'> = {
  HIGH: 'high', MEDIUM: 'medium', LOW: 'low',
};

export default function LogTransactionPage() {
  const navigate = useNavigate();
  const location = useLocation();
  // The Copilot agent can hand over a draft; it never writes one itself.
  const handoff = (location.state as { draft?: TransactionDraft } | null)?.draft ?? null;

  const [text, setText] = useState('');
  const [draft, setDraft] = useState<TransactionDraft | null>(handoff);
  const [confidence, setConfidence] = useState<Confidence | null>(handoff ? 'HIGH' : null);
  const [warnings, setWarnings] = useState<string[]>([]);
  const [sourceText, setSourceText] = useState<string | null>(null);

  const [parseError, setParseError] = useState<ApiError | null>(null);
  const [parsing, setParsing] = useState(false);
  // A handoff arrives because the user chose "Edit" in the chat, so go
  // straight to the form; a draft parsed here starts on the review card.
  const [editing, setEditing] = useState(handoff !== null);
  const [manual, setManual] = useState(false);
  const [saved, setSaved] = useState<Transaction | null>(null);
  const [saving, setSaving] = useState(false);

  async function parse() {
    const t = text.trim();
    if (!t) return;
    setParsing(true);
    setParseError(null);
    setSaved(null);
    try {
      const res = await ai.parseTransaction(t);
      setDraft(res.draft);
      setConfidence(res.confidence);
      setWarnings(res.warnings ?? []);
      setSourceText(t);
      setEditing(false);
    } catch (e) {
      if (e instanceof ApiError) setParseError(e);
      setDraft(null);
    } finally {
      setParsing(false);
    }
  }

  /**
   * Confirming the card saves it outright — the review card already showed
   * every field, so re-presenting the same values in a form asked the user to
   * approve twice. The form is only for when they want to change something,
   * or when the save is rejected and they need to fix it.
   */
  async function confirmDraft() {
    if (!draft || saving) return;
    setSaving(true);
    setParseError(null);
    try {
      const body = {
        ticker: draft.ticker, side: draft.side,
        quantity: draft.quantity, price: draft.price,
        fees: 0, tradeDate: draft.tradeDate, note: null,
        source: 'AI_ASSISTED' as const,
      };
      const t = draft.transactionId
        ? await txApi.update(draft.transactionId, body)
        : await txApi.create(body);
      onSaved(t);
    } catch (e) {
      // Unknown ticker, or a timeline conflict — the form surfaces the
      // field-level detail and lets them correct it.
      if (e instanceof ApiError) setParseError(e);
      setEditing(true);
    } finally {
      setSaving(false);
    }
  }

  function onSaved(t: Transaction) {
    setSaved(t);
    setDraft(null);
    setEditing(false);
    setManual(false);
    setText('');
    setWarnings([]);
    setSourceText(null);
  }

  const parseFailed = parseError?.code === 'AI_PARSE_FAILED';
  const aiDown = parseError?.code === 'AI_UNAVAILABLE';

  return (
    <div className="grid-2">
      <div className="stack">
        <Card
          title="Describe the transaction"
          subtitle="Plain English. Copilot fills the form, you confirm it."
          action={<Badge tone="brand" icon="sparkles">AI</Badge>}
        >
          <ChatComposer
            value={text}
            onChange={setText}
            onSubmit={parse}
            placeholder="e.g. bought 15 NVDA at 142 last Tuesday"
            hint="Nothing is saved until you confirm."
            submitLabel={parsing ? 'Parsing…' : 'Parse'}
            suggestions={EXAMPLES}
            onSuggestion={setText}
            disabled={parsing}
          />
        </Card>

        {parseError && (
          <Banner
            tone={aiDown ? 'caution' : 'loss'}
            title={aiDown ? 'Copilot is unavailable right now' : parseFailed ? 'A few details are missing' : 'Parsing failed'}
            action={
              <Button size="sm" variant="secondary" onClick={() => { setManual(true); setParseError(null); }}>
                Enter it manually
              </Button>
            }
          >
            {aiDown
              ? 'The parser is offline. Enter the transaction manually — nothing else is affected.'
              : /* The parser names exactly which fields it still needs — show that,
                   not a generic hint, so the user can just add them and retry. */
                parseError.message}
          </Banner>
        )}

        {draft && !editing && (
          <ParsedTransactionCard
            source={sourceText ?? undefined}
            confidence={confidence ? CONFIDENCE_LABEL[confidence] : undefined}
            fields={[
              { key: 'Action', value: draft.side === 'BUY' ? 'Buy' : 'Sell' },
              { key: 'Symbol', value: draft.ticker },
              { key: 'Quantity', value: quantity(draft.quantity) },
              { key: 'Price', value: money(draft.price) },
              { key: 'Date', value: tradeDate(draft.tradeDate) },
              { key: 'Total', value: money(draft.quantity * draft.price) },
            ]}
            confirmLabel={saving
              ? 'Saving…'
              : draft.transactionId ? 'Confirm & update' : 'Confirm & save'}
            onConfirm={confirmDraft}
            onEdit={() => setEditing(true)}
            onDiscard={() => { setDraft(null); setWarnings([]); setSourceText(null); }}
          />
        )}

        {warnings.length > 0 && draft && !editing && (
          <Banner tone="info" title="How Copilot read that">
            <ul style={{ margin: 0, paddingLeft: 'var(--space-6)' }}>
              {warnings.map((w, i) => <li key={i}>{w}</li>)}
            </ul>
          </Banner>
        )}

        {(editing && draft) && (
          <Card title="Review and save" subtitle="Pre-filled by Copilot — check every field.">
            <TransactionForm
              initial={valuesFromDraft(draft)}
              source="AI_ASSISTED"
              editingId={draft.transactionId ?? undefined}
              submitLabel={draft.transactionId ? 'Confirm & update' : 'Confirm & save'}
              onSaved={onSaved}
              onCancel={() => setEditing(false)}
            />
          </Card>
        )}

        {manual && !draft && (
          <Card title="Enter manually" subtitle="No AI involved.">
            <TransactionForm
              initial={emptyValues()}
              source="MANUAL"
              onSaved={onSaved}
              onCancel={() => setManual(false)}
            />
          </Card>
        )}

        {saved && (
          <Banner
            tone="gain"
            title="Transaction saved"
            action={<Button size="sm" variant="secondary" onClick={() => navigate('/transactions')}>View all</Button>}
          >
            {saved.side === 'BUY' ? 'Bought' : 'Sold'} {quantity(saved.quantity)} {saved.ticker} at{' '}
            {money(saved.price)} on {tradeDate(saved.tradeDate)}. Cost basis and P&L recalculated.
          </Banner>
        )}
      </div>

      <div className="stack">
        <Banner tone="info" title="How parsing works">
          Copilot extracts the fields from your sentence and pre-fills the form. It never writes to
          your portfolio on its own — the transaction is saved only when you confirm.
        </Banner>

        {!manual && !draft && (
          <Card title="Prefer to type it in?" subtitle="The manual form is always available.">
            <Button variant="secondary" fullWidth iconLeft="pencil" onClick={() => setManual(true)}>
              Enter manually
            </Button>
          </Card>
        )}
      </div>
    </div>
  );
}
