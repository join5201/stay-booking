package com.o2o.booking.domain;

/**
 * 결제 시도를 열 수 없는 만료 예약. 설계 근거: 11 PAY-01 에러 표의 409 BOOKING_EXPIRED, 11 결제
 * 접수와 환불 절의 처리 순서(TTL 만료 여부). 이미 EXPIRED이거나, HELD인데 처리 시각이 만료 시각
 * 이상인 경우다. 후자는 먼저 만료와 선점 반환을 저장한 뒤 던진다(2차 계약 7절 D-2 가).
 */
public class BookingExpiredException extends RuntimeException {

    public BookingExpiredException(BookingId bookingId) {
        super("만료된 예약이다: " + bookingId.value());
    }
}
