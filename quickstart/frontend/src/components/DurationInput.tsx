import type { CSSProperties } from 'react';

export default function DurationInput({
  value,
  onChange,
  inputStyle,
  selectStyle,
  units,
}: {
  value?: string;
  onChange: (v: string) => void;
  inputStyle?: CSSProperties;
  selectStyle?: CSSProperties;
  units?: Map<string, string> | string;
}) {
  const m = /^(\d+)(.)$/.exec(value || '');
  const num = m ? Number(m[1]) : 0;

  const defaultLabels: Record<string, string> = {
    s: 'seconds',
    m: 'minutes',
    d: 'days',
  };

  let unitOptions: [string, string][];
  if (typeof units === 'string') {
    const seen = new Set<string>();
    unitOptions = [];
    for (const ch of units.split('')) {
      if (seen.has(ch)) continue;
      seen.add(ch);
      unitOptions.push([ch, defaultLabels[ch] ?? ch]);
    }
    if (unitOptions.length === 0) {
      unitOptions = Object.entries(defaultLabels);
    }
  } else if (units instanceof Map) {
    unitOptions = Array.from(units.entries());
  } else {
    unitOptions = Object.entries(defaultLabels);
  }

  // choose unit: use parsed value if valid, otherwise first entry key or 'd'
  const unit = m ? m[2] : (unitOptions[0] ? unitOptions[0][0] : 'd');

  return (
    <>
      <input
        type="number"
        min={1}
        className="form-control"
        value={num}
        onChange={(e) => {
          const n = Number(e.target.value) || 0;
          onChange(`${n}${unit}`);
        }}
        style={{ width: '120px', ...(inputStyle || {}) }}
      />
      <select
        className="form-select"
        value={unit}
        onChange={(e) => {
          const u = (e.target.value as string) || (unitOptions[0] ? unitOptions[0][0] : 'd');
          const n = Number(num) || 0;
          onChange(`${n}${u}`);
        }}
        style={{ width: '120px', ...(selectStyle || {}) }}
      >
        {unitOptions.map(([key, label]) => (
          <option key={key} value={key}>
            {label}
          </option>
        ))}
      </select>
    </>
  );
}

export function toISO_8601(duration: string): string {
  const match = /^(\d+)([smhdM])$/.exec(duration);
  if (!match) throw new Error(`Invalid duration format: ${duration}`);

  const value = parseInt(match[1], 10);
  const unit = match[2];

  switch (unit) {
    case 's':
      return `PT${value}S`;
    case 'm':
      return `PT${value}M`;
    case 'h':
      return `PT${value}H`;
    case 'd':
      return `P${value}D`;
    default:
      throw new Error(`Unknown duration unit: ${unit}`);
  }
}

