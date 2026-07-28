import React from 'react';

export function Sparkline({
  data = [], width = 96, height = 28, tone, strokeWidth = 1.5, fill = false, className = '', ...rest
}) {
  if (!data.length) return <svg width={width} height={height} className={className} />;
  const min = Math.min.apply(null, data);
  const max = Math.max.apply(null, data);
  const span = max - min || 1;
  const step = data.length > 1 ? width / (data.length - 1) : width;
  const pad = strokeWidth;
  const pts = data.map(function (v, i) {
    const x = i * step;
    const y = pad + (1 - (v - min) / span) * (height - pad * 2);
    return x.toFixed(2) + ',' + y.toFixed(2);
  });
  const dir = tone || (data[data.length - 1] >= data[0] ? 'gain' : 'loss');
  const color = dir === 'gain' ? 'var(--gain-600)' : dir === 'loss' ? 'var(--loss-600)' : 'var(--ink-400)';
  return (
    <svg width={width} height={height} viewBox={'0 0 ' + width + ' ' + height}
      preserveAspectRatio="none" className={className} aria-hidden="true" {...rest}>
      {fill ? (
        <polygon points={'0,' + height + ' ' + pts.join(' ') + ' ' + width + ',' + height}
          fill={color} opacity="0.1" />
      ) : null}
      <polyline points={pts.join(' ')} fill="none" stroke={color}
        strokeWidth={strokeWidth} strokeLinejoin="round" strokeLinecap="round" />
    </svg>
  );
}
