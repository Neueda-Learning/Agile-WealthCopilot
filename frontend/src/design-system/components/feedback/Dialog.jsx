import React from 'react';
import { IconButton } from '../core/IconButton.jsx';

export function Dialog({ open = true, title, subtitle, footer, onClose, width, children, className = '', ...rest }) {
  if (!open) return null;
  return (
    <div className="wc-scrim" onClick={onClose}>
      <div className={'wc-dialog ' + className} role="dialog" aria-modal="true"
        style={width ? { maxWidth: width } : undefined}
        onClick={function (e) { e.stopPropagation(); }} {...rest}>
        <header className="wc-dialog__head">
          <div>
            <div className="wc-dialog__title">{title}</div>
            {subtitle ? <div className="wc-dialog__sub">{subtitle}</div> : null}
          </div>
          {onClose ? <IconButton icon="x" label="Close" size="sm" onClick={onClose} /> : null}
        </header>
        <div className="wc-dialog__body">{children}</div>
        {footer ? <footer className="wc-dialog__foot">{footer}</footer> : null}
      </div>
    </div>
  );
}
