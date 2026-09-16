"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "../client";
import { keys } from "../keys";
import type { Booking, BookingStatus, CancelBookingBody, Page, PageParams, PaymentAttempt, PaymentAttemptList, RequestBookingBody, RequestPaymentBody } from "../types";

// 예약 훅 넷과 결제 훅 둘. BOOK-01부터 04, PAY-01과 02(인계 문서 68행부터 73행).
// 멱등키는 화면이 useRef에 들고 mutate 변수로 넘긴다. 같은 동작의 재시도는 같은 키, 새 동작은 새 키(계약 6절)

export interface Keyed<B> {
  idempotencyKey: string;
  body: B;
}

function invalidateBooking(qc: ReturnType<typeof useQueryClient>, bookingId: string) {
  void qc.invalidateQueries({ queryKey: keys.booking(bookingId) });
  void qc.invalidateQueries({ queryKey: keys.bookingsAll() });
  void qc.invalidateQueries({ queryKey: keys.paymentAttempts(bookingId) });
}

// BOOK-01. 201 HELD. 여섯 필드 전부 필수. 성공이나 replayed 뒤 화면이 BOOK-03을 읽는다
export function useRequestBooking() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ idempotencyKey, body }: Keyed<RequestBookingBody>) => api<Booking>("/bookings", { method: "POST", body, idempotencyKey }),
    onSuccess: (result) => {
      qc.setQueryData(keys.booking(result.data.id), result.data);
      void qc.invalidateQueries({ queryKey: keys.bookingsAll() });
    },
  });
}

// BOOK-02. status는 네 값 밖을 만들지 않는다
export function useBookings(params: { status?: BookingStatus } & PageParams = {}) {
  return useQuery({
    queryKey: keys.bookings(params),
    queryFn: async () => (await api<Page<Booking>>("/bookings", { query: { ...params } })).data,
  });
}

// BOOK-03. staleTime 0. refetch를 Countdown.onZero와 쓰기의 onSettled와 새로 고침에 잇는다
export function useBooking(bookingId: string | undefined) {
  return useQuery({
    queryKey: keys.booking(bookingId ?? ""),
    queryFn: async () => (await api<Booking>(`/bookings/${bookingId}`)).data,
    enabled: !!bookingId,
    staleTime: 0,
  });
}

// BOOK-04. 200 Booking. reason은 300자 이하. 시트가 열릴 때 키 하나
export function useCancelBooking(bookingId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ idempotencyKey, body }: Keyed<CancelBookingBody>) => api<Booking>(`/bookings/${bookingId}/cancellations`, { method: "POST", body, idempotencyKey }),
    onSettled: () => invalidateBooking(qc, bookingId),
  });
}

// PAY-01. 202와 REQUESTED 하나. 결과는 BOOK-03으로. 클릭마다 새 키, mockMode는 쿠키 값(APPROVE면 생략 가능)
export function useRequestPayment(bookingId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ idempotencyKey, body }: Keyed<RequestPaymentBody>) => api<PaymentAttempt>(`/bookings/${bookingId}/payment-attempts`, { method: "POST", body, idempotencyKey }),
    onSettled: () => invalidateBooking(qc, bookingId),
  });
}

// PAY-02. 훅만. 화면은 BOOK-03의 payment.attempts로 충분하다
export function usePaymentAttempts(bookingId: string | undefined) {
  return useQuery({
    queryKey: keys.paymentAttempts(bookingId ?? ""),
    queryFn: async () => (await api<PaymentAttemptList>(`/bookings/${bookingId}/payment-attempts`)).data,
    enabled: !!bookingId,
    staleTime: 0,
  });
}
