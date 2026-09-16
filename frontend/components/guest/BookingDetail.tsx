"use client";

import Link from "next/link";
import { useState } from "react";
import { newIdempotencyKey } from "@/lib/api/client";
import { useBooking, useCancelBooking, useProperty, useRoomType } from "@/lib/api/hooks";
import type { Booking } from "@/lib/api/types";
import { errorViewOf } from "@/lib/error-view";
import { retryPolicyOf } from "@/lib/errors";
import { isRegionCode } from "@/lib/regions";
import { seoulDateOf } from "@/lib/seoul-time";
import { searchQueryOf } from "@/lib/stay";
import { useBanner } from "../Banner";
import { Button } from "../Button";
import { ConfirmSheet } from "../ConfirmSheet";
import { Countdown } from "../Countdown";
import { DateText } from "../DateText";
import { Field } from "../Field";
import { Notice } from "../Notice";
import { PaymentAttemptsList } from "../PaymentAttemptsList";
import { PriceBreakdownTable } from "../PriceBreakdownTable";
import { QueryErrorNotice } from "../QueryErrorNotice";
import { Skeleton } from "../Skeleton";
import { BOOKING_BADGE, StatusBadge } from "../StatusBadge";
import { TextArea } from "../TextInput";
import { MAX_ATTEMPTS } from "./PaymentScreen";
import { expiredTextOf, stayTextOf } from "./booking-text";

export const CANCEL_REASON_MAX = 300;

// W15. CONFIRMED이고 체크인 서울 날짜가 serverNow 서울 날짜보다 뒤일 때만. 체크인은 숙박 날짜 문자열 그대로, serverNow는 서울 날짜로 바꿔 문자열끼리 비교
export function cancelAllowed(b: Pick<Booking, "status" | "checkIn" | "serverNow">): boolean {
  return b.status === "CONFIRMED" && b.checkIn > seoulDateOf(b.serverNow);
}

export interface BookingDetailProps {
  bookingId: string;
  // HELD의 결제하기(G5)
  onPay: () => void;
  onList: () => void;
}

// G7 예약 상세. BOOK-03 진입 시와 Countdown 0 도달 시 1회와 새로 고침. 주기 폴링 없음.
// 취소 시트가 열릴 때 키 하나, 닫으면 폐기. 확인이 BOOK-04(reason 300자 이하). 성공이나 replayed 뒤 BOOK-03 재조회는 훅의 onSettled
export function BookingDetail({ bookingId, onPay, onList }: BookingDetailProps) {
  const banner = useBanner();
  const booking = useBooking(bookingId);
  const property = useProperty(booking.data?.propertyId);
  const roomType = useRoomType(booking.data?.roomTypeId);
  const cancel = useCancelBooking(bookingId);

  // 시트가 열려 있는 동안의 키. null이면 닫힘
  const [cancelKey, setCancelKey] = useState<string | null>(null);
  const [reason, setReason] = useState("");
  const [reasonError, setReasonError] = useState<string | null>(null);

  const openSheet = () => {
    setReason("");
    setReasonError(null);
    setCancelKey(newIdempotencyKey());
  };
  const closeSheet = () => setCancelKey(null);

  const send = (idempotencyKey: string) => {
    const trimmed = reason.trim();
    cancel.mutate(
      { idempotencyKey, body: trimmed ? { reason: trimmed } : {} },
      {
        onSuccess: () => {
          setCancelKey(null);
          banner.notify("예약을 취소했습니다.");
        },
        onError: (error) => {
          const view = errorViewOf(error);
          if (view.kind === "field") {
            setReasonError(view.fields.reason ?? view.message);
            return;
          }
          if (view.kind === "banner") {
            // 네트워크와 REQUEST_IN_PROGRESS는 같은 키. 키 결함 코드는 새 키
            const nextKey = retryPolicyOf(error) === "new-key" ? newIdempotencyKey() : idempotencyKey;
            setCancelKey(nextKey);
            banner.show({ message: view.message, actionLabel: "다시 시도", onAction: () => send(nextKey), retryAfterSec: view.retryAfterSec ?? undefined });
            return;
          }
          // CANCELLATION_NOT_ALLOWED와 BOOKING_STATE_CONFLICT는 재조회(onSettled) 뒤 상태 화면과 짧은 띠
          setCancelKey(null);
          banner.notify(view.kind === "conflict" ? "다른 곳에서 먼저 바뀌었습니다." : view.message);
        },
      },
    );
  };

  if (booking.isPending) return <Skeleton lines={8} />;
  if (booking.isError) return <QueryErrorNotice error={booking.error} onRetry={() => void booking.refetch()} retrying={booking.isFetching} action={<Button variant="outline" onClick={onList}>목록으로</Button>} />;

  const b = booking.data;
  const badge = BOOKING_BADGE[b.status];
  const region = property.data?.regionCode;
  const propertyQuery = searchQueryOf({ regionCode: region && isRegionCode(region) ? region : "", checkIn: b.checkIn, checkOut: b.checkOut, guestCount: b.guestCount });
  const allowed = cancelAllowed(b);
  const tooLong = reason.length > CANCEL_REASON_MAX;

  return (
    <div className="flex w-[720px] flex-col gap-5">
      <nav aria-label="경로" className="text-13 text-ink-3">
        <Link href="/bookings" className="hover:underline">
          내 예약
        </Link>
        <span className="mx-1">/</span>
        <span>예약 상세</span>
      </nav>

      <header className="flex items-start justify-between gap-4">
        <div className="flex flex-col gap-2">
          <div className="flex items-center gap-3">
            <h1 className="text-22 font-semibold">예약 상세</h1>
            <StatusBadge tone={badge.tone}>{badge.label}</StatusBadge>
          </div>
          <p className="text-16 font-semibold">{roomType.data?.name ?? b.roomTypeId}</p>
          <p className="text-14 text-ink-2">
            <Link href={`/properties/${b.propertyId}${propertyQuery}`} className="hover:underline">
              {property.data?.name ?? b.propertyId}
            </Link>
          </p>
          <p className="text-14 tabular-nums text-ink-2">{stayTextOf(b)}</p>
        </div>
        <div className="flex flex-col items-end gap-2">
          {b.status === "HELD" ? (
            <div className="text-right">
              <p className="text-12 text-ink-3">남은 시간</p>
              <Countdown expiresAt={b.expiresAt} serverNow={b.serverNow} onZero={() => void booking.refetch()} className="text-22 font-semibold" />
            </div>
          ) : null}
          <Button variant="outline" onClick={() => void booking.refetch()} loading={booking.isFetching}>
            새로 고침
          </Button>
        </div>
      </header>

      {b.status === "HELD" ? (
        <Notice title="결제를 기다리는 예약입니다" action={<Button onClick={onPay}>결제하기</Button>}>
          남은 시간 안에 결제하지 않으면 예약이 만료됩니다.
        </Notice>
      ) : null}

      {b.status === "CONFIRMED" ? (
        <Notice
          title="예약이 확정됐습니다"
          action={
            <div className="flex items-center gap-3">
              <Button variant="outline" onClick={openSheet} disabled={!allowed}>
                예약 취소
              </Button>
              {allowed ? null : <span role="note" className="text-13 text-ink-3">체크인 날짜가 지나 취소할 수 없습니다.</span>}
            </div>
          }
        >
          확정 시각 <DateText value={b.confirmedAt} />. 체크인 전날까지 취소할 수 있습니다.
        </Notice>
      ) : null}

      {b.status === "EXPIRED" ? <Notice title="예약이 만료됐습니다">{expiredTextOf(b.expirationReason)}</Notice> : null}

      {b.status === "CANCELED" ? (
        <Notice title="예약이 취소됐습니다">
          취소 시각 <DateText value={b.canceledAt} />. {b.cancellationReason ? `사유: ${b.cancellationReason}` : "사유 없음."}
        </Notice>
      ) : null}

      <PriceBreakdownTable price={b.priceSnapshot} />

      <section className="flex flex-col gap-2">
        <h2 className="text-16 font-semibold">
          결제 시도 <span className="text-13 font-normal text-ink-3 tabular-nums">{b.payment.attemptCount}/{MAX_ATTEMPTS}</span>
        </h2>
        <PaymentAttemptsList attempts={b.payment.attempts} refund={b.payment.refund} />
      </section>

      <section className="flex flex-col gap-2">
        <h2 className="text-16 font-semibold">예약 정보</h2>
        <dl className="grid grid-cols-[140px_1fr] gap-x-4 gap-y-1 rounded-card border border-line bg-surface px-4 py-3 text-14">
          <dt className="text-ink-3">예약 번호</dt>
          <dd className="font-mono text-13">{b.id}</dd>
          <dt className="text-ink-3">요청 시각</dt>
          <dd><DateText value={b.createdAt} /></dd>
          {b.status === "HELD" ? (
            <>
              <dt className="text-ink-3">만료 예정</dt>
              <dd><DateText value={b.expiresAt} /></dd>
            </>
          ) : null}
          {b.confirmedAt ? (
            <>
              <dt className="text-ink-3">확정 시각</dt>
              <dd><DateText value={b.confirmedAt} /></dd>
            </>
          ) : null}
          {b.canceledAt ? (
            <>
              <dt className="text-ink-3">취소 시각</dt>
              <dd><DateText value={b.canceledAt} /></dd>
            </>
          ) : null}
          {b.expiredAt ? (
            <>
              <dt className="text-ink-3">만료 시각</dt>
              <dd><DateText value={b.expiredAt} /></dd>
            </>
          ) : null}
        </dl>
      </section>

      <ConfirmSheet
        open={cancelKey !== null}
        title="예약을 취소할까요?"
        description="확정된 예약을 취소하면 승인된 결제가 환불됩니다. 되돌릴 수 없습니다."
        confirmLabel="예약 취소"
        cancelLabel="돌아가기"
        onConfirm={() => cancelKey && send(cancelKey)}
        onCancel={closeSheet}
        busy={cancel.isPending}
        confirmDisabled={tooLong}
      >
        <Field label="취소 사유" hint={`${reason.length}/${CANCEL_REASON_MAX}. 비워도 됩니다.`} error={reasonError ?? (tooLong ? `취소 사유는 ${CANCEL_REASON_MAX}자 이하입니다.` : undefined)}>
          <TextArea value={reason} onValueChange={(v) => { setReason(v); setReasonError(null); }} invalid={tooLong || !!reasonError} rows={3} />
        </Field>
      </ConfirmSheet>
    </div>
  );
}
