import * as React from 'react';

export interface SegmentedOption { value: string; label: string }

export interface SegmentedControlProps {
  options?: Array<string | SegmentedOption>;
  value?: string;
  onChange?: (value: string) => void;
  className?: string;
}

export declare function SegmentedControl(props: SegmentedControlProps): JSX.Element;
