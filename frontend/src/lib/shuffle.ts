/** Proper Fisher-Yates shuffle (returns a new array). */
export function shuffle<T>(input: readonly T[]): T[] {
  const arr = input.slice();
  for (let i = arr.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [arr[i], arr[j]] = [arr[j], arr[i]];
  }
  return arr;
}

/** Pick `count` unique items at random (no duplicates). */
export function pickUnique<T>(input: readonly T[], count: number): T[] {
  return shuffle(input).slice(0, Math.min(count, input.length));
}
