"use client";

import { useRouter, useSearchParams } from "next/navigation";
import { BookingList } from "@/components/guest/BookingList";
import { bookingFilterOf, type BookingFilter } from "@/components/guest/booking-text";

// G6 예약 목록. URL의 status와 page가 진실(계약 2절 G6 행). status는 화면이 네 값 밖을 만들지 않는다
export default function BookingsPage() {
  const router = useRouter();
  const params = useSearchParams();
  const page = Math.max(0, Number(params.get("page") ?? "0") || 0);
  const filter = bookingFilterOf(params.get("status"));

  const go = (nextFilter: BookingFilter, nextPage: number) => {
    const q = new URLSearchParams();
    if (nextFilter !== "ALL") q.set("status", nextFilter);
    if (nextPage > 0) q.set("page", String(nextPage));
    const s = q.toString();
    router.push(s ? `/bookings?${s}` : "/bookings");
  };

  return <BookingList filter={filter} page={page} onChange={go} onOpen={(id) => router.push(`/bookings/${id}`)} onSearch={() => router.push("/")} />;
}
