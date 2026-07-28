import React from 'react';

/* WealthCopilot has no supplied logo mark. The brand signature is the wordmark
   set in Public Sans Bold with a rotated square accent. Do not substitute a
   drawn logo — render this. */
export function Wordmark({ size = 18, showDot = true, monochrome = false, className = '', ...rest }) {
  const d = Math.round(size * 0.42);
  return (
    <span className={'wc-wordmark ' + className} style={{ fontSize: size }} {...rest}>
      {showDot ? (
        <span className="wc-wordmark__dot" style={{ width: d, height: d, background: monochrome ? 'currentColor' : undefined }} />
      ) : null}
      <span>Wealth<em>Copilot</em></span>
    </span>
  );
}
