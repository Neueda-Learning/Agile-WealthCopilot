window.WC_DATA = (function () {
  const series = function (start, drift, n) {
    const out = []; let v = start;
    for (let i = 0; i < n; i++) { v = v * (1 + (Math.sin(i * 1.7) * 0.012) + drift); out.push(Math.round(v * 100) / 100); }
    return out;
  };
  const holdings = [
    { id: 'voo', symbol: 'VOO', name: 'Vanguard S&P 500 ETF', account: 'Brokerage', qty: 48, price: 519.13, cost: 431.02, sector: 'US equity' },
    { id: 'aapl', symbol: 'AAPL', name: 'Apple Inc.', account: 'Brokerage', qty: 120, price: 182.4, cost: 141.86, sector: 'US equity' },
    { id: 'msft', symbol: 'MSFT', name: 'Microsoft Corp.', account: 'Brokerage', qty: 36, price: 450.12, cost: 322.4, sector: 'US equity' },
    { id: 'vxus', symbol: 'VXUS', name: 'Vanguard Total Intl Stock', account: 'ISA', qty: 310, price: 61.88, cost: 55.12, sector: 'Intl equity' },
    { id: 'nvda', symbol: 'NVDA', name: 'NVIDIA Corp.', account: 'Brokerage', qty: 40, price: 124.6, cost: 61.9, sector: 'US equity' },
    { id: 'bnd', symbol: 'BND', name: 'Vanguard Total Bond Market', account: 'Retirement', qty: 420, price: 72.94, cost: 76.1, sector: 'Bonds' },
    { id: 'schd', symbol: 'SCHD', name: 'Schwab US Dividend Equity', account: 'Retirement', qty: 190, price: 79.22, cost: 68.4, sector: 'US equity' },
    { id: 'gld', symbol: 'GLD', name: 'SPDR Gold Shares', account: 'Brokerage', qty: 55, price: 214.8, cost: 188.2, sector: 'Commodities' }
  ].map(function (h, i) {
    const value = h.qty * h.price;
    const basis = h.qty * h.cost;
    const dayPct = [0.42, -1.08, 0.6, 0.18, 2.31, -0.12, 0.34, -0.51][i];
    return Object.assign({}, h, {
      value: value, basis: basis, pl: value - basis, plPct: (value / basis - 1) * 100,
      dayPct: dayPct, day: value * dayPct / 100,
      spark: series(100, dayPct / 900, 24)
    });
  });
  const transactions = [
    { id: 't1', date: 'Jul 26, 2026', side: 'Buy', symbol: 'AAPL', qty: 12, price: 182.4, account: 'Brokerage', source: 'AI' },
    { id: 't2', date: 'Jul 21, 2026', side: 'Buy', symbol: 'VOO', qty: 4, price: 512.08, account: 'Brokerage', source: 'Manual' },
    { id: 't3', date: 'Jul 14, 2026', side: 'Sell', symbol: 'NVDA', qty: 10, price: 118.44, account: 'Brokerage', source: 'Manual' },
    { id: 't4', date: 'Jul 02, 2026', side: 'Buy', symbol: 'BND', qty: 60, price: 73.1, account: 'Retirement', source: 'AI' },
    { id: 't5', date: 'Jun 28, 2026', side: 'Buy', symbol: 'VXUS', qty: 45, price: 60.02, account: 'ISA', source: 'Manual' }
  ];
  const total = holdings.reduce(function (s, h) { return s + h.value; }, 0);
  const basis = holdings.reduce(function (s, h) { return s + h.basis; }, 0);
  const day = holdings.reduce(function (s, h) { return s + h.day; }, 0);
  const alloc = {};
  holdings.forEach(function (h) { alloc[h.sector] = (alloc[h.sector] || 0) + h.value; });
  return {
    holdings: holdings, transactions: transactions,
    total: total, basis: basis, day: day,
    dayPct: day / (total - day) * 100,
    pl: total - basis, plPct: (total / basis - 1) * 100,
    allocation: Object.keys(alloc).map(function (k) { return { label: k, value: alloc[k] }; }),
    portfolioSeries: series(196000, 0.0021, 60),
    money: function (n, digits) {
      return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD',
        minimumFractionDigits: digits == null ? 2 : digits, maximumFractionDigits: digits == null ? 2 : digits }).format(n);
    }
  };
})();
