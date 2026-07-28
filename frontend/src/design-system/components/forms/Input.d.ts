import * as React from 'react';

/**
 * Labelled text/number field.
 */
export interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  /** Quiet helper text under the field. Replaced by error when error is set. */
  hint?: string;
  error?: string;
  /** Static leading string, e.g. "$". */
  prefix?: string;
  /** Lucide icon name shown leading, when there is no prefix. */
  iconLeft?: string;
  /** Renders the value in tabular mono — use for money, quantities, prices. */
  numeric?: boolean;
}

export declare function Input(props: InputProps): JSX.Element;
