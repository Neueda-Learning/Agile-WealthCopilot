import React from 'react';

export function SegmentedControl({ options = [], value, onChange, className = '', ...rest }) {
  return (
    <div className={'wc-segmented ' + className} role="group" {...rest}>
      {options.map(function (o) {
        const v = typeof o === 'string' ? o : o.value;
        const l = typeof o === 'string' ? o : o.label;
        return (
          <button key={v} type="button" className="wc-segmented__item"
            aria-pressed={v === value} onClick={function () { if (onChange) onChange(v); }}>
            {l}
          </button>
        );
      })}
    </div>
  );
}
