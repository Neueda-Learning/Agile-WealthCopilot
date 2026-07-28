const { Icon, Card, Stat, DeltaValue, Sparkline, AllocationBar, DataTable, TickerAvatar, Badge, SegmentedControl, Button, Banner } = window.WealthCopilotDesignSystem_f10604;

function DashboardScreen({ onOpenHolding, onAsk }) {
  const d = window.WC_DATA;
  const [range, setRange] = React.useState('1Y');
  const movers = d.holdings.slice().sort(function (a, b) { return Math.abs(b.dayPct) - Math.abs(a.dayPct); }).slice(0, 4);
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--section-gap)' }}>
      <Banner tone="caution" title="Prices are 15 minutes delayed">Cached at 4:02pm ET on Jul 27, 2026. Values may differ from your broker.</Banner>

      <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr', gap: 'var(--section-gap)' }}>
        <Card>
          <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', gap: 'var(--space-6)' }}>
            <Stat label="Total value" size="xl" value={d.money(d.total)}
              foot={<><DeltaValue value={d.day} percent={d.dayPct} currency="USD" size={13} /><span>today</span></>} />
            <SegmentedControl options={['1D', '1W', '1M', '1Y', 'ALL']} value={range} onChange={setRange} />
          </div>
          <div style={{ marginTop: 'var(--space-6)' }}>
            <Sparkline data={d.portfolioSeries} width={720} height={168} strokeWidth={2} fill style={{ width: '100%' }} />
          </div>
        </Card>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--section-gap)' }}>
          <Card>
            <Stat label="Total return" size="lg" value={d.money(d.pl)}
              foot={<><DeltaValue value={d.pl} percent={d.plPct} currency="USD" size={13} showArrow={false} /><span>all time</span></>} />
          </Card>
          <Card title="Allocation" subtitle={d.holdings.length + ' positions'}>
            <AllocationBar segments={d.allocation} />
          </Card>
        </div>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr', gap: 'var(--section-gap)' }}>
        <Card title="Top movers" subtitle="Today" flush
          action={<Button variant="ghost" size="sm" iconRight="chevron-right" onClick={function () { onOpenHolding(); }}>All holdings</Button>}>
          <DataTable
            compact
            onRowClick={onOpenHolding}
            columns={[
              { key: 'symbol', header: 'Position', render: function (r) {
                return (
                  <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-4)' }}>
                    <TickerAvatar symbol={r.symbol} size={30} />
                    <div>
                      <div style={{ fontWeight: 600, color: 'var(--text-primary)' }}>{r.symbol}</div>
                      <div style={{ fontSize: 'var(--fs-sm)', color: 'var(--text-muted)' }}>{r.name}</div>
                    </div>
                  </div>
                );
              } },
              { key: 'spark', header: '', width: 90, render: function (r) { return <Sparkline data={r.spark} width={80} height={22} />; } },
              { key: 'value', header: 'Market value', align: 'right', render: function (r) { return d.money(r.value); } },
              { key: 'day', header: 'Today', align: 'right', render: function (r) { return <DeltaValue value={r.day} percent={r.dayPct} currency="USD" size={13} pill />; } }
            ]}
            rows={movers} />
        </Card>

        <Card title="Ask Copilot" subtitle="Read-only. It cannot trade."
          action={<Badge tone="brand" icon="sparkles">Beta</Badge>}>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-3)' }}>
            {['What is my biggest position?', 'How concentrated am I in tech?', 'What did I buy this month?'].map(function (q) {
              return (
                <button key={q} type="button" className="wc-btn wc-btn--secondary wc-btn--sm"
                  style={{ justifyContent: 'space-between', width: '100%', fontWeight: 500 }}
                  onClick={function () { onAsk(q); }}>
                  {q}<Icon name="arrow-right" size={14} />
                </button>
              );
            })}
          </div>
        </Card>
      </div>
    </div>
  );
}

Object.assign(window, { DashboardScreen });
