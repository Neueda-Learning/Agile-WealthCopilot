import * as React from 'react';

export interface StatProps extends React.HTMLAttributes<HTMLDivElement> {
  /** Uppercase 11px eyebrow, e.g. "Total value". */
  label: React.ReactNode;
  /** Pre-formatted figure string — Stat does not format. */
  value: React.ReactNode;
  /** xl for the hero portfolio value, lg for KPI cards, md inline. */
  size?: 'md' | 'lg' | 'xl';
  /** Supporting row, usually a DeltaValue plus a timeframe label. */
  foot?: React.ReactNode;
}

export declare function Stat(props: StatProps): JSX.Element;
