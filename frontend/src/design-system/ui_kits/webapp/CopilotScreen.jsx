const { Card, ChatMessage, ChatComposer, Banner, DeltaValue, AllocationBar, Badge } = window.WealthCopilotDesignSystem_f10604;

const ANSWERS = {
  'what is my biggest position?': {
    tool: 'read_holdings(sort="value", limit=1)',
    body: function (d) {
      const top = d.holdings.slice().sort(function (a, b) { return b.value - a.value; })[0];
      return (
        <>
          Your largest position is <strong>{top.symbol}</strong> — {d.money(top.value)}, or {(top.value / d.total * 100).toFixed(1)}% of the portfolio.
          Unrealised P&L on it is <DeltaValue value={top.pl} percent={top.plPct} currency="USD" size={14} />.
        </>
      );
    }
  },
  'how concentrated am i in tech?': {
    tool: 'read_holdings() → group_by(sector)',
    body: function (d) {
      return (
        <>
          US equity is {(d.allocation[0].value / d.total * 100).toFixed(1)}% of your {d.money(d.total, 0)} portfolio.
          <div style={{ marginTop: 'var(--space-4)' }}><AllocationBar segments={d.allocation} /></div>
        </>
      );
    }
  },
  'what did i buy this month?': {
    tool: 'read_transactions(from="2026-07-01")',
    body: function (d) {
      return (
        <>
          Four buys in July: AAPL, VOO, BND and a partial VXUS add — {d.money(d.transactions.filter(function (t) { return t.side === 'Buy'; }).reduce(function (s, t) { return s + t.qty * t.price; }, 0))} deployed. One sell: 10 NVDA on Jul 14.
        </>
      );
    }
  }
};

function CopilotScreen({ seed }) {
  const d = window.WC_DATA;
  const [turns, setTurns] = React.useState(function () {
    return seed ? [{ role: 'user', text: seed }, Object.assign({ role: 'assistant' }, ANSWERS[seed.toLowerCase()] || ANSWERS['what is my biggest position?'])] : [];
  });
  const [q, setQ] = React.useState('');
  const endRef = React.useRef(null);

  const ask = function (text) {
    const key = text.toLowerCase().trim();
    const a = ANSWERS[key] || {
      tool: 'read_portfolio_summary()',
      body: function (dd) {
        return (
          <>
            Your portfolio is worth {dd.money(dd.total)}, up <DeltaValue value={dd.pl} percent={dd.plPct} currency="USD" size={14} showArrow={false} /> since you started tracking.
            Ask about a position, a sector, or a date range for more detail.
          </>
        );
      }
    };
    setTurns(turns.concat([{ role: 'user', text: text }, Object.assign({ role: 'assistant' }, a)]));
    setQ('');
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--section-gap)', maxWidth: 860, margin: '0 auto' }}>
      <Banner tone="info" title="Copilot is read-only">
        It can read your holdings and transactions to answer questions. It cannot place trades, move money, or edit your records.
      </Banner>

      <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-6)', minHeight: 220 }}>
        {turns.length === 0 ? (
          <Card>
            <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-4)' }}>
              <Badge tone="brand" icon="sparkles">Copilot</Badge>
              <span style={{ color: 'var(--text-secondary)', fontSize: 'var(--fs-sm)' }}>Ask anything about your own portfolio. Start with one of the prompts below.</span>
            </div>
          </Card>
        ) : null}
        {turns.map(function (t, i) {
          return t.role === 'user'
            ? <ChatMessage key={i} role="user">{t.text}</ChatMessage>
            : <ChatMessage key={i} tool={t.tool}>{t.body(d)}</ChatMessage>;
        })}
        <div ref={endRef} />
      </div>

      <ChatComposer
        value={q} onChange={setQ} onSubmit={function () { if (q.trim()) ask(q); }}
        suggestions={['What is my biggest position?', 'How concentrated am I in tech?', 'What did I buy this month?']}
        onSuggestion={ask} />
    </div>
  );
}

Object.assign(window, { CopilotScreen });
