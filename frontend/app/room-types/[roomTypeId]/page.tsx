"use client";

import { useParams, useRouter, useSearchParams } from "next/navigation";
import { RoomTypeDetail } from "@/components/guest/RoomTypeDetail";
import { searchValuesOf } from "@/lib/stay";

// G3 객실 상세. 검색 쿼리를 유지하고 예약하기는 G4로 쿼리 그대로(계약 2절 G3 행)
export default function RoomTypePage() {
  const { roomTypeId } = useParams<{ roomTypeId: string }>();
  const router = useRouter();
  const params = useSearchParams();
  return <RoomTypeDetail roomTypeId={roomTypeId} values={searchValuesOf(params)} onReserve={(query) => router.push(`/room-types/${roomTypeId}/book${query}`)} />;
}
