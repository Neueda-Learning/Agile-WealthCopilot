import * as React from 'react';

export interface AllocationSegment { label: string; value: number; color?: string }

export interface AllocationBarProps extends React.HTMLAttributes<HTMLDivElement> {
  /** Raw values — percentages are computed, do not pre-normalise. */
  segments?: AllocationSegment[];
  showLegend?: boolean;
}

export declare function AllocationBar(props: AllocationBarProps): JSX.Element;
