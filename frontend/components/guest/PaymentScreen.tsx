"use client";

import { useEffect, useState } from "react";
import { newIdempotencyKey } from "@/lib/api/client";
import { useBooking, useProperty, useRequestPayment, useRoomType } from "@/lib/api/hooks";
import type { Booking, BookingStatus, RequestPaymentBody } from "@/lib/api/types";
import { errorViewOf } from "@/lib/error-view";
import { retryPolicyOf } from "@/lib/errors";
import { isRegionCode } from "@/lib/regions";
import { searchQueryOf } from "@/lib/stay";
import { useBanner } from "../Banner";
import { Button } from "../Button";
import { Countdown } from "../Countdown";
import { useDevActor } from "../DevActorProvider";
import { Notice } from "../Notice";
import { PaymentAttemptsList } from "../PaymentAttemptsList";
import { PriceBreakdownTable } from "../PriceBreakdownTable";
import { QueryErrorNotice } from "../QueryErrorNotice";
import type { SearchValues } from "../SearchForm";
import { Skeleton } from "../Skeleton";
import { BOOKING_BADGE, StatusBadge } from "../StatusBadge";
import { StayConditions } from "./StayConditions";

export const MAX_ATTEMPTS = 3;

// G5의 상태 분기(계약 2절 G5 행, W14). CONFIRMED, HELD와 FAILED 3 미만(처음 포함), HELD와 REQUESTED(DEFER), EXPIRED 두 사유
export type PaymentPhase = "confirmed" | "payable" | "processing" | "exhausted" | "expired" | "other";

export function paymentPhaseOf(booking: Pick<Booking, "status" | "payment">): PaymentPhase {
  if (booking.status === "CONFIRMED") return "confirmed";
  if (booking.status === "EXPIRED") return "expired";
  if (booking.status !== "HELD") return "other";
  const attempts = booking.payment.attempts;
  if (attempts.some((a) => a.status === "REQUESTED")) return "processing";
  const failed = attempts.filter((a) => a.status === "FAILED").length;
  return failed < MAX_ATTEMPTS ? "payable" : "exhausted";
}

export interface PaymentScreenProps {
  bookingId: string;
  // 결제 뒤 예약 상세로(G7)
  onDetail: () => void;
  // 진입 시 HELD가 아니면 G7로 replace
  onReplaceDetail: () => void;
  // 만료 뒤 같은 조건으로 새 예약. G1 쿼리
  onNewBooking: (query: string) => void;
}

const EXPIRED_TEXT: Record<string, string> = {
  TTL_EXPIRED: "남은 시간 안에 결제하지 않아 예약이 만료됐습니다.",
  PAYMENT_FAILED: "결제 시도 3회가 모두 실패해 예약이 만료됐습니다.",
};

// G5 결제. BOOK-03 진입 시. 결제 버튼이 PAY-01(클릭마다 새 키, mockMode는 쿠키 값이고 APPROVE면 생략).
// 성공이나 replayed 뒤 BOOK-03 재조회는 훅의 onSettled. 남은 시간은 expiresAt과 serverNow 차이(P01)
export function PaymentScreen({ bookingId, onDetail, onReplaceDetail, onNewBooking }: PaymentScreenProps) {
  const banner = useBanner();
  const { mockMode } = useDevActor();
  const booking = useBooking(bookingId);
  const payment = useRequestPayment(bookingId);
  const property = useProperty(booking.data?.propertyId);
  const roomType = useRoomType(booking.data?.roomTypeId);

  // 첫 응답의 상태로만 replace를 판단한다. 결제 뒤의 CONFIRMED와 EXPIRED는 이 화면의 카드
  const [entryStatus, setEntryStatus] = useState<BookingStatus | null>(null);
  if (booking.data && entryStatus === null) setEntryStatus(booking.data.status);
  useEffect(() => {
    if (entryStatus !== null && entryStatus !== "HELD") onReplaceDetail();
  }, [entryStatus, onReplaceDetail]);

  const send = (idempotencyKey: string, body: RequestPaymentBody) => {
    payment.mutate(
      { idempotencyKey, body },
      {
        onError: (error) => {
          const view = errorViewOf(error);
          if (view.kind === "banner") {
            // 네트워크와 5xx와 REQUEST_IN_PROGRESS는 같은 키. 키 결함 코드는 새 키
            const nextKey = retryPolicyOf(error) === "new-key" ? newIdempotencyKey() : idempotencyKey;
            banner.show({ message: view.message, actionLabel: "다시 시도", onAction: () => send(nextKey, body), retryAfterSec: view.retryAfterSec ?? undefined });
            return;
          }
          // BOOKING_EXPIRED, BOOKING_STATE_CONFLICT, PAYMENT_IN_PROGRESS, PAYMENT_ATTEMPTS_EXHAUSTED는 재조회 뒤 상태 화면과 짧은 띠
          banner.notify(view.kind === "conflict" ? "다른 곳에서 먼저 바뀌었습니다." : view.message);
        },
      },
    );
  };

  // 클릭마다 새 키. mockMode는 APPROVE면 생략
  const pay = () => send(newIdempotencyKey(), mockMode === "APPROVE" ? {} : { mockMode });

  if (booking.isPending) return <Skeleton lines={8} />;
  if (booking.isError) return <QueryErrorNotice error={booking.error} onRetry={() => void booking.refetch()} retrying={booking.isFetching} />;
  if (entryStatus !== "HELD") return <Skeleton lines={4} />;

  const b = booking.data;
  const phase = paymentPhaseOf(b);
  const badge = BOOKING_BADGE[b.status];
  const region = property.data?.regionCode;
  const values: SearchValues = { regionCode: region && isRegionCode(region) ? region : "", checkIn: b.checkIn, checkOut: b.checkOut, guestCount: b.guestCount };
  const busy = payment.isPending || booking.isFetching;

  return (
    <div className="flex w-[720px] flex-col gap-5">
      <header className="flex items-start justify-between gap-4">
        <div className="flex flex-col gap-2">
          <div className="flex items-center gap-3">
            <h1 className="text-22 font-semibold">결제</h1>
            <StatusBadge tone={badge.tone}>{badge.label}</StatusBadge>
          </div>
          <p className="text-16 font-semibold">{roomType.data?.name ?? b.roomTypeId}</p>
          <StayConditions values={{ ...values, regionCode: "" }} />
        </div>
        {b.status === "HELD" ? (
          <div className="text-right">
            <p className="text-12 text-ink-3">남은 시간</p>
            <Countdown expiresAt={b.expiresAt} serverNow={b.serverNow} onZero={() => void booking.refetch()} className="text-22 font-semibold" />
          </div>
        ) : null}
      </header>

      {phase === "confirmed" ? (
        <Notice title="예약이 확정됐습니다" action={<Button onClick={onDetail}>예약 상세로</Button>}>
          결제가 승인됐습니다. 예약 상세에서 확정 내용을 볼 수 있습니다.
        </Notice>
      ) : null}

      {phase === "expired" ? (
        <Notice title="예약이 만료됐습니다" action={<Button onClick={() => onNewBooking(searchQueryOf(values))}>같은 조건으로 새 예약</Button>}>
          {EXPIRED_TEXT[b.expirationReason ?? ""] ?? "예약이 만료됐습니다."}
        </Notice>
      ) : null}

      {phase === "processing" ? (
        <Notice title="결제를 처리 중입니다" action={<Button variant="outline" onClick={() => void booking.refetch()} loading={booking.isFetching}>새로 고침</Button>}>
          결과가 오면 이 화면이 바뀝니다. 잠시 뒤 새로 고침하세요.
        </Notice>
      ) : null}

      {phase === "exhausted" ? <Notice title="결제 시도를 다 썼습니다">결제 시도 3회를 다 썼습니다. 예약 상세에서 상태를 확인하세요.</Notice> : null}

      <PriceBreakdownTable price={b.priceSnapshot} />

      <section className="flex flex-col gap-2">
        <h2 className="text-16 font-semibold">
          결제 시도 <span className="text-13 font-normal text-ink-3 tabular-nums">{b.payment.attemptCount}/{MAX_ATTEMPTS}</span>
        </h2>
        <PaymentAttemptsList attempts={b.payment.attempts} refund={b.payment.refund} />
      </section>

      {b.status === "HELD" ? (
        <div className="flex items-center justify-end gap-3 border-t border-line pt-4">
          {mockMode !== "APPROVE" ? <span className="text-12 text-ink-3">개발용: {mockMode}</span> : null}
          <Button variant="outline" onClick={onDetail} disabled={payment.isPending}>
            예약 상세로
          </Button>
          <Button size="lg" onClick={pay} loading={payment.isPending} disabled={busy || phase !== "payable"}>
            {b.payment.attemptCount > 0 ? "다시 결제하기" : "결제하기"}
          </Button>
        </div>
      ) : null}
    </div>
  );
}
