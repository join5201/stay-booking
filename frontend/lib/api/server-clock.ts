// 서버 시각 오프셋. 응답의 serverNow(본문 우선, 없으면 Date 헤더)와 받은 순간의 차이만 기억한다.
// 이후 serverNowIso는 그 차이를 더해 낸다. 브라우저 시계의 절대값은 쓰지 않는다(계약 6절 serverNow 행)

let offsetMs: number | null = null;

export function noteServerNow(iso: string): void {
  const ms = Date.parse(iso);
  if (Number.isNaN(ms)) return;
  offsetMs = ms - Date.now();
}

// 아직 응답을 하나도 못 받았으면 null. 카운트다운 화면은 전부 BOOK-03의 serverNow를 쓰므로 이것은 보조
export function serverNowIso(): string | null {
  return offsetMs === null ? null : new Date(Date.now() + offsetMs).toISOString();
}

export function resetServerClock(): void {
  offsetMs = null;
}
