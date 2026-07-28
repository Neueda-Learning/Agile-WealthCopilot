import React from 'react';
import { Icon } from '../core/Icon.jsx';

function fmt(n, opts) {
  const o = opts || {};
  return new Intl.NumberFormat('en-US', {
    style: o.currency ? 'currency' : 'decimal',
    currency: o.currency || 'USD',
    minimumFractionDigits: o.digits == null ? 2 : o.digits,
    maximumFractionDigits: o.digits == null ? 2 : o.digits,
  }).format(n);
}

export function DeltaValue({
  value = 0, percent, currency, digits, showArrow = true,
  pill = false, size, className = '', ...rest
}) {
  const dir = value > 0 ? 'gain' : value < 0 ? 'loss' : 'flat';
  const sign = value > 0 ? '+' : value < 0 ? '-' : '';
  const abs = Math.abs(value);
  const main = percent != null && value === 0
    ? ''
    : sign + fmt(abs, { currency: currency, digits: digits });
  const pct = percent == null ? null
    : (main ? ' (' : sign) + Math.abs(percent).toFixed(2) + '%' + (main ? ')' : '');
  const cls = ['wc-delta', 'wc-delta--' + dir, pill ? 'wc-delta--pill' : '', className]
    .filter(Boolean).join(' ');
  return (
    <span className={cls} style={size ? { fontSize: size } : undefined} {...rest}>
      {showArrow && dir !== 'flat'
        ? <Icon name={dir === 'gain' ? 'arrow-up-right' : 'arrow-down-right'} size={size ? size * 0.9 : 14} />
        : null}
      {main}{pct}
    </span>
  );
}
