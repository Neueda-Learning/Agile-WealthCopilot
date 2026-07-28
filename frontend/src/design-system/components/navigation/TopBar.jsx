import React from 'react';

export function TopBar({ title, subtitle, children, className = '', ...rest }) {
  return (
    <header className={'wc-topbar ' + className} {...rest}>
      <div>
        <div className="wc-topbar__title">{title}</div>
        {subtitle ? <div className="wc-card__sub">{subtitle}</div> : null}
      </div>
      <span className="wc-topbar__spacer" />
      {children}
    </header>
  );
}
