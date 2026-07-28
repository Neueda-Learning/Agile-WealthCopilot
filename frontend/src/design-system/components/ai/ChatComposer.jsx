import React from 'react';
import { Button } from '../core/Button.jsx';
import { IconButton } from '../core/IconButton.jsx';

export function ChatComposer({
  value, onChange, onSubmit, placeholder = 'Ask about your portfolio…',
  hint = 'Copilot reads your holdings. It cannot place trades.',
  submitLabel = 'Ask', suggestions = [], onSuggestion, disabled = false, className = '', ...rest
}) {
  return (
    <div className={'wc-composer ' + className} {...rest}>
      <textarea className="wc-composer__input" value={value} placeholder={placeholder} disabled={disabled}
        onChange={function (e) { if (onChange) onChange(e.target.value); }}
        onKeyDown={function (e) {
          if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); if (onSubmit) onSubmit(); }
        }} />
      {suggestions.length ? (
        <div className="wc-composer__row" style={{ flexWrap: 'wrap' }}>
          {suggestions.map(function (s) {
            return <button key={s} type="button" className="wc-btn wc-btn--secondary wc-btn--sm"
              onClick={function () { if (onSuggestion) onSuggestion(s); }}>{s}</button>;
          })}
        </div>
      ) : null}
      <div className="wc-composer__row">
        <span className="wc-composer__hint">{hint}</span>
        <span style={{ flex: 1 }} />
        <IconButton icon="paperclip" label="Attach statement" size="sm" />
        <Button size="sm" iconRight="arrow-up" onClick={onSubmit} disabled={disabled || !value}>{submitLabel}</Button>
      </div>
    </div>
  );
}
