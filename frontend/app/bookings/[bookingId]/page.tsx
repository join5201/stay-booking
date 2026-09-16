"use client";

import { useParams, useRouter } from "next/navigation";
import { BookingDetail } from "@/components/guest/BookingDetail";

// G7 예약 상세(계약 2절 G7 행)
export default function BookingPage() {
  const { bookingId } = useParams<{ bookingId: string }>();
  const router = useRouter();
  return <BookingDetail bookingId={bookingId} onPay={() => router.push(`/bookings/${bookingId}/pay`)} onList={() => router.push("/bookings")} />;
}
