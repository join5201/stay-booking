"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import type { ReactNode } from "react";
import { useRoomType } from "@/lib/api/hooks";
import type { Period } from "@/lib/calendar";
import { MAX_PERIOD_DAYS } from "@/lib/calendar";
import { Button } from "../Button";
import { PeriodPicker } from "../PeriodPicker";
import { QueryErrorNotice } from "../QueryErrorNotice";

export type CalendarTab = "inventories" | "rates";

export interface RoomTypeCalendarShellProps {
  roomTypeId: string;
  tab: CalendarTab;
  period: Period;
  // 탭 링크에 붙는 ?from&to. 둘이 같은 기간을 본다
  query: string;
  onPeriodChange: (period: Period) => void;
  children: ReactNode;
}

const TABS: { key: CalendarTab; label: string }[] = [
  { key: "inventories", label: "재고" },
  { key: "rates", label: "요금" },
];

// H4와 H5의 공통 머리. CAT-08은 keep(staleTime 없음)이라 탭을 오가도 다시 부르지 않는다(인계 문서 113행). PeriodPicker는 최대 366일(P02)
export function RoomTypeCalendarShell({ roomTypeId, tab, period, query, onPeriodChange, children }: RoomTypeCalendarShellProps) {
  const router = useRouter();
  const roomType = useRoomType(roomTypeId, { keep: true });

  if (roomType.isError) {
    return (
      <QueryErrorNotice error={roomType.error} onRetry={() => void roomType.refetch()} retrying={roomType.isFetching} action={<Button variant="outline" onClick={() => router.push("/host/properties")}>내 숙소로</Button>} />
    );
  }

  return (
    <div className="flex flex-col gap-5">
      <header>
        <p className="text-13 text-ink-3">
          <Link href="/host/properties" className="hover:underline">
            내 숙소
          </Link>
          {" / "}
          {roomType.data ? (
            <Link href={`/host/properties/${roomType.data.propertyId}/room-types`} className="hover:underline">
              객실 타입
            </Link>
          ) : (
            "객실 타입"
          )}
          {" / "}
          {roomType.data?.name ?? roomTypeId}
        </p>
        <h1 className="text-22 font-semibold">{roomType.data ? `${roomType.data.name} 달력` : "달력"}</h1>
        {roomType.data ? <p className="mt-1 text-13 text-ink-3 tabular-nums">최대 인원 {roomType.data.maxOccupancy}명</p> : null}
      </header>

      <nav aria-label="달력 종류" className="flex gap-6 border-b border-line">
        {TABS.map((t) => {
          const active = t.key === tab;
          return (
            <Link
              key={t.key}
              href={`/host/room-types/${roomTypeId}/${t.key}${query}`}
              aria-current={active ? "page" : undefined}
              className={`flex h-10 items-center border-b-2 text-15 font-semibold ${active ? "border-devbar text-ink" : "border-transparent text-ink-3 hover:text-ink"}`}
            >
              {t.label}
            </Link>
          );
        })}
      </nav>

      {/* 기간이 URL에서 바뀌면 입력 초안도 그 값으로 */}
      <PeriodPicker key={`${period.from}/${period.to}`} value={period} onChange={onPeriodChange} maxDays={MAX_PERIOD_DAYS} />

      {children}
    </div>
  );
}
