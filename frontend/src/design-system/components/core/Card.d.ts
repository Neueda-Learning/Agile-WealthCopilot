import * as React from 'react';

/**
 * Hairline-bordered surface — the container for everything on a WealthCopilot page.
 */
export interface CardProps extends React.HTMLAttributes<HTMLElement> {
  title?: React.ReactNode;
  subtitle?: React.ReactNode;
  /** Right-aligned header slot — usually a Button ghost or SegmentedControl. */
  action?: React.ReactNode;
  /** flat = hairline only (default). raised = menus/floating. */
  elevation?: 'flat' | 'raised';
  interactive?: boolean;
  /** Removes body padding — use when the body is a table. */
  flush?: boolean;
  children?: React.ReactNode;
}

export declare function Card(props: CardProps): JSX.Element;
