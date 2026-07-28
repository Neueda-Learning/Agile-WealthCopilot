import React from 'react';
import { Icon } from '../core/Icon.jsx';

const ICONS = { info: 'info', gain: 'check-circle-2', caution: 'alert-triangle', loss: 'alert-octagon' };

export function Banner({ tone = 'info', title, icon, children, action, className = '', ...rest }) {
  return (
    <div className={'wc-banner wc-banner--' + tone + ' ' + className} {...rest}>
      <Icon name={icon || ICONS[tone]} size={16} style={{ marginTop: 2 }} />
      <div style={{ flex: 1 }}>
        {title ? <strong className="wc-banner__title">{title}</strong> : null}
        {children}
      </div>
      {action}
    </div>
  );
}
