import * as React from 'react';

/**
 * One turn in the Copilot conversation.
 */
export interface ChatMessageProps extends React.HTMLAttributes<HTMLDivElement> {
  role?: 'user' | 'assistant';
  /** Read-only tool call disclosure, e.g. "read_holdings(portfolio)". */
  tool?: string;
  children?: React.ReactNode;
}

export declare function ChatMessage(props: ChatMessageProps): JSX.Element;
