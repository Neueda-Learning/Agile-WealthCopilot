import * as React from 'react';

export interface TickerAvatarProps extends React.HTMLAttributes<HTMLSpanElement> {
  /** Ticker symbol; truncated to 4 characters. */
  symbol: string;
  size?: number;
}

export declare function TickerAvatar(props: TickerAvatarProps): JSX.Element;
