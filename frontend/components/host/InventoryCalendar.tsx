"use client";

import { useMemo, useState } from "react";
import { useInventories } from "@/lib/api/hooks";
import type { DailyInventory } from "@/lib/api/types";
import { apiPeriodOf, previousItem, type Period } from "@/lib/calendar";
import { Button } from "../Button";
import { CalendarGrid } from "../CalendarGrid";
import { QueryErrorNotice } from "../QueryErrorNotice";
import { Skeleton } from "../Skeleton";
import { InventoryBulkPanel } from "./InventoryBulkPanel";
import { InventoryCellPanel } from "./InventoryCellPanel";

export interface InventoryCalendarProps {
  roomTypeId: string;
  // 양끝 포함. API에는 to에 하루를 더해 보낸다
  period: Period;
}

// H4 몸통. INV-04 기간 조회와 달력. 칸을 누르면 패널(누락이면 등록, 있으면 수정), 일괄 등록은 따로 패널
export function InventoryCalendar({ roomTypeId, period }: InventoryCalendarProps) {
  const api = apiPeriodOf(period);
  const range = useInventories(roomTypeId, api.from, api.to);
  const [selected, setSelected] = useState<string | null>(null);
  const [bulkOpen, setBulkOpen] = useState(false);

  const items = useMemo(() => range.data?.items ?? [], [range.data]);
  const byDate = useMemo(() => new Map(items.map((i) => [i.date, i])), [items]);
  const existingDates = useMemo(() => items.map((i) => i.date), [items]);

  if (range.isError) return <QueryErrorNotice error={range.error} onRetry={() => void range.refetch()} retrying={range.isFetching} />;
  if (range.isPending) return <Skeleton lines={8} />;

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between">
        <p className="text-13 text-ink-3 tabular-nums">
          등록 {items.length}일, 미등록 {range.data.missingDates.length}일. 칸을 누르면 등록하거나 수정합니다.
        </p>
        <Button variant="outline" onClick={() => setBulkOpen(true)}>
          일괄 등록
        </Button>
      </div>

      <CalendarGrid
        from={period.from}
        to={period.to}
        selectedDate={selected}
        onSelect={setSelected}
        renderCell={(date) => {
          const item = byDate.get(date);
          return item ? <InventoryCell item={item} /> : <span className="text-ink-4">미등록</span>;
        }}
      />

      {selected ? (
        <InventoryCellPanel key={selected} roomTypeId={roomTypeId} date={selected} exists={byDate.has(selected)} previous={previousItem(items, selected)} onClose={() => setSelected(null)} />
      ) : null}
      {bulkOpen ? <InventoryBulkPanel roomTypeId={roomTypeId} period={period} existingDates={existingDates} refreshing={range.isFetching} onClose={() => setBulkOpen(false)} /> : null}
    </div>
  );
}

// 칸 안의 재고 넷. 총과 가용을 크게, 선점과 판매를 작게
function InventoryCell({ item }: { item: DailyInventory }) {
  return (
    <div className="tabular-nums">
      <div className="flex items-baseline gap-1">
        <span className="font-semibold">{item.availableCount}</span>
        <span className="text-11 text-ink-3">/ 총 {item.totalCount}</span>
      </div>
      <div className="text-11 text-ink-3">
        선점 {item.heldCount} 판매 {item.soldCount}
      </div>
    </div>
  );
}
