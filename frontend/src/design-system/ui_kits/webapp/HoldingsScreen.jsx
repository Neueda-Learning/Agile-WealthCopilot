const { Card, DataTable, TickerAvatar, DeltaValue, Sparkline, Tabs, Badge, Input, Select, Button, EmptyState } = window.WealthCopilotDesignSystem_f10604;

function HoldingsScreen({ tab, onTab }) {
  const d = window.WC_DATA;
  const [q, setQ] = React.useState('');
  const [account, setAccount] = React.useState('All accounts');
  const rows = d.holdings.filter(function (h) {
    const matchQ = !q || (h.symbol + ' ' + h.name).toLowerCase().indexOf(q.toLowerCase()) > -1;
    return matchQ && (account === 'All accounts' || h.account === account);
  });

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-6)' }}>
      <Tabs tabs={[{ id: 'holdings', label: 'Holdings' }, { id: 'transactions', label: 'Transactions' }]} value={tab} onChange={onTab} />

      <div style={{ display: 'flex', alignItems: 'flex-end', gap: 'var(--space-4)' }}>
        <div style={{ width: 260 }}><Input iconLeft="search" placeholder="Search symbol or name" value={q} onChange={function (e) { setQ(e.target.value); }} /></div>
        <div style={{ width: 190 }}><Select value={account} onChange={function (e) { setAccount(e.target.value); }} options={['All accounts', 'Brokerage', 'ISA', 'Retirement']} /></div>
        <span style={{ flex: 1 }} />
        <Button variant="secondary" size="md" iconLeft="download">Export CSV</Button>
      </div>

      {tab === 'holdings' ? (
        <Card flush>
          {rows.length ? (
            <DataTable
              columns={[
                { key: 'symbol', header: 'Position', render: function (r) {
                  return (
                    <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-4)' }}>
                      <TickerAvatar symbol={r.symbol} />
                      <div>
                        <div style={{ fontWeight: 600, color: 'var(--text-primary)' }}>{r.symbol}</div>
                        <div style={{ fontSize: 'var(--fs-sm)', color: 'var(--text-muted)' }}>{r.name}</div>
                      </div>
                    </div>
                  );
                } },
                { key: 'account', header: 'Account', render: function (r) { return <Badge outline>{r.account}</Badge>; } },
                { key: 'spark', header: '30d', width: 96, render: function (r) { return <Sparkline data={r.spark} width={84} height={24} />; } },
                { key: 'qty', header: 'Shares', align: 'right' },
                { key: 'price', header: 'Price', align: 'right', render: function (r) { return d.money(r.price); } },
                { key: 'value', header: 'Market value', align: 'right', render: function (r) { return d.money(r.value); } },
                { key: 'pl', header: 'Total P&L', align: 'right', render: function (r) { return <DeltaValue value={r.pl} percent={r.plPct} currency="USD" size={13} />; } },
                { key: 'day', header: 'Today', align: 'right', render: function (r) { return <DeltaValue value={r.day} percent={r.dayPct} currency="USD" size={13} pill />; } }
              ]}
              rows={rows} onRowClick={function () {}} />
          ) : (
            <EmptyState icon="search-x" title="No positions match that filter">Clear the search or pick a different account.</EmptyState>
          )}
        </Card>
      ) : (
        <Card flush>
          <DataTable
            columns={[
              { key: 'date', header: 'Date' },
              { key: 'side', header: 'Type', render: function (r) { return <Badge tone={r.side === 'Buy' ? 'gain' : 'loss'}>{r.side}</Badge>; } },
              { key: 'symbol', header: 'Symbol', render: function (r) { return <span style={{ fontWeight: 600, color: 'var(--text-primary)' }}>{r.symbol}</span>; } },
              { key: 'account', header: 'Account', render: function (r) { return <Badge outline>{r.account}</Badge>; } },
              { key: 'source', header: 'Entry', render: function (r) { return r.source === 'AI' ? <Badge tone="brand" icon="sparkles">Parsed</Badge> : <span style={{ color: 'var(--text-muted)', fontSize: 'var(--fs-sm)' }}>Manual</span>; } },
              { key: 'qty', header: 'Shares', align: 'right' },
              { key: 'price', header: 'Price', align: 'right', render: function (r) { return d.money(r.price); } },
              { key: 'total', header: 'Total', align: 'right', render: function (r) { return d.money(r.qty * r.price); } }
            ]}
            rows={d.transactions.filter(function (t) { return account === 'All accounts' || t.account === account; })} />
        </Card>
      )}
    </div>
  );
}

Object.assign(window, { HoldingsScreen });
