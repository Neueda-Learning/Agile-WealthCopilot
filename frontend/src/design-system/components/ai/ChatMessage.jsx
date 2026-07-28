import React from 'react';
import { Icon } from '../core/Icon.jsx';

export function ChatMessage({ role = 'assistant', tool, children, className = '', ...rest }) {
  return (
    <div className={'wc-msg wc-msg--' + role + ' ' + className} {...rest}>
      <span className="wc-msg__avatar">
        <Icon name={role === 'user' ? 'user' : 'sparkles'} size={14} />
      </span>
      <div>
        {tool ? <div className="wc-msg__tool"><Icon name="wrench" size={11} />{tool}</div> : null}
        <div className="wc-msg__bubble">{children}</div>
      </div>
    </div>
  );
}
