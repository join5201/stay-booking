import type { Booking, BookingStatus } from "@/lib/api/types";
import { nightsOf } from "@/lib/stay";

// G5와 G7이 같이 쓰는 문구. 만료 두 사유(계약 2절 G5 행과 G7 행)
export const EXPIRED_TEXT: Record<string, string> = {
  TTL_EXPIRED: "남은 시간 안에 결제하지 않아 예약이 만료됐습니다.",
  PAYMENT_FAILED: "결제 시도 3회가 모두 실패해 예약이 만료됐습니다.",
};

export function expiredTextOf(reason: string | null): string {
  return EXPIRED_TEXT[reason ?? ""] ?? "예약이 만료됐습니다.";
}

// 2026-10-01 부터 2026-10-03 전까지 2박, 2명. 숙박 날짜 문자열은 그대로(P03)
export function stayTextOf(b: Pick<Booking, "checkIn" | "checkOut" | "guestCount">): string {
  return `${b.checkIn} 부터 ${b.checkOut} 전까지 ${nightsOf(b)}박, ${b.guestCount}명`;
}

// G6 필터 다섯. 화면이 네 값 밖을 만들지 않는다(계약 2절 G6 행)
export type BookingFilter = "ALL" | BookingStatus;

export const BOOKING_FILTERS: readonly { value: BookingFilter; label: string }[] = [
  { value: "ALL", label: "전체" },
  { value: "HELD", label: "선점" },
  { value: "CONFIRMED", label: "확정" },
  { value: "EXPIRED", label: "만료" },
  { value: "CANCELED", label: "취소" },
];

export function bookingFilterOf(value: string | null): BookingFilter {
  return BOOKING_FILTERS.some((f) => f.value === value) && value !== "ALL" ? (value as BookingStatus) : "ALL";
}
