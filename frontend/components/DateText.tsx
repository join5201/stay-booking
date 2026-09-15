import { formatSeoul, type SeoulFormat } from "@/lib/seoul-time";

export interface DateTextProps {
  // UTC ISO 시각. null이면 빈 자리 표시
  value: string | null | undefined;
  format?: SeoulFormat;
  empty?: string;
  className?: string;
}

// UTC 시각을 서울 시각으로. 숙박 날짜 문자열(checkIn 등)은 이 컴포넌트를 거치지 않고 그대로 쓴다
export function DateText({ value, format = "datetime", empty = "-", className = "" }: DateTextProps) {
  if (!value) return <span className={`text-ink-4 ${className}`}>{empty}</span>;
  return (
    <time dateTime={value} className={`tabular-nums ${className}`}>
      {formatSeoul(value, format)}
    </time>
  );
}
