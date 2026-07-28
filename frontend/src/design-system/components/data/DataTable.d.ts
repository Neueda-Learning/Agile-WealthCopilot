import * as React from 'react';

export interface DataTableColumn {
  key: string;
  header: React.ReactNode;
  /** "right" also switches the cell to tabular mono — use for every number. */
  align?: 'left' | 'right';
  width?: number | string;
  render?: (row: any) => React.ReactNode;
}

export interface DataTableProps extends React.TableHTMLAttributes<HTMLTableElement> {
  columns?: DataTableColumn[];
  rows?: any[];
  /** 40px rows instead of 48px. */
  compact?: boolean;
  onRowClick?: (row: any) => void;
}

export declare function DataTable(props: DataTableProps): JSX.Element;
