import type { Availability, AvailabilityDay } from "@/lib/api/types";

// 날짜별 사유. INVENTORY_NOT_CONFIGURED는 null(판매 안 함), RATE_NOT_CONFIGURED는 missingRateDates, INVENTORY_UNAVAILABLE은 0(마감),
// OCCUPANCY_EXCEEDED는 전 날짜(계약 2절 G3 행). 사유가 없으면 빈 문자열
export function dayReasonOf(day: AvailabilityDay, availability: Pick<Availability, "missingRateDates" | "reasons">, maxOccupancy: number): string {
  if (day.availableCount === null) return "판매 안 함";
  if (availability.missingRateDates.includes(day.date)) return "요금 없음";
  if (day.availableCount === 0) return "마감";
  if (availability.reasons.includes("OCCUPANCY_EXCEEDED")) return `인원 초과 (최대 ${maxOccupancy}명)`;
  return "";
}

export interface AvailabilityTableProps {
  availability: Availability;
  maxOccupancy: number;
}

// G3의 날짜별 가용 수. 체크아웃 날은 줄에 없다
export function AvailabilityTable({ availability, maxOccupancy }: AvailabilityTableProps) {
  return (
    <div className="rounded-card border border-line bg-surface">
      <table className="w-full border-collapse text-14">
        <thead>
          <tr className="bg-surface-2 text-12 text-ink-3">
            <th scope="col" className="border-b border-line px-3 py-2 text-left font-semibold">날짜</th>
            <th scope="col" className="border-b border-line px-3 py-2 text-right font-semibold">남은 객실</th>
            <th scope="col" className="border-b border-line px-3 py-2 text-left font-semibold">사유</th>
          </tr>
        </thead>
        <tbody>
          {availability.days.map((d) => {
            const reason = dayReasonOf(d, availability, maxOccupancy);
            return (
              <tr key={d.date} className={`border-b border-line-soft ${reason ? "text-ink-3" : ""}`}>
                <td className="px-3 py-2 tabular-nums">{d.date}</td>
                <td className="px-3 py-2 text-right tabular-nums">{d.availableCount === null ? "-" : d.availableCount}</td>
                <td className="px-3 py-2">{reason || <span className="text-ink-4">-</span>}</td>
              </tr>
            );
          })}
        </tbody>
      </table>
      <p className="border-t border-line px-3 py-2 text-12 text-ink-3">
        {availability.available ? `전 날짜 예약 가능. 남은 객실 최소 ${availability.availableCount}` : "이 조건으로는 예약할 수 없는 날짜가 있습니다."}
      </p>
    </div>
  );
}
