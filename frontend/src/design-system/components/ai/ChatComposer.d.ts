import * as React from 'react';

export interface ChatComposerProps extends React.HTMLAttributes<HTMLDivElement> {
  value?: string;
  onChange?: (value: string) => void;
  /** Fired on Enter (Shift+Enter inserts a newline) and on the send button. */
  onSubmit?: () => void;
  placeholder?: string;
  /** Capability disclosure shown under the field — keep it, don't hide it. */
  hint?: string;
  submitLabel?: string;
  /** Starter prompts rendered as small secondary buttons. */
  suggestions?: string[];
  onSuggestion?: (s: string) => void;
  disabled?: boolean;
}

export declare function ChatComposer(props: ChatComposerProps): JSX.Element;
