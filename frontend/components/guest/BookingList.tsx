"use client";

import { useBookings, useProperty, useRoomType } from "@/lib/api/hooks";
import type { Booking } from "@/lib/api/types";
import { Button } from "../Button";
import { DataTable, type Column } from "../DataTable";
import { DateText } from "../DateText";
import { EmptyState } from "../EmptyState";
import { Money } from "../Money";
import { Pagination } from "../Pagination";
import { QueryErrorNotice } from "../QueryErrorNotice";
import { SegmentedFilter } from "../SegmentedFilter";
import { BOOKING_BADGE, StatusBadge } from "../StatusBadge";
import { BOOKING_FILTERS, type BookingFilter, stayTextOf } from "./booking-text";

export const BOOKING_PAGE_SIZE = 20;

export interface BookingListProps {
  filter: BookingFilter;
  page: number;
  onChange: (filter: BookingFilter, page: number) => void;
  onOpen: (bookingId: string) => void;
  onSearch: () => void;
}

// 줄마다 CAT-03과 CAT-08로 이름(인계 문서 Q1, 계약 7절 D-5). staleTime 60초라 같은 숙소와 객실은 한 번
function NamesCell({ propertyId, roomTypeId }: { propertyId: string; roomTypeId: string }) {
  const property = useProperty(propertyId);
  const roomType = useRoomType(roomTypeId);
  return (
    <div className="flex flex-col">
      <span className="font-semibold">{roomType.data?.name ?? (roomType.isError ? roomTypeId : "…")}</span>
      <span className="text-13 text-ink-3">{property.data?.name ?? (property.isError ? propertyId : "…")}</span>
    </div>
  );
}

// G6 예약 목록. BOOK-02에 status와 page. 필터는 다섯 고정. 401 ACTOR_REQUIRED는 Notice가 개발용 바를 가리킨다
export function BookingList({ filter, page, onChange, onOpen, onSearch }: BookingListProps) {
  const list = useBookings({ page, size: BOOKING_PAGE_SIZE, ...(filter === "ALL" ? {} : { status: filter }) });

  const columns: Column<Booking>[] = [
    { key: "names", header: "객실과 숙소", render: (b) => <NamesCell propertyId={b.propertyId} roomTypeId={b.roomTypeId} /> },
    { key: "stay", header: "숙박", width: "300px", render: (b) => <span className="tabular-nums text-ink-2">{stayTextOf(b)}</span> },
    { key: "amount", header: "금액", width: "110px", align: "right", render: (b) => <Money amount={b.priceSnapshot.totalAmount} /> },
    {
      key: "status",
      header: "상태",
      width: "80px",
      render: (b) => {
        const badge = BOOKING_BADGE[b.status];
        return <StatusBadge tone={badge.tone}>{badge.label}</StatusBadge>;
      },
    },
    { key: "createdAt", header: "요청 시각", width: "150px", render: (b) => <DateText value={b.createdAt} className="text-13 text-ink-3" /> },
    {
      key: "detail",
      header: "",
      width: "70px",
      align: "right",
      render: (b) => (
        <button type="button" onClick={(e) => { e.stopPropagation(); onOpen(b.id); }} className="text-13 text-devbar hover:underline">
          상세
        </button>
      ),
    },
  ];

  return (
    <div className="flex flex-col gap-5">
      <header className="flex items-center justify-between">
        <h1 className="text-22 font-semibold">내 예약</h1>
        <SegmentedFilter options={BOOKING_FILTERS} value={filter} onChange={(v) => onChange(v, 0)} ariaLabel="예약 상태" />
      </header>

      {list.isError ? (
        <QueryErrorNotice error={list.error} onRetry={() => void list.refetch()} retrying={list.isFetching} />
      ) : (
        <>
          <DataTable
            columns={columns}
            rows={list.data?.items ?? []}
            rowKey={(b) => b.id}
            loading={list.isPending}
            onRowClick={(b) => onOpen(b.id)}
            empty={<EmptyState title="예약이 없습니다" description="숙소를 찾아 날짜와 인원을 정하면 예약할 수 있습니다." action={<Button onClick={onSearch}>숙소 찾기</Button>} />}
          />
          {list.data && list.data.totalPages > 1 ? <Pagination page={list.data.page} totalPages={list.data.totalPages} onChange={(next) => onChange(filter, next)} disabled={list.isFetching} /> : null}
        </>
      )}
    </div>
  );
}
