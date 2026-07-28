import React from 'react';

export function Checkbox({ label, description, type = 'checkbox', className = '', ...rest }) {
  const shape = type === 'radio' ? 'wc-check--radio' : 'wc-check--box';
  return (
    <label className={'wc-check ' + shape + ' ' + className}>
      <input type={type} {...rest} />
      <span>
        {label}
        {description ? <span style={{ display: 'block', color: 'var(--text-muted)', fontSize: 'var(--fs-sm)' }}>{description}</span> : null}
      </span>
    </label>
  );
}
