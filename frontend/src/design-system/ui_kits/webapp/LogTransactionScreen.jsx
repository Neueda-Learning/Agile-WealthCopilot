const { Card, ChatComposer, ParsedTransactionCard, Input, Select, Checkbox, Button, Banner, Badge, DataTable, Icon } = window.WealthCopilotDesignSystem_f10604;

const SAMPLE = 'bought 12 AAPL at 182.40 yesterday';

function parseText(text) {
  const t = text.toLowerCase();
  const side = /sold|sell/.test(t) ? 'Sell' : 'Buy';
  const sym = (text.match(/\b[A-Z]{2,5}\b/) || ['AAPL'])[0];
  const nums = (t.match(/\d+(\.\d+)?/g) || []).map(Number);
  const qty = nums[0] || 1;
  const price = nums[1] || 100;
  return { side: side, symbol: sym, qty: qty, price: price, date: /yesterday/.test(t) ? 'Jul 26, 2026' : 'Jul 27, 2026' };
}

function LogTransactionScreen({ onSaved, saved }) {
  const d = window.WC_DATA;
  const [text, setText] = React.useState(SAMPLE);
  const [parsed, setParsed] = React.useState(null);
  const [editing, setEditing] = React.useState(false);

  const run = function () { if (text.trim()) { setParsed(parseText(text)); setEditing(false); } };

  return (
    <div style={{ display: 'grid', gridTemplateColumns: '1.15fr 1fr', gap: 'var(--section-gap)', alignItems: 'start' }}>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--section-gap)' }}>
        <Card title="Describe the transaction" subtitle="Plain English — Copilot fills the form, you confirm it."
          action={<Badge tone="brand" icon="sparkles">AI</Badge>}>
          <ChatComposer
            value={text} onChange={setText} onSubmit={run}
            placeholder="e.g. sold 20 NVDA at 118.44 last Tuesday"
            hint="Nothing is saved until you press Confirm."
            submitLabel="Parse"
            suggestions={['bought 4 VOO at 512.08', 'sold 20 NVDA at 118.44 on Jul 14']}
            onSuggestion={function (s) { setText(s); }} />
        </Card>

        {parsed && !editing ? (
          <ParsedTransactionCard
            source={text}
            confidence={parsed.qty && parsed.price ? 'high' : 'low'}
            fields={[
              { key: 'Action', value: parsed.side },
              { key: 'Symbol', value: parsed.symbol },
              { key: 'Quantity', value: String(parsed.qty) },
              { key: 'Price', value: d.money(parsed.price) },
              { key: 'Date', value: parsed.date },
              { key: 'Total', value: d.money(parsed.qty * parsed.price) }
            ]}
            onConfirm={function () { onSaved(parsed); setParsed(null); }}
            onEdit={function () { setEditing(true); }}
            onDiscard={function () { setParsed(null); }} />
        ) : null}

        {editing && parsed ? (
          <Card title="Review and edit" subtitle="Fields pre-filled by Copilot">
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 'var(--space-5)' }}>
              <Select label="Action" defaultValue={parsed.side} options={['Buy', 'Sell']} />
              <Input label="Symbol" defaultValue={parsed.symbol} />
              <Input label="Account" defaultValue="Brokerage" />
              <Input label="Quantity" numeric defaultValue={String(parsed.qty)} />
              <Input label="Price per share" prefix="$" numeric defaultValue={parsed.price.toFixed(2)} />
              <Input label="Date" defaultValue={parsed.date} iconLeft="calendar" />
            </div>
            <div style={{ marginTop: 'var(--space-6)', display: 'flex', alignItems: 'center', gap: 'var(--space-5)' }}>
              <Checkbox label="Reinvest dividends" defaultChecked />
              <span style={{ flex: 1 }} />
              <Button variant="ghost" onClick={function () { setEditing(false); }}>Back</Button>
              <Button onClick={function () { onSaved(parsed); setParsed(null); setEditing(false); }}>Save transaction</Button>
            </div>
          </Card>
        ) : null}

        {saved ? (
          <Banner tone="gain" title="Transaction saved">
            {saved.side} {saved.qty} {saved.symbol} at {d.money(saved.price)} — cost basis and P&L recalculated.
          </Banner>
        ) : null}
      </div>

      <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--section-gap)' }}>
        <Banner tone="info" title="How parsing works">
          Copilot extracts fields from your sentence and pre-fills the form. It never writes to your portfolio on its own.
        </Banner>
        <Card title="Recent entries" subtitle="Last 5" flush>
          <DataTable compact
            columns={[
              { key: 'date', header: 'Date' },
              { key: 'side', header: 'Type', render: function (r) { return <Badge tone={r.side === 'Buy' ? 'gain' : 'loss'}>{r.side}</Badge>; } },
              { key: 'symbol', header: 'Symbol' },
              { key: 'total', header: 'Total', align: 'right', render: function (r) { return d.money(r.qty * r.price); } }
            ]}
            rows={d.transactions} />
        </Card>
      </div>
    </div>
  );
}

Object.assign(window, { LogTransactionScreen });
