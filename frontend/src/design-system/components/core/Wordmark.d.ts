import * as React from 'react';

export interface WordmarkProps extends React.HTMLAttributes<HTMLSpanElement> {
  /** Cap height driver in px — the accent square scales with it. */
  size?: number;
  showDot?: boolean;
  /** Renders the accent square in currentColor for single-colour contexts. */
  monochrome?: boolean;
}

export declare function Wordmark(props: WordmarkProps): JSX.Element;
