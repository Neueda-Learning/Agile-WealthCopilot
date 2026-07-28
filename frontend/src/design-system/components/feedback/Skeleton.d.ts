import * as React from 'react';

export interface SkeletonProps extends React.HTMLAttributes<HTMLSpanElement> {
  width?: number | string;
  height?: number | string;
  radius?: number | string;
  /** >1 renders a stack of lines, the last at 60% width. */
  lines?: number;
}

export declare function Skeleton(props: SkeletonProps): JSX.Element;
