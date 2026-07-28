import * as React from 'react';

export interface ParsedField { key: string; value: React.ReactNode }

export interface ParsedTransactionCardProps extends React.HTMLAttributes<HTMLDivElement> {
  /** Field/value pairs in reading order: Action, Symbol, Quantity, Price, Date, Total. */
  fields?: ParsedField[];
  /** The user's original sentence, quoted back for verification. */
  source?: string;
  confidence?: 'high' | 'medium' | 'low';
  onConfirm?: () => void;
  onEdit?: () => void;
  onDiscard?: () => void;
  confirmLabel?: string;
}

export declare function ParsedTransactionCard(props: ParsedTransactionCardProps): JSX.Element;
