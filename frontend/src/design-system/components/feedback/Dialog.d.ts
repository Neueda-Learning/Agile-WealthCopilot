import * as React from 'react';

export interface DialogProps extends React.HTMLAttributes<HTMLDivElement> {
  open?: boolean;
  title?: React.ReactNode;
  subtitle?: React.ReactNode;
  /** Right-aligned action row on the tinted footer. */
  footer?: React.ReactNode;
  onClose?: () => void;
  /** Overrides the 520px max width. */
  width?: number | string;
  children?: React.ReactNode;
}

export declare function Dialog(props: DialogProps): JSX.Element;
