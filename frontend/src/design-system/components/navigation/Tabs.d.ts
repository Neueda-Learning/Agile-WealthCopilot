import * as React from 'react';

export interface TabItem { id: string; label: string }

export interface TabsProps extends React.HTMLAttributes<HTMLDivElement> {
  tabs?: Array<string | TabItem>;
  value?: string;
  onChange?: (id: string) => void;
}

export declare function Tabs(props: TabsProps): JSX.Element;
