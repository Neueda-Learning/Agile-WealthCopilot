import * as React from 'react';

export interface EmptyStateProps extends React.HTMLAttributes<HTMLDivElement> {
  /** Lucide icon name in a grey circle. */
  icon?: string;
  title: React.ReactNode;
  action?: React.ReactNode;
  children?: React.ReactNode;
}

export declare function EmptyState(props: EmptyStateProps): JSX.Element;
