import * as React from 'react';

export interface BannerProps extends React.HTMLAttributes<HTMLDivElement> {
  tone?: 'info' | 'gain' | 'caution' | 'loss';
  title?: React.ReactNode;
  /** Overrides the tone's default Lucide icon. */
  icon?: string;
  /** Trailing slot, usually a ghost Button. */
  action?: React.ReactNode;
  children?: React.ReactNode;
}

export declare function Banner(props: BannerProps): JSX.Element;
