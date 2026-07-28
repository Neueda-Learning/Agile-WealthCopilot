import React from 'react';
import { Icon } from './Icon.jsx';

export function Badge({ tone = 'neutral', outline = false, icon, children, className = '', ...rest }) {
  const cls = ['wc-badge', 'wc-badge--' + (outline ? 'outline' : tone), className].filter(Boolean).join(' ');
  return (
    <span className={cls} {...rest}>
      {icon ? <Icon name={icon} size={11} /> : null}
      {children}
    </span>
  );
}
