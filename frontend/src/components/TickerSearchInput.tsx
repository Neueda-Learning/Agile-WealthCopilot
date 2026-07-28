import { useEffect, useRef, useState } from 'react';
import { Input, TickerAvatar } from '../design-system';
import { market } from '../api/endpoints';
import type { SymbolSearchResult } from '../types/api';

interface Props {
  value: string;
  onChange: (ticker: string) => void;
  onResolved?: (r: SymbolSearchResult) => void;
  label?: string;
  error?: string;
}

/**
 * Ticker field with search-as-you-type. Debounced at 300 ms per the design
 * system's layout notes — symbol search hits Twelve Data per request, so
 * every keystroke firing would burn the rate limit.
 */
export default function TickerSearchInput({ value, onChange, onResolved, label = 'Symbol', error }: Props) {
  const [results, setResults] = useState<SymbolSearchResult[]>([]);
  const [open, setOpen] = useState(false);
  const [searchFailed, setSearchFailed] = useState(false);
  const boxRef = useRef<HTMLDivElement>(null);
  // Set when a result is picked, so re-rendering with the new value does not
  // immediately reopen the menu.
  const justPicked = useRef(false);

  useEffect(() => {
    if (justPicked.current) { justPicked.current = false; return; }
    const q = value.trim();
    if (q.length < 1) { setResults([]); return; }

    const ctl = new AbortController();
    const timer = setTimeout(() => {
      market.search(q, ctl.signal)
        .then((r) => { setResults(r); setOpen(r.length > 0); setSearchFailed(false); })
        .catch((e) => { if (e.name !== 'AbortError') { setResults([]); setSearchFailed(true); } });
    }, 300);

    return () => { clearTimeout(timer); ctl.abort(); };
  }, [value]);

  useEffect(() => {
    function onDocClick(e: MouseEvent) {
      if (boxRef.current && !boxRef.current.contains(e.target as Node)) setOpen(false);
    }
    document.addEventListener('mousedown', onDocClick);
    return () => document.removeEventListener('mousedown', onDocClick);
  }, []);

  function pick(r: SymbolSearchResult) {
    justPicked.current = true;
    onChange(r.ticker);
    onResolved?.(r);
    setOpen(false);
  }

  return (
    <div className="ticker-search" ref={boxRef}>
      <Input
        label={label}
        iconLeft="search"
        placeholder="e.g. NVDA"
        autoComplete="off"
        value={value}
        onChange={(e) => onChange(e.target.value.toUpperCase())}
        onFocus={() => setOpen(results.length > 0)}
        error={error}
        hint={searchFailed && !error ? 'Symbol search is unavailable — type the exact ticker.' : undefined}
      />
      {open && (
        <div className="ticker-search__menu">
          {results.map((r) => (
            <button
              key={`${r.ticker}-${r.exchange}`}
              type="button"
              className="ticker-search__item"
              onClick={() => pick(r)}
            >
              <TickerAvatar symbol={r.ticker} size={28} />
              <span style={{ minWidth: 0 }}>
                <span style={{ display: 'block', fontWeight: 600, color: 'var(--text-primary)' }}>
                  {r.ticker}
                </span>
                <span style={{ display: 'block', fontSize: 'var(--fs-sm)', color: 'var(--text-muted)' }}>
                  {r.name} · {r.exchange}
                </span>
              </span>
              {r.currency !== 'USD' && (
                <span style={{ marginLeft: 'auto', fontSize: 'var(--fs-xs)', color: 'var(--amber-700)' }}>
                  {r.currency} — not supported
                </span>
              )}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
