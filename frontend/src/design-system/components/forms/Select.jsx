import React from 'react';
import { Icon } from '../core/Icon.jsx';

export function Select({ label, hint, options = [], id, className = '', ...rest }) {
  const fid = id || 'sel-' + (label || 'field').toLowerCase().replace(/[^a-z0-9]+/g, '-');
  return (
    <div className="wc-field">
      {label ? <label className="wc-field__label" htmlFor={fid}>{label}</label> : null}
      <div className="wc-inputwrap">
        <select id={fid} className={'wc-input wc-select ' + className} {...rest}>
          {options.map(function (o) {
            const v = typeof o === 'string' ? o : o.value;
            const l = typeof o === 'string' ? o : o.label;
            return <option key={v} value={v}>{l}</option>;
          })}
        </select>
        <span className="wc-inputwrap__affix wc-inputwrap__affix--right"><Icon name="chevron-down" size={16} /></span>
      </div>
      {hint ? <div className="wc-field__hint">{hint}</div> : null}
    </div>
  );
}
