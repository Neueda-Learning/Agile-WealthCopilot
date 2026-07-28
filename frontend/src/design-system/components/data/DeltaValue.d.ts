import * as React from 'react';

/**
 * Signed money/percentage change with directional colour and arrow.
 */
export interface DeltaValueProps extends React.HTMLAttributes<HTMLSpanElement> {
  /** Signed change. Sign drives colour: >0 green, <0 red, 0 grey. */
  value?: number;
  /** Optional percentage shown alongside, e.g. 2.41 renders "(2.41%)". */
  percent?: number;
  /** ISO currency code — omit for a plain number. */
  currency?: string;
  digits?: number;
  showArrow?: boolean;
  /** Wraps in a tinted pill — for table cells and stat footers. */
  pill?: boolean;
  /** Font size in px; the arrow scales with it. */
  size?: number;
}

export declare function DeltaValue(props: DeltaValueProps): JSX.Element;
