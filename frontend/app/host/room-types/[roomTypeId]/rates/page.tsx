"use client";

import { useParams } from "next/navigation";
import { RateCalendar } from "@/components/host/RateCalendar";
import { RoomTypeCalendarShell } from "@/components/host/RoomTypeCalendarShell";
import { useCalendarPeriod } from "@/components/host/useCalendarPeriod";

// H5 요금 달력. /host/room-types/[roomTypeId]/rates?from&to(양끝 포함). 머리와 기간은 H4와 공유
export default function RatesPage() {
  const { roomTypeId } = useParams<{ roomTypeId: string }>();
  const { period, query, setPeriod } = useCalendarPeriod();
  return (
    <RoomTypeCalendarShell roomTypeId={roomTypeId} tab="rates" period={period} query={query} onPeriodChange={setPeriod}>
      <RateCalendar roomTypeId={roomTypeId} period={period} />
    </RoomTypeCalendarShell>
  );
}
