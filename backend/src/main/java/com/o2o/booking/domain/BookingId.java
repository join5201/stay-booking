package com.o2o.booking.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * 예약 식별자. 설계 근거: 06-2 1절 Booking 식별자 열(BookingId + 유니크 idempotencyKey).
 * 11 응답 모델 Booking의 id가 최대 64자라 같은 상한을 둔다. 결제 컨텍스트는 예약 ID를
 * 문자열로 받으므로(06-1 R6, 결제는 예약을 모른다) 이 타입은 예약 컨텍스트 안에만 있다.
 */
public record BookingId(String value) {

    private static final String PREFIX = "booking_";

    public BookingId {
        Objects.requireNonNull(value, "BookingId는 null일 수 없다");
        if (value.isBlank() || value.length() > 64) {
            throw new IllegalArgumentException("BookingId는 1자 이상 64자 이하여야 한다: " + value);
        }
    }

    public static BookingId newId() {
        return new BookingId(PREFIX + UUID.randomUUID().toString().replace("-", ""));
    }

    public static BookingId of(String value) {
        return new BookingId(value);
    }
}
