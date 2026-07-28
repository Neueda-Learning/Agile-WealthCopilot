import React from 'react';
import { Icon } from './Icon.jsx';

export function Button({
  variant = 'primary', size = 'md', iconLeft, iconRight,
  fullWidth = false, disabled = false, children, className = '', ...rest
}) {
  const cls = ['wc-btn', 'wc-btn--' + variant, 'wc-btn--' + size,
    fullWidth ? 'wc-btn--block' : '', className].filter(Boolean).join(' ');
  const gs = size === 'sm' ? 14 : 16;
  return (
    <button type="button" className={cls} disabled={disabled} {...rest}>
      {iconLeft ? <Icon name={iconLeft} size={gs} /> : null}
      {children}
      {iconRight ? <Icon name={iconRight} size={gs} /> : null}
    </button>
  );
}
