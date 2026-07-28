import React from 'react';

export function Stat({ label, value, size = 'lg', foot, className = '', ...rest }) {
  return (
    <div className={'wc-stat ' + className} {...rest}>
      <div className="wc-stat__label">{label}</div>
      <div className={'wc-stat__value wc-stat__value--' + size}>{value}</div>
      {foot ? <div className="wc-stat__foot">{foot}</div> : null}
    </div>
  );
}
