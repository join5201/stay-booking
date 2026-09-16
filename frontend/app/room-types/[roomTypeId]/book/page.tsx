"use client";

import { useParams, useRouter, useSearchParams } from "next/navigation";
import { useEffect } from "react";
import { BookingConfirm } from "@/components/guest/BookingConfirm";
import { Skeleton } from "@/components/Skeleton";
import { searchQueryOf, searchValuesOf, stayComplete } from "@/lib/stay";

// G4 예약 확인. 쿼리가 없으면 G3로 replace(계약 2절 G4 행). 201 HELD면 G5로
export default function BookPage() {
  const { roomTypeId } = useParams<{ roomTypeId: string }>();
  const router = useRouter();
  const params = useSearchParams();
  const values = searchValuesOf(params);
  const complete = stayComplete(values);
  const query = searchQueryOf(values);

  useEffect(() => {
    if (!complete) router.replace(`/room-types/${roomTypeId}${query}`);
  }, [complete, query, roomTypeId, router]);

  if (!complete) return <Skeleton lines={4} />;
  return <BookingConfirm roomTypeId={roomTypeId} values={values} onHeld={(bookingId) => router.push(`/bookings/${bookingId}/pay`)} onBack={() => router.push(`/room-types/${roomTypeId}${query}`)} />;
}
