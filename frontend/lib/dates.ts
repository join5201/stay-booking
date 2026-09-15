// YYYY-MM-DD 날짜 문자열 계산. 시간대 없이 날짜만 다룬다(숙박 날짜와 달력 기간)

const YMD = /^(\d{4})-(\d{2})-(\d{2})$/;

export function isYmd(value: string): boolean {
  const m = YMD.exec(value);
  if (!m) return false;
  const [y, mo, d] = [Number(m[1]), Number(m[2]), Number(m[3])];
  const t = new Date(Date.UTC(y, mo - 1, d));
  return t.getUTCFullYear() === y && t.getUTCMonth() === mo - 1 && t.getUTCDate() === d;
}

function toUtc(ymd: string): number {
  const [y, m, d] = ymd.split("-").map(Number);
  return Date.UTC(y, m - 1, d);
}

function fromUtc(ms: number): string {
  return new Date(ms).toISOString().slice(0, 10);
}

export function addDays(ymd: string, days: number): string {
  return fromUtc(toUtc(ymd) + days * 86_400_000);
}

// to에서 from을 뺀 일수. 체크인과 체크아웃이면 박수
export function daysBetween(from: string, to: string): number {
  return Math.round((toUtc(to) - toUtc(from)) / 86_400_000);
}

// from부터 to까지 양끝 포함
export function eachDay(from: string, to: string): string[] {
  const out: string[] = [];
  for (let t = toUtc(from); t <= toUtc(to); t += 86_400_000) out.push(fromUtc(t));
  return out;
}

// 0 일요일부터 6 토요일
export function weekdayOf(ymd: string): number {
  return new Date(toUtc(ymd)).getUTCDay();
}

export function monthKeyOf(ymd: string): string {
  return ymd.slice(0, 7);
}
