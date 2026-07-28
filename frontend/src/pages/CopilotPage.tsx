import { useEffect, useRef, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import {
  Badge, Banner, Button, Card, ChatComposer, ChatMessage, Icon, ParsedTransactionCard,
} from '../design-system';
import { ApiError } from '../api/client';
import { ai } from '../api/endpoints';
import { money, quantity, tradeDate } from '../lib/format';
import type { ToolCall, TransactionDraft } from '../types/api';

interface Turn {
  role: 'user' | 'assistant';
  text: string;
  toolCalls?: ToolCall[];
  draft?: TransactionDraft | null;
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

  useEffect(() => {
    endRef.current?.scrollIntoView({ behavior: 'smooth', block: 'end' });
  }, [turns.length, busy]);

  function newChat() {
    setTurns([]);
    setConversationId(undefined);
    setError(null);
    setInput('');
  }

  return (
    <div className="stack" style={{ maxWidth: 860, margin: '0 auto' }}>
      <Banner
        tone="info"
        title="Copilot is read-only"
        action={turns.length > 0
          ? <Button size="sm" variant="secondary" iconLeft="rotate-ccw" onClick={newChat}>New chat</Button>
          : undefined}
      >
        It reads your holdings and transactions to answer questions. It cannot place trades, move
        money, or edit your records.
      </Banner>

      <div className="chat-log">
        {turns.length === 0 && !busy && (
          <Card>
            <div className="row">
              <Badge tone="brand" icon="sparkles">Copilot</Badge>
              <span style={{ color: 'var(--text-secondary)', fontSize: 'var(--fs-sm)' }}>
                Ask anything about your own portfolio. Start with one of the prompts below.
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
                {t.text}
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
                  confirmLabel="Open in form"
                  onConfirm={() => navigate('/log', { state: { draft: t.draft } })}
                  onEdit={() => navigate('/log', { state: { draft: t.draft } })}
                  onDiscard={() => setTurns((prev) => prev.map((x, xi) => xi === i ? { ...x, draft: null } : x))}
                />
              )}
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
        hint="Copilot reads your holdings. It cannot place trades."
        submitLabel="Ask"
        suggestions={turns.length === 0 ? SUGGESTIONS : []}
        onSuggestion={ask}
        disabled={busy}
      />
    </div>
  );
}
