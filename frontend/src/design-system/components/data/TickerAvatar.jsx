import React from 'react';

export function TickerAvatar({ symbol = '', size = 34, className = '', ...rest }) {
  return (
    <span className={'wc-ticker ' + className}
      style={{ width: size, height: size, fontSize: Math.max(10, Math.round(size * 0.32)) }} {...rest}>
      {symbol.slice(0, 4)}
    </span>
  );
}
