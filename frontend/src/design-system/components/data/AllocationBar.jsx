import React from 'react';

const PALETTE = ['var(--chart-1)', 'var(--chart-2)', 'var(--chart-3)', 'var(--chart-4)', 'var(--chart-5)', 'var(--chart-6)'];

export function AllocationBar({ segments = [], showLegend = true, className = '', ...rest }) {
  const total = segments.reduce(function (s, x) { return s + x.value; }, 0) || 1;
  return (
    <div className={className} {...rest}>
      <div className="wc-alloc">
        {segments.map(function (s, i) {
          return <span key={s.label} className="wc-alloc__seg"
            style={{ width: (s.value / total * 100) + '%', background: s.color || PALETTE[i % PALETTE.length] }} />;
        })}
      </div>
      {showLegend ? (
        <div className="wc-alloc-legend">
          {segments.map(function (s, i) {
            return (
              <span key={s.label} className="wc-alloc-legend__item">
                <span className="wc-alloc-legend__dot" style={{ background: s.color || PALETTE[i % PALETTE.length] }} />
                {s.label}
                <span className="wc-alloc-legend__val">{(s.value / total * 100).toFixed(1)}%</span>
              </span>
            );
          })}
        </div>
      ) : null}
    </div>
  );
}
