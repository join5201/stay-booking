"use client";

import { useMemo, useState } from "react";
import { useRates } from "@/lib/api/hooks";
import { apiPeriodOf, previousItem, type Period } from "@/lib/calendar";
import { CalendarGrid } from "../CalendarGrid";
import { Money } from "../Money";
import { QueryErrorNotice } from "../QueryErrorNotice";
import { Skeleton } from "../Skeleton";
import { RateCellPanel } from "./RateCellPanel";

export interface RateCalendarProps {
  roomTypeId: string;
  period: Period;
}

// H5 몸통. RATE-03 기간 조회와 달력. 일괄 등록 API가 없어 날짜별 등록 안내(인계 문서 Q3)
export function RateCalendar({ roomTypeId, period }: RateCalendarProps) {
  const api = apiPeriodOf(period);
  const range = useRates(roomTypeId, api.from, api.to);
  const [selected, setSelected] = useState<string | null>(null);

  const items = useMemo(() => range.data?.items ?? [], [range.data]);
  const byDate = useMemo(() => new Map(items.map((i) => [i.date, i])), [items]);

  if (range.isError) return <QueryErrorNotice error={range.error} onRetry={() => void range.refetch()} retrying={range.isFetching} />;
  if (range.isPending) return <Skeleton lines={8} />;

  return (
    <div className="flex flex-col gap-4">
      <p className="text-13 text-ink-3 tabular-nums">
        등록 {items.length}일, 미등록 {range.data.missingDates.length}일. 요금은 날짜별로 등록합니다. 일괄 등록은 없습니다.
      </p>

      <CalendarGrid
        from={period.from}
        to={period.to}
        selectedDate={selected}
        onSelect={setSelected}
        renderCell={(date) => {
          const item = byDate.get(date);
          return item ? <Money amount={item.amount} className="font-semibold" /> : <span className="text-ink-4">미등록</span>;
        }}
      />

      {selected ? <RateCellPanel key={selected} roomTypeId={roomTypeId} date={selected} exists={byDate.has(selected)} previous={previousItem(items, selected)} onClose={() => setSelected(null)} /> : null}
    </div>
  );
}
