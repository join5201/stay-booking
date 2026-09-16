package com.o2o.booking.domain;

import java.util.Objects;

/**
 * 이용자 식별자. 설계 근거: 05-3 이용자 식별자 행(userId. v8에서 guestId에서 개명), 06-2 1절
 * Booking 내부 요소의 UserId VO. API JSON의 guestId와 이 이름의 변환은 api 층이 한다.
 * 값은 로컬 행위자 ID다(P07). 회원 기능이 없어 예약이 가진 유일한 이용자 정보다.
 */
public record UserId(String value) {

    public UserId {
        Objects.requireNonNull(value, "UserId는 null일 수 없다");
        if (value.isBlank() || value.length() > 64) {
            throw new IllegalArgumentException("UserId는 1자 이상 64자 이하여야 한다: " + value);
        }
    }

    public static UserId of(String value) {
        return new UserId(value);
    }
}
