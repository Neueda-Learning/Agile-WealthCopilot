import * as React from 'react';

export interface SparklineProps extends React.SVGAttributes<SVGSVGElement> {
  /** Ordered price series, oldest first. */
  data?: number[];
  width?: number;
  height?: number;
  /** Overrides the auto direction (last vs first point). */
  tone?: 'gain' | 'loss' | 'flat';
  strokeWidth?: number;
  /** Adds a 10%-opacity area under the line — for hero charts, not table cells. */
  fill?: boolean;
}

export declare function Sparkline(props: SparklineProps): JSX.Element;
