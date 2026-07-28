import * as React from 'react';

export interface IconProps extends React.HTMLAttributes<HTMLSpanElement> {
  /** Lucide icon name in kebab-case, e.g. "trending-up", "wallet", "sparkles". */
  name: string;
  /** Square size in px. Use 16 inline, 18 in nav, 20+ for feature moments. */
  size?: number;
  /** Defaults to currentColor — prefer inheriting over passing a color. */
  color?: string;
}

export declare function Icon(props: IconProps): JSX.Element;
