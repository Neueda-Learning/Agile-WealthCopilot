import React from 'react';

export function Card({
  title, subtitle, action, elevation = 'flat', interactive = false,
  flush = false, children, className = '', ...rest
}) {
  const cls = ['wc-card', 'wc-card--' + elevation,
    interactive ? 'wc-card--interactive' : '', className].filter(Boolean).join(' ');
  return (
    <section className={cls} {...rest}>
      {title || action ? (
        <header className="wc-card__head">
          <div>
            <div className="wc-card__title">{title}</div>
            {subtitle ? <div className="wc-card__sub">{subtitle}</div> : null}
          </div>
          {action}
        </header>
      ) : null}
      <div className={'wc-card__body' + (flush ? ' wc-card__body--flush' : '')}>{children}</div>
    </section>
  );
}
