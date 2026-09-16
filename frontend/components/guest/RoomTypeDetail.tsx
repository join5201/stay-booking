"use client";

import Link from "next/link";
import { useApplicablePromotions, useAvailability, useQuote, useRoomType } from "@/lib/api/hooks";
import type { Role } from "@/lib/dev-actor";
import { searchQueryOf, stayComplete, stayParamsOf } from "@/lib/stay";
import { Button } from "../Button";
import { useDevActor } from "../DevActorProvider";
import { PriceBreakdownTable } from "../PriceBreakdownTable";
import { QueryErrorNotice } from "../QueryErrorNotice";
import type { SearchValues } from "../SearchForm";
import { Skeleton } from "../Skeleton";
import { ApplicablePromotionList } from "./ApplicablePromotionList";
import { AvailabilityTable } from "./AvailabilityTable";
import { StayConditions } from "./StayConditions";

export interface RoomTypeDetailProps {
  roomTypeId: string;
  values: SearchValues;
  onReserve: (query: string) => void;
}

export interface ReserveInputs {
  complete: boolean;
  loading: boolean;
  failed: boolean;
  available: boolean | null;
  role: Role;
}

// 예약 버튼을 막는 이유(W13). 조건 미완성, 셋 중 하나 실패, available false, public 행위자. 없으면 null
export function reserveBlockReason(input: ReserveInputs): string | null {
  if (!input.complete) return "날짜와 인원을 정하면 예약할 수 있습니다.";
  if (input.loading) return "가용 수와 예상 금액을 확인하는 중입니다.";
  if (input.failed) return "가용 수나 예상 금액을 못 읽어 예약할 수 없습니다.";
  if (input.available === false) return "이 조건으로는 예약할 수 없습니다. 날짜별 사유를 보세요.";
  if (input.role === "public") return "예약하려면 위의 개발용 바에서 게스트 행위자를 고르세요.";
  return null;
}

// G3 객실 상세. CAT-08 진입 시. 조건 셋(체크인, 체크아웃, 인원)이 있으면 SEARCH-02와 SEARCH-03과 PROMO-05를 동시에.
// regionCode는 세 API가 안 받으므로 검색으로 돌아갈 때만 쓴다. 예상 금액 고정 문구는 PriceBreakdownTable의 estimate
export function RoomTypeDetail({ roomTypeId, values, onReserve }: RoomTypeDetailProps) {
  const { role } = useDevActor();
  const complete = stayComplete(values);
  const stay = complete ? stayParamsOf(values) : null;
  const query = searchQueryOf(values);
  const roomType = useRoomType(roomTypeId);
  const availability = useAvailability(roomTypeId, stay);
  const quote = useQuote(roomTypeId, stay);
  const promotions = useApplicablePromotions(roomTypeId, stay);

  if (roomType.isPending) return <Skeleton lines={6} />;
  if (roomType.isError) {
    return <QueryErrorNotice error={roomType.error} onRetry={() => void roomType.refetch()} retrying={roomType.isFetching} action={<Link href={`/${query}`} className="text-devbar hover:underline">검색으로</Link>} />;
  }
  const rt = roomType.data;
  const three = [availability, quote, promotions];
  const reason = reserveBlockReason({
    complete,
    loading: complete && three.some((q) => q.isPending),
    failed: three.some((q) => q.isError),
    available: availability.data ? availability.data.available : null,
    role,
  });

  return (
    <div className="flex flex-col gap-5">
      <nav aria-label="경로" className="text-13 text-ink-3">
        <Link href={`/${query}`} className="hover:underline">
          검색
        </Link>
        <span className="mx-2">/</span>
        <Link href={`/properties/${rt.propertyId}${query}`} className="hover:underline">
          숙소
        </Link>
        <span className="mx-2">/</span>
        <span>{rt.name}</span>
      </nav>

      <header className="flex items-start justify-between gap-4">
        <div className="flex flex-col gap-2">
          <h1 className="text-22 font-semibold">{rt.name}</h1>
          <p className="text-14 text-ink-2">최대 {rt.maxOccupancy}명{rt.description ? `. ${rt.description}` : ""}</p>
          <StayConditions values={values} />
        </div>
        <div className="flex flex-col items-end gap-1">
          <Button size="lg" disabled={!!reason} onClick={() => onReserve(query)}>
            예약하기
          </Button>
          {reason ? (
            <p role="note" className="max-w-[260px] text-right text-12 text-ink-3">
              {reason}
            </p>
          ) : null}
        </div>
      </header>

      {complete ? (
        <div className="grid grid-cols-2 gap-5">
          <section className="flex flex-col gap-2">
            <h2 className="text-16 font-semibold">날짜별 남은 객실</h2>
            {availability.isError ? (
              <QueryErrorNotice error={availability.error} onRetry={() => void availability.refetch()} retrying={availability.isFetching} />
            ) : availability.isPending ? (
              <Skeleton lines={4} />
            ) : (
              <AvailabilityTable availability={availability.data} maxOccupancy={rt.maxOccupancy} />
            )}
          </section>

          <section className="flex flex-col gap-2">
            <h2 className="text-16 font-semibold">예상 금액</h2>
            {quote.isError ? (
              <QueryErrorNotice error={quote.error} onRetry={() => void quote.refetch()} retrying={quote.isFetching} />
            ) : quote.isPending ? (
              <Skeleton lines={4} />
            ) : (
              <PriceBreakdownTable price={quote.data.price} estimate />
            )}
          </section>

          <section className="col-span-2 flex flex-col gap-2">
            <h2 className="text-16 font-semibold">적용 가능한 프로모션</h2>
            {promotions.isError ? (
              <QueryErrorNotice error={promotions.error} onRetry={() => void promotions.refetch()} retrying={promotions.isFetching} />
            ) : promotions.isPending ? (
              <Skeleton lines={2} />
            ) : (
              <ApplicablePromotionList promotions={promotions.data} />
            )}
          </section>
        </div>
      ) : null}
    </div>
  );
}
