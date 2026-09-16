"use client";

import { useParams } from "next/navigation";
import { InventoryCalendar } from "@/components/host/InventoryCalendar";
import { RoomTypeCalendarShell } from "@/components/host/RoomTypeCalendarShell";
import { useCalendarPeriod } from "@/components/host/useCalendarPeriod";

// H4 재고 달력. /host/room-types/[roomTypeId]/inventories?from&to(양끝 포함). 머리와 기간은 H5와 공유
export default function InventoriesPage() {
  const { roomTypeId } = useParams<{ roomTypeId: string }>();
  const { period, query, setPeriod } = useCalendarPeriod();
  return (
    <RoomTypeCalendarShell roomTypeId={roomTypeId} tab="inventories" period={period} query={query} onPeriodChange={setPeriod}>
      <InventoryCalendar roomTypeId={roomTypeId} period={period} />
    </RoomTypeCalendarShell>
  );
}
