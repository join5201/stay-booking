package com.o2o.booking.domain;

import java.time.Instant;

/**
 * 예약이 만료됨(HELD에서 EXPIRED). 설계 근거: 06-4 1-2 expire(reason)의 Post 열(커밋 후 BookingExpired
 * 발행), 06-4 v5 2-4 이벤트 페이로드 행(bookingId와 사유), 03 예약 이벤트 행.
 *
 * 사유는 TTL_EXPIRED 또는 PAYMENT_FAILED다. 발행과 전달 규칙은 BookingConfirmed와 같다.
 */
public record BookingExpired(BookingId bookingId, ExpirationReason reason, Instant occurredAt) {

    public static BookingExpired of(Booking booking) {
        return new BookingExpired(booking.id(), booking.expirationReason(), booking.expiredAt());
    }
}
