import type { PaymentAttempt, Refund } from "@/lib/api/types";
import { DateText } from "./DateText";
import { Money } from "./Money";
import { ATTEMPT_BADGE, REFUND_BADGE, StatusBadge } from "./StatusBadge";

export interface PaymentAttemptsListProps {
  attempts: readonly PaymentAttempt[];
  // 취소나 늦은 승인의 환불 줄. 없으면 null
  refund?: Refund | null;
}

const FAILURE_TEXT: Record<string, string> = {
  MOCK_DECLINED: "Mock 거절",
};

const REFUND_REASON: Record<Refund["reason"], string> = {
  BOOKING_CANCELED: "예약 취소",
  LATE_APPROVAL: "늦은 승인",
};

// 결제 시도 최대 셋과 환불 한 줄. G5와 G7
export function PaymentAttemptsList({ attempts, refund = null }: PaymentAttemptsListProps) {
  if (attempts.length === 0 && !refund) return <p className="text-13 text-ink-3">결제 시도 없음</p>;
  return (
    <ol className="flex flex-col gap-2">
      {attempts.map((a) => {
        const badge = ATTEMPT_BADGE[a.status];
        return (
          <li key={a.id} className="flex items-center gap-3 rounded-card border border-line bg-surface px-4 py-3 text-14">
            <span className="w-14 text-13 text-ink-3 tabular-nums">{a.attemptNumber}회</span>
            <StatusBadge tone={badge.tone}>{badge.label}</StatusBadge>
            <Money amount={a.amount} className="ml-1" />
            {a.failureCode ? <span className="text-13 text-ink-3">{FAILURE_TEXT[a.failureCode] ?? a.failureCode}</span> : null}
            {a.mockMode ? <span className="text-12 text-ink-4">개발용 {a.mockMode}</span> : null}
            <span className="ml-auto text-13 text-ink-3">
              <DateText value={a.completedAt ?? a.requestedAt} />
            </span>
          </li>
        );
      })}
      {refund ? (
        <li className="flex items-center gap-3 rounded-card border border-line bg-surface-2 px-4 py-3 text-14">
          <span className="w-14 text-13 text-ink-3">환불</span>
          <StatusBadge tone={REFUND_BADGE.tone}>{REFUND_BADGE.label}</StatusBadge>
          <Money amount={refund.amount} className="ml-1" />
          <span className="text-13 text-ink-3">{REFUND_REASON[refund.reason]}</span>
          <span className="ml-auto text-13 text-ink-3">
            <DateText value={refund.refundedAt} />
          </span>
        </li>
      ) : null}
    </ol>
  );
}
