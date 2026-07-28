import React from 'react';
import { Icon } from '../core/Icon.jsx';
import { Wordmark } from '../core/Wordmark.jsx';

export function SidebarNav({ groups = [], active, onNavigate, footer, showWordmark = true, className = '', ...rest }) {
  return (
    <nav className={'wc-sidebar ' + className} {...rest}>
      {showWordmark ? <div style={{ padding: '0 var(--space-4)' }}><Wordmark size={17} /></div> : null}
      {groups.map(function (g, gi) {
        return (
          <div className="wc-navgroup" key={g.label || gi}>
            {g.label ? <div className="wc-navgroup__label">{g.label}</div> : null}
            {g.items.map(function (it) {
              return (
                <button key={it.id} type="button" className="wc-navitem"
                  aria-current={it.id === active ? 'page' : undefined}
                  onClick={function () { if (onNavigate) onNavigate(it.id); }}>
                  <Icon name={it.icon} size={17} />
                  {it.label}
                  {it.badge ? <span className="wc-navitem__badge">{it.badge}</span> : null}
                </button>
              );
            })}
          </div>
        );
      })}
      {footer ? <div style={{ marginTop: 'auto' }}>{footer}</div> : null}
    </nav>
  );
}
