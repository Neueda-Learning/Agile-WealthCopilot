import { useEffect, useRef, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import {
  Badge, Banner, Button, Card, ChatComposer, ChatMessage, Icon, ParsedTransactionCard,
} from '../design-system';
import { ApiError } from '../api/client';
import { ai, transactions as txApi } from '../api/endpoints';
import Markdown from '../components/Markdown';
import { money, quantity, tradeDate } from '../lib/format';
import type { ToolCall, TransactionDraft } from '../types/api';

/** Which conversation to resume; the transcript itself lives in the database. */
const CONVERSATION_KEY = 'wc.conversationId';

interface Turn {
  role: 'user' | 'assistant';
  text: string;
  toolCalls?: ToolCall[];
  draft?: TransactionDraft | null;
  /** Set once the user confirms a draft, replacing the card with a receipt. */
  savedNote?: string;
}

const SUGGESTIONS = [
  'What is my total unrealized gain?',
  'Which holding has lost me the most?',
  'How much did I invest last month?',
];

/** "get_holdings · 12ms" — the disclosure of what the agent actually read. */
function toolLabel(calls: ToolCall[]): string {
  return calls.map((c) => `${c.name} · ${c.durationMs}ms`).join('  ');
}

export default function CopilotPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const seed = (location.state as { seed?: string } | null)?.seed;

  const [turns, setTurns] = useState<Turn[]>([]);
  const [conversationId, setConversationId] = useState<number | undefined>(undefined);
  const [input, setInput] = useState('');
  const [busy, setBusy] = useState(false);
  const [restoring, setRestoring] = useState(!seed);
  const [savingDraft, setSavingDraft] = useState<number | null>(null);
  const [error, setError] = useState<ApiError | null>(null);
  const endRef = useRef<HTMLDivElement>(null);
  const seeded = useRef(false);

  async function ask(message: string) {
    const text = message.trim();
    if (!text || busy) return;

    setTurns((t) => [...t, { role: 'user', text }]);
    setInput('');
    setBusy(true);
    setError(null);

    try {
      const res = await ai.chat(text, conversationId);
      setConversationId(res.conversationId);
      localStorage.setItem(CONVERSATION_KEY, String(res.conversationId));
      setTurns((t) => [...t, {
        role: 'assistant', text: res.reply,
        toolCalls: res.toolCalls, draft: res.draftTransaction,
      }]);
    } catch (e) {
      if (e instanceof ApiError) setError(e);
    } finally {
      setBusy(false);
    }
  }

  // A prompt chip on the dashboard opens this page with a question ready to go.
  useEffect(() => {
    if (seed && !seeded.current) {
      seeded.current = true;
      void ask(seed);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [seed]);

  // Leaving the page unmounts this component, so the transcript is reloaded
  // from the server rather than kept in memory — it also survives a refresh.
  useEffect(() => {
    if (seed) return;
    const stored = localStorage.getItem(CONVERSATION_KEY);
    if (!stored) {
      setRestoring(false);
      return;
    }

    let cancelled = false;
    (async () => {
      try {
        const history = await ai.messages(Number(stored));
        if (cancelled) return;
        setTurns(history
          .filter((m) => m.role === 'USER' || m.role === 'ASSISTANT')
          .map((m) => ({ role: m.role === 'USER' ? 'user' : 'assistant', text: m.content })));
        setConversationId(Number(stored));
      } catch {
        // Deleted, or belongs to a different account — start clean.
        localStorage.removeItem(CONVERSATION_KEY);
      } finally {
        if (!cancelled) setRestoring(false);
      }
    })();
    return () => { cancelled = true; };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [seed]);

  useEffect(() => {
    endRef.current?.scrollIntoView({ behavior: 'smooth', block: 'end' });
  }, [turns.length, busy]);

  /**
   * The agent prepares changes but never writes them; this is the human
   * confirmation step that actually commits one. Failures fall back to the
   * form, which can show which field the backend rejected.
   */
  async function confirmDraft(index: number, draft: TransactionDraft) {
    if (savingDraft !== null) return;
    setSavingDraft(index);
    setError(null);
    try {
      const body = {
        ticker: draft.ticker, side: draft.side,
        quantity: draft.quantity, price: draft.price,
        fees: 0, tradeDate: draft.tradeDate, note: null,
        source: 'AI_ASSISTED' as const,
      };
      const saved = draft.transactionId
        ? await txApi.update(draft.transactionId, body)
        : await txApi.create(body);
      const verb = draft.transactionId ? 'Updated' : (saved.side === 'BUY' ? 'Bought' : 'Sold');
      setTurns((prev) => prev.map((x, xi) => xi === index
        ? {
          ...x,
          draft: null,
          savedNote: `${verb} ${quantity(saved.quantity)} ${saved.ticker} at `
            + `${money(saved.price)} on ${tradeDate(saved.tradeDate)}.`,
        }
        : x));
    } catch (e) {
      if (e instanceof ApiError) setError(e);
      navigate('/log', { state: { draft } });
    } finally {
      setSavingDraft(null);
    }
  }

  function newChat() {
    setTurns([]);
    setConversationId(undefined);
    localStorage.removeItem(CONVERSATION_KEY);
    setError(null);
    setInput('');
  }

  return (
    <div className="stack" style={{ maxWidth: 860, margin: '0 auto' }}>
      <Banner
        tone="info"
        title="Copilot never saves anything on its own"
        action={turns.length > 0
          ? <Button size="sm" variant="secondary" iconLeft="rotate-ccw" onClick={newChat}>New chat</Button>
          : undefined}
      >
        It answers questions about your portfolio and the markets, and can prepare a new entry or a
        change to an existing one — but nothing is written until you confirm it. It cannot place
        trades or move money.
      </Banner>

      <div className="chat-log">
        {turns.length === 0 && !busy && !restoring && (
          <Card>
            <div className="row">
              <Badge tone="brand" icon="sparkles">Copilot</Badge>
              <span style={{ color: 'var(--text-secondary)', fontSize: 'var(--fs-sm)' }}>
                Ask about your portfolio or anything else in the markets. Start with a prompt below.
              </span>
            </div>
          </Card>
        )}

        {turns.map((t, i) => (
          t.role === 'user' ? (
            <ChatMessage key={i} role="user">{t.text}</ChatMessage>
          ) : (
            <div key={i} className="stack-sm">
              <ChatMessage tool={t.toolCalls?.length ? toolLabel(t.toolCalls) : undefined}>
                <Markdown>{t.text}</Markdown>
              </ChatMessage>
              {t.draft && (
                <ParsedTransactionCard
                  confidence="high"
                  fields={[
                    { key: 'Action', value: t.draft.side === 'BUY' ? 'Buy' : 'Sell' },
                    { key: 'Symbol', value: t.draft.ticker },
                    { key: 'Quantity', value: quantity(t.draft.quantity) },
                    { key: 'Price', value: money(t.draft.price) },
                    { key: 'Date', value: tradeDate(t.draft.tradeDate) },
                    { key: 'Total', value: money(t.draft.quantity * t.draft.price) },
                  ]}
                  confirmLabel={savingDraft === i
                    ? 'Saving…'
                    : t.draft.transactionId ? 'Confirm & update' : 'Confirm & save'}
                  onConfirm={() => confirmDraft(i, t.draft!)}
                  onEdit={() => navigate('/log', { state: { draft: t.draft } })}
                  onDiscard={() => setTurns((prev) => prev.map((x, xi) => xi === i ? { ...x, draft: null } : x))}
                />
              )}
              {t.savedNote && <Banner tone="gain" title="Saved">{t.savedNote}</Banner>}
            </div>
          )
        ))}

        {busy && (
          <ChatMessage>
            <span className="row" style={{ color: 'var(--text-muted)' }}>
              <Icon name="sparkles" size={14} /> Reading your portfolio…
            </span>
          </ChatMessage>
        )}

        <div ref={endRef} />
      </div>

      {error && (
        <Banner
          tone={error.code === 'AI_UNAVAILABLE' ? 'caution' : 'loss'}
          title={error.code === 'AI_UNAVAILABLE' ? 'Copilot is unavailable right now' : 'Copilot could not answer'}
        >
          {error.code === 'AI_UNAVAILABLE'
            ? 'The assistant is offline. Your portfolio data is unaffected — try again shortly.'
            : error.message}
        </Banner>
      )}

      <ChatComposer
        value={input}
        onChange={setInput}
        onSubmit={() => ask(input)}
        placeholder="Ask about your portfolio…"
        hint="Nothing is saved until you confirm it. Copilot cannot place trades."
        submitLabel="Ask"
        suggestions={turns.length === 0 ? SUGGESTIONS : []}
        onSuggestion={ask}
        disabled={busy}
      />
    </div>
  );
}
