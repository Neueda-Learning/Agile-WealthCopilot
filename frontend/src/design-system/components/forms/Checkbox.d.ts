import * as React from 'react';

export interface CheckboxProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label: React.ReactNode;
  /** Secondary line under the label. */
  description?: string;
  /** Pass "radio" for a single-choice control with identical layout. */
  type?: 'checkbox' | 'radio';
}

export declare function Checkbox(props: CheckboxProps): JSX.Element;
