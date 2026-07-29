/**
 * Identity is never left to color alone — every chart with more than one
 * meaning-carrying fill states what each one is.
 */
export default function ChartLegend({ items }: { items: { label: string; color: string }[] }) {
  return (
    <span className="dv-legend">
      {items.map((i) => (
        <span key={i.label} className="dv-legend__item">
          <span className="dv-legend__swatch" style={{ background: i.color }} aria-hidden="true" />
          {i.label}
        </span>
      ))}
    </span>
  );
}
