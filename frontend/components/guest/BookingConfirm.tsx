"use client";

import { useRef, useState } from "react";
import { newIdempotencyKey } from "@/lib/api/client";
import { useQuote, useRequestBooking, useRoomType } from "@/lib/api/hooks";
import type { RequestBookingBody } from "@/lib/api/types";
import { errorViewOf } from "@/lib/error-view";
import { isApiError, retryPolicyOf } from "@/lib/errors";
import { stayParamsOf } from "@/lib/stay";
import { useBanner } from "../Banner";
import { Button } from "../Button";
import { Money } from "../Money";
import { Notice } from "../Notice";
import { PriceBreakdownTable } from "../PriceBreakdownTable";
import { QueryErrorNotice } from "../QueryErrorNotice";
import type { SearchValues } from "../SearchForm";
import { Skeleton } from "../Skeleton";
import { StayConditions } from "./StayConditions";

export interface BookingConfirmProps {
  roomTypeId: string;
  // 조건 셋이 다 있는 값. 없으면 페이지가 G3로 보낸다
  values: SearchValues;
  onHeld: (bookingId: string) => void;
  onBack: () => void;
}

// G4 예약 확인. 진입 시 SEARCH-03을 다시 받아 그 값을 expectedTotalAmount로(P06). 확인 버튼이 BOOK-01.
// 멱등키는 마운트 시 하나. PRICE_CHANGED 뒤 다시 확인은 새 키, 네트워크와 5xx와 REQUEST_IN_PROGRESS 재시도는 같은 키(계약 6절 멱등키 행, W09)
export function BookingConfirm({ roomTypeId, values, onHeld, onBack }: BookingConfirmProps) {
  const banner = useBanner();
  const stay = stayParamsOf(values);
  const roomType = useRoomType(roomTypeId);
  const quote = useQuote(roomTypeId, stay, { always: true });
  const request = useRequestBooking();
  const keyRef = useRef(newIdempotencyKey());
  // 화면 안 Notice 문구. PRICE_CHANGED는 새 금액으로 다시 확인 버튼이 붙는다
  const [notice, setNotice] = useState<{ message: string; priceChanged: boolean } | null>(null);

  const submit = (body: RequestBookingBody) => {
    setNotice(null);
    request.mutate(
      { idempotencyKey: keyRef.current, body },
      {
        onSuccess: (result) => onHeld(result.data.id),
        onError: (error) => {
          const view = errorViewOf(error);
          if (view.kind === "banner") {
            // 같은 키로 다시 시도(같은 요청의 재전송). 새 키 정책이면 다음 시도 전에 키를 바꾼다
            if (retryPolicyOf(error) === "new-key") keyRef.current = newIdempotencyKey();
            banner.show({ message: view.message, actionLabel: "다시 시도", onAction: () => submit(body), retryAfterSec: view.retryAfterSec ?? undefined });
            return;
          }
          if (isApiError(error) && error.code === "PRICE_CHANGED") {
            // 예약과 Hold는 안 생겼다. SEARCH-03을 다시 받고 새 금액으로 다시 확인은 새 키
            keyRef.current = newIdempotencyKey();
            void quote.refetch();
            setNotice({ message: view.kind === "notice" ? view.message : "금액이 바뀌었습니다.", priceChanged: true });
            return;
          }
          setNotice({ message: view.kind === "field" || view.kind === "notice" || view.kind === "notFound" ? view.message : "예약을 만들지 못했습니다.", priceChanged: false });
        },
      },
    );
  };

  if (roomType.isPending || quote.isPending) return <Skeleton lines={8} />;
  if (roomType.isError) return <QueryErrorNotice error={roomType.error} onRetry={() => void roomType.refetch()} retrying={roomType.isFetching} action={<Button variant="outline" onClick={onBack}>객실로</Button>} />;
  if (quote.isError) return <QueryErrorNotice error={quote.error} onRetry={() => void quote.refetch()} retrying={quote.isFetching} action={<Button variant="outline" onClick={onBack}>객실로</Button>} />;

  const price = quote.data.price;
  const body: RequestBookingBody = {
    roomTypeId,
    checkIn: quote.data.checkIn,
    checkOut: quote.data.checkOut,
    guestCount: quote.data.guestCount,
    expectedTotalAmount: price.totalAmount,
    currency: "KRW",
  };
  const busy = request.isPending;

  return (
    <div className="flex w-[720px] flex-col gap-5">
      <header className="flex flex-col gap-2">
        <h1 className="text-22 font-semibold">예약 확인</h1>
        <p className="text-16 font-semibold">{roomType.data.name}</p>
        <StayConditions values={values} />
      </header>

      <PriceBreakdownTable price={price} estimate />

      {notice ? (
        <Notice
          title={notice.priceChanged ? "금액이 바뀌었습니다" : "예약을 만들지 못했습니다"}
          action={
            notice.priceChanged ? (
              <Button onClick={() => submit(body)} loading={busy} disabled={quote.isFetching}>
                <Money amount={price.totalAmount} />
                으로 다시 확인
              </Button>
            ) : undefined
          }
        >
          {notice.message}
        </Notice>
      ) : null}

      <p className="text-13 text-ink-3">확인하면 객실을 잠시 잡아 두고 결제로 갑니다. 남은 시간 안에 결제하지 않으면 예약이 만료됩니다.</p>

      <div className="flex justify-end gap-2 border-t border-line pt-4">
        <Button variant="outline" onClick={onBack} disabled={busy}>
          돌아가기
        </Button>
        <Button size="lg" onClick={() => submit(body)} loading={busy} disabled={busy || quote.isFetching || notice?.priceChanged === true}>
          <Money amount={price.totalAmount} />
          에 예약 확인
        </Button>
      </div>
    </div>
  );
}
