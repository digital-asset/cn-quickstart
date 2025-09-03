export function toISO_8601(duration: string): string {
  const match = /^(\d+)([smhd])$/.exec(duration);
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