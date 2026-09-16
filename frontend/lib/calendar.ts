import { addDays, daysBetween, eachDay, isYmd } from "./dates";
import { serverNowIso } from "./api/server-clock";
import { seoulDateOf } from "./seoul-time";

// H4와 H5 달력 도우미. 화면과 URL의 기간은 양끝 포함이고 백엔드 INV-04와 RATE-03과 INV-02는 to를 제외한다
// (document/11 688행, 821행, 1032행). API에 보낼 때만 to에 하루를 더한다

export interface Period {
  from: string;
  to: string;
}

// 재고와 요금 기간 조회 상한. 양끝 포함 일수(P02)
export const MAX_PERIOD_DAYS = 366;

export function apiPeriodOf(period: Period): Period {
  return { from: period.from, to: addDays(period.to, 1) };
}

// 형식, 순서, 상한. PeriodPicker의 검사와 같은 규칙
export function isValidPeriod(period: Period, maxDays = MAX_PERIOD_DAYS): boolean {
  if (!isYmd(period.from) || !isYmd(period.to)) return false;
  const days = daysBetween(period.from, period.to);
  return days >= 0 && days + 1 <= maxDays;
}

// 서울 오늘. 서버 시각 오프셋이 있으면 그것을, 없으면 브라우저 시계를 쓴다. 달력 기본 기간에만 쓰고 판정에는 안 쓴다
export function todaySeoul(): string {
  return seoulDateOf(serverNowIso() ?? new Date().toISOString());
}

// URL의 from과 to. 없거나 틀리면 오늘부터 defaultDays일
export function periodFromParams(params: URLSearchParams, today: string, defaultDays = 60): Period {
  const period = { from: params.get("from") ?? "", to: params.get("to") ?? "" };
  if (isValidPeriod(period)) return period;
  return { from: today, to: addDays(today, defaultDays - 1) };
}

// 기간(양끝 포함) 안에서 이미 등록된 날짜. INV-02는 한 날짜라도 있으면 전부 실패라 보내기 전에 막는다(계약 2-1절 2행)
export function overlappingDates(period: Period, existing: Iterable<string>): string[] {
  const set = new Set(existing);
  return eachDay(period.from, period.to).filter((d) => set.has(d));
}

// 직전 날짜의 항목. 바로 전날이 없으면 그 앞에서 가장 가까운 날짜. 등록 폼이 값을 미리 채운다
export function previousItem<T extends { date: string }>(items: readonly T[], date: string): T | null {
  let best: T | null = null;
  for (const item of items) {
    if (item.date < date && (!best || item.date > best.date)) best = item;
  }
  return best;
}
