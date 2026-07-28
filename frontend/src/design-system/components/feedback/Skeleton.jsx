import React from 'react';

export function Skeleton({ width = '100%', height = 12, radius, lines = 1, className = '', style, ...rest }) {
  if (lines > 1) {
    return (
      <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-3)' }}>
        {Array.from({ length: lines }).map(function (_, i) {
          return <span key={i} className={'wc-skel ' + className}
            style={{ width: i === lines - 1 ? '60%' : width, height: height, borderRadius: radius, ...style }} />;
        })}
      </div>
    );
  }
  return <span className={'wc-skel ' + className} style={{ display: 'block', width: width, height: height, borderRadius: radius, ...style }} {...rest} />;
}
