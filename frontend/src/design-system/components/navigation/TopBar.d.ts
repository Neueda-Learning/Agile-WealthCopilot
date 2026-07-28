import * as React from 'react';

export interface TopBarProps extends React.HTMLAttributes<HTMLElement> {
  title?: React.ReactNode;
  subtitle?: React.ReactNode;
  /** Right-aligned actions. */
  children?: React.ReactNode;
}

export declare function TopBar(props: TopBarProps): JSX.Element;
