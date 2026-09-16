"use client";

import { useParams, useRouter } from "next/navigation";
import { useCallback } from "react";
import { PaymentScreen } from "@/components/guest/PaymentScreen";

// G5 결제. HELD 아니면 G7로 replace(계약 2절 G5 행)
export default function PayPage() {
  const { bookingId } = useParams<{ bookingId: string }>();
  const router = useRouter();
  const detail = `/bookings/${bookingId}`;
  const replaceDetail = useCallback(() => router.replace(detail), [router, detail]);
  return <PaymentScreen bookingId={bookingId} onDetail={() => router.push(detail)} onReplaceDetail={replaceDetail} onNewBooking={(query) => router.push(`/${query}`)} />;
}
