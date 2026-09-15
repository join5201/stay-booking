import type { AttemptStatus, BookingStatus } from "@/lib/api/types";

// 배지 색 넷. 인계 문서 95행. 자연물 톤 저채도
export type BadgeTone = "held" | "confirmed" | "expired" | "canceled";

export interface StatusBadgeProps {
  tone: BadgeTone;
  children: string;
}

const TONE: Record<BadgeTone, string> = {
  held: "bg-badge-held-bg text-badge-held-fg",
  confirmed: "bg-badge-confirmed-bg text-badge-confirmed-fg",
  expired: "bg-badge-expired-bg text-badge-expired-fg",
  canceled: "bg-badge-canceled-bg text-badge-canceled-fg",
};

export function StatusBadge({ tone, children }: StatusBadgeProps) {
  return (
    <span className={`inline-block rounded-badge px-1.5 py-0.5 text-12 font-semibold ${TONE[tone]}`}>
      {children}
    </span>
  );
}

// 상태 이름은 context.md 13절. 색은 인계 문서 95행
export const BOOKING_BADGE: Record<BookingStatus, { label: string; tone: BadgeTone }> = {
  HELD: { label: "선점", tone: "held" },
  CONFIRMED: { label: "확정", tone: "confirmed" },
  EXPIRED: { label: "만료", tone: "expired" },
  CANCELED: { label: "취소", tone: "canceled" },
};

export const ATTEMPT_BADGE: Record<AttemptStatus, { label: string; tone: BadgeTone }> = {
  REQUESTED: { label: "처리 중", tone: "expired" },
  APPROVED: { label: "승인", tone: "confirmed" },
  FAILED: { label: "실패", tone: "canceled" },
};

export const REFUND_BADGE = { label: "환불", tone: "expired" as BadgeTone };

export function promotionBadge(enabled: boolean): { label: string; tone: BadgeTone } {
  return enabled ? { label: "사용 중", tone: "confirmed" } : { label: "꺼짐", tone: "expired" };
}
