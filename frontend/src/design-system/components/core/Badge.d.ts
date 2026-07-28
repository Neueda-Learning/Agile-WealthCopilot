import * as React from 'react';

export interface BadgeProps extends React.HTMLAttributes<HTMLSpanElement> {
  /** gain/loss are reserved for market direction, never for generic success/error. */
  tone?: 'neutral' | 'brand' | 'gain' | 'loss' | 'caution';
  outline?: boolean;
  /** Lucide icon name rendered at 11px before the label. */
  icon?: string;
  children?: React.ReactNode;
}

export declare function Badge(props: BadgeProps): JSX.Element;
