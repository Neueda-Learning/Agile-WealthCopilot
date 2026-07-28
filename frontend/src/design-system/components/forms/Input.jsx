import React from 'react';
import { Icon } from '../core/Icon.jsx';

export function Input({
  label, hint, error, prefix, iconLeft, numeric = false,
  id, className = '', ...rest
}) {
  const fid = id || 'in-' + (label || 'field').toLowerCase().replace(/[^a-z0-9]+/g, '-');
  const cls = ['wc-input', numeric ? 'wc-input--num' : '', error ? 'wc-input--invalid' : '', className]
    .filter(Boolean).join(' ');
  const padLeft = prefix ? 26 : iconLeft ? 34 : undefined;
  return (
    <div className="wc-field">
      {label ? <label className="wc-field__label" htmlFor={fid}>{label}</label> : null}
      <div className="wc-inputwrap">
        {prefix ? <span className="wc-inputwrap__affix wc-inputwrap__affix--left">{prefix}</span> : null}
        {iconLeft && !prefix ? (
          <span className="wc-inputwrap__affix wc-inputwrap__affix--left"><Icon name={iconLeft} size={16} /></span>
        ) : null}
        <input id={fid} className={cls} style={padLeft ? { paddingLeft: padLeft } : undefined}
          aria-invalid={error ? true : undefined} {...rest} />
      </div>
      {error ? <div className="wc-field__error">{error}</div>
        : hint ? <div className="wc-field__hint">{hint}</div> : null}
    </div>
  );
}
