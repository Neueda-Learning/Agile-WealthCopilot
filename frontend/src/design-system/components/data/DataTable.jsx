import React from 'react';

export function DataTable({ columns = [], rows = [], compact = false, onRowClick, className = '', ...rest }) {
  const cls = ['wc-table', compact ? 'wc-table--compact' : '', onRowClick ? 'wc-table--clickable' : '', className]
    .filter(Boolean).join(' ');
  return (
    <table className={cls} {...rest}>
      <thead>
        <tr>{columns.map(function (c) {
          return <th key={c.key} className={c.align === 'right' ? 'num' : undefined} style={c.width ? { width: c.width } : undefined}>{c.header}</th>;
        })}</tr>
      </thead>
      <tbody>
        {rows.map(function (r, i) {
          return (
            <tr key={r.id || i} onClick={onRowClick ? function () { onRowClick(r); } : undefined}>
              {columns.map(function (c) {
                return <td key={c.key} className={c.align === 'right' ? 'num' : undefined}>
                  {c.render ? c.render(r) : r[c.key]}
                </td>;
              })}
            </tr>
          );
        })}
      </tbody>
    </table>
  );
}
