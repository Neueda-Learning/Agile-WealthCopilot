import React from 'react';

export function Tabs({ tabs = [], value, onChange, className = '', ...rest }) {
  return (
    <div className={'wc-tabs ' + className} role="tablist" {...rest}>
      {tabs.map(function (t) {
        const id = typeof t === 'string' ? t : t.id;
        const label = typeof t === 'string' ? t : t.label;
        return (
          <button key={id} type="button" role="tab" className="wc-tab"
            aria-selected={id === value} onClick={function () { if (onChange) onChange(id); }}>
            {label}
          </button>
        );
      })}
    </div>
  );
}
