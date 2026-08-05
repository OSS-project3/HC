/**
 * 건곤감리 (four I Ching trigrams) — the Korean-flag corner symbols, drawn as
 * bar patterns. Used as faint background decoration in the hero.
 *  건 Geon ☰ [1,1,1] · 곤 Gon ☷ [0,0,0] · 감 Gam ☵ [0,1,0] · 리 Ri ☲ [1,0,1]
 */
const patterns: Record<string, number[]> = {
  건: [1, 1, 1],
  곤: [0, 0, 0],
  감: [0, 1, 0],
  리: [1, 0, 1],
};

export function Trigram({ name, className }: { name: keyof typeof patterns | string; className?: string }) {
  const rows = patterns[name] ?? [1, 1, 1];
  const barH = 12;
  const gap = 9;
  const w = 78;
  const brokenGap = 14;
  return (
    <svg
      className={className}
      viewBox={`0 0 ${w} ${rows.length * barH + (rows.length - 1) * gap}`}
      aria-hidden="true"
      focusable="false"
    >
      {rows.map((solid, i) => {
        const y = i * (barH + gap);
        if (solid) {
          return <rect key={i} x="0" y={y} width={w} height={barH} rx="2" />;
        }
        const half = (w - brokenGap) / 2;
        return (
          <g key={i}>
            <rect x="0" y={y} width={half} height={barH} rx="2" />
            <rect x={half + brokenGap} y={y} width={half} height={barH} rx="2" />
          </g>
        );
      })}
    </svg>
  );
}
