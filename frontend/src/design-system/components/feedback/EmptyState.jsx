import React from 'react';
import { Icon } from '../core/Icon.jsx';

export function EmptyState({ icon = 'inbox', title, children, action, className = '', ...rest }) {
  return (
    <div className={'wc-empty ' + className} {...rest}>
      <span className="wc-empty__icon"><Icon name={icon} size={20} /></span>
      <div className="wc-empty__title">{title}</div>
      {children ? <p className="wc-empty__body">{children}</p> : null}
      {action ? <div style={{ marginTop: 'var(--space-3)' }}>{action}</div> : null}
    </div>
  );
}
