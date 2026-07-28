import React from 'react';

export function Switch({ label, checked = false, disabled = false, onChange, className = '', ...rest }) {
  return (
    <label className={'wc-switch ' + className} data-on={checked} data-disabled={disabled} {...rest}>
      <span className="wc-switch__track"><span className="wc-switch__thumb" /></span>
      <input type="checkbox" checked={checked} disabled={disabled}
        onChange={onChange} style={{ position: 'absolute', opacity: 0, width: 0, height: 0 }} />
      {label ? <span>{label}</span> : null}
    </label>
  );
}
