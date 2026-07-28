import React from 'react';

const CDN = 'https://unpkg.com/lucide-static@0.446.0/icons/';

/* Lucide (2px stroke, rounded caps) is the WealthCopilot icon set.
   Glyphs are masked so they inherit currentColor like a real icon font. */
export function Icon({ name, size = 16, color = 'currentColor', style, ...rest }) {
  const url = 'url("' + CDN + name + '.svg")';
  return (
    <span
      aria-hidden="true"
      data-icon={name}
      style={{
        display: 'inline-block', flex: '0 0 auto', width: size, height: size,
        backgroundColor: color,
        WebkitMaskImage: url, maskImage: url,
        WebkitMaskRepeat: 'no-repeat', maskRepeat: 'no-repeat',
        WebkitMaskSize: 'contain', maskSize: 'contain',
        WebkitMaskPosition: 'center', maskPosition: 'center',
        ...style,
      }}
      {...rest}
    />
  );
}
