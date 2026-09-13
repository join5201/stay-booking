package com.o2o.booking.domain;

import java.time.Instant;

/**
 * 예약이 취소됨(CONFIRMED에서 CANCELED). 설계 근거: 06-4 1-2 cancel()의 Post 열(커밋 후 BookingCanceled
 * 발행), 06-4 v5 2-4 이벤트 페이로드 행(bookingId), 03 예약 이벤트 행.
 *
 * 발행과 전달 규칙은 BookingConfirmed와 같다.
 */
public record BookingCanceled(BookingId bookingId, Instant occurredAt) {

    public static BookingCanceled of(Booking booking) {
        return new BookingCanceled(booking.id(), booking.canceledAt());
    }
}
