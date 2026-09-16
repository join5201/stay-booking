// 시각은 UTC로 받아 서울로 보여 준다(P03). 숙박 날짜 문자열은 그대로 쓴다

const SEOUL = "Asia/Seoul";

function parts(iso: string): Record<string, string> {
  const fmt = new Intl.DateTimeFormat("ko-KR", {
    timeZone: SEOUL, hourCycle: "h23",
    year: "numeric", month: "2-digit", day: "2-digit", hour: "2-digit", minute: "2-digit", second: "2-digit",
  });
  const out: Record<string, string> = {};
  for (const p of fmt.formatToParts(new Date(iso))) out[p.type] = p.value;
  return out;
}

// YYYY-MM-DD (서울 날짜). 취소 가능 판단이 이 값끼리 비교한다
export function seoulDateOf(iso: string): string {
  const p = parts(iso);
  return `${p.year}-${p.month}-${p.day}`;
}

export type SeoulFormat = "datetime" | "date" | "time";

export function formatSeoul(iso: string, format: SeoulFormat = "datetime"): string {
  const p = parts(iso);
  const date = `${p.year}-${p.month}-${p.day}`;
  const time = `${p.hour}:${p.minute}`;
  if (format === "date") return date;
  if (format === "time") return time;
  return `${date} ${time}`;
}

// 남은 밀리초. expiresAt과 serverNow 차이. 브라우저 시계를 쓰지 않는다
export function remainingMs(expiresAt: string, serverNow: string): number {
  return Math.max(0, Date.parse(expiresAt) - Date.parse(serverNow));
}

// mm:ss. 한 시간을 넘으면 h:mm:ss
export function formatRemaining(ms: number): string {
  const totalSec = Math.max(0, Math.ceil(ms / 1000));
  const h = Math.floor(totalSec / 3600);
  const m = Math.floor((totalSec % 3600) / 60);
  const s = totalSec % 60;
  const mm = String(m).padStart(2, "0");
  const ss = String(s).padStart(2, "0");
  return h > 0 ? `${h}:${mm}:${ss}` : `${mm}:${ss}`;
}
