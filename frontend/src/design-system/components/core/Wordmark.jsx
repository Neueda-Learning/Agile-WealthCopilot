import React from 'react';

/* Shared brand signature used by auth screens and the application sidebar. */
export function Wordmark({ size = 18, showDot = true, monochrome = false, className = '', ...rest }) {
  const logoSize = Math.round(size * 1.5);
  return (
    <span className={'wc-wordmark ' + className} style={{ fontSize: size }} {...rest}>
      {showDot ? (
        <img
          className={'wc-wordmark__logo' + (monochrome ? ' wc-wordmark__logo--monochrome' : '')}
          src="/wealthcopilot-logo.png"
          width={logoSize}
          height={logoSize}
          alt=""
          aria-hidden="true"
        />
      ) : null}
      <span>Wealth<em>Copilot</em></span>
    </span>
  );
}
