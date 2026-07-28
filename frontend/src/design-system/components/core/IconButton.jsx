import React from 'react';
import { Icon } from './Icon.jsx';

export function IconButton({ icon, label, size = 'md', bordered = false, className = '', ...rest }) {
  const cls = ['wc-iconbtn', 'wc-iconbtn--' + size,
    bordered ? 'wc-iconbtn--bordered' : '', className].filter(Boolean).join(' ');
  return (
    <button type="button" className={cls} aria-label={label} title={label} {...rest}>
      <Icon name={icon} size={size === 'sm' ? 15 : 17} />
    </button>
  );
}
