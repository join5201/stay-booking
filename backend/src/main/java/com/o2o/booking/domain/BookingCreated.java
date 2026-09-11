package com.o2o.booking.domain;

import java.time.Instant;
import java.time.LocalDate;

import com.o2o.shared.RoomTypeId;

/**
 * 예약이 생성됨(HELD). 설계 근거: 06-4 1-2 RequestBooking의 Post 열(커밋 후 BookingCreated),
 * 06-2 4절 생성 경계의 커밋 후 이벤트, 03 예약 이벤트 행.
 *
 * 발행은 앱 서비스가 트랜잭션 안에서 하고 커밋 뒤 전달은 구독자가 지킨다
 * (backend/.claude/rules/layers.md 3-3). 구독자는 아직 없다. 검증 항목은 계약 8-1절 K12다.
 */
public record BookingCreated(BookingId bookingId, UserId userId, RoomTypeId roomTypeId,
                             LocalDate checkIn, LocalDate checkOut, long totalAmount,
                             Instant expiresAt, Instant occurredAt) {

    public static BookingCreated of(Booking booking) {
        return new BookingCreated(booking.id(), booking.userId(), booking.roomTypeId(),
                booking.period().checkIn(), booking.period().checkOut(),
                booking.priceSnapshot().totalAmount(), booking.expiresAt(), booking.createdAt());
    }
}
