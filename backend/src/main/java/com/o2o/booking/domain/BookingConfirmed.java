package com.o2o.booking.domain;

import java.time.Instant;

/**
 * 예약이 확정됨(HELD에서 CONFIRMED). 설계 근거: 06-4 1-2 confirm()의 Post 열(커밋 후 BookingConfirmed
 * 발행), 06-4 v5 2-4 이벤트 페이로드 행(bookingId. 구독자 없음), 03 예약 이벤트 행.
 *
 * 발행은 앱 서비스가 트랜잭션 안에서 하고 커밋 뒤 전달은 구독자가 지킨다(layers.md 3-3).
 * 프로덕션 구독자는 v1 밖이라 테스트 전용 구독자만 있다(계약 2-2절). 검증은 8-1절 L12다.
 */
public record BookingConfirmed(BookingId bookingId, Instant occurredAt) {

    public static BookingConfirmed of(Booking booking) {
        return new BookingConfirmed(booking.id(), booking.confirmedAt());
    }
}
