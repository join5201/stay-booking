package com.o2o.booking.domain;

import java.time.Instant;

/**
 * TTL 만료의 선행조건 위반. 06-4 1-2 expire(reason)의 Pre가 원인이 TTL이면 expiresAt 경과를
 * 추가로 검사한다고 적는다. 처리 시각이 expiresAt보다 앞이면 아직 만료가 아니다. 정확히
 * expiresAt이면 만료다(11 상태 전이와 시간 경계).
 *
 * 앱 서비스는 due 목록을 읽고 잠근 뒤 다시 확인하므로 정상 경로에서는 여기 오지 않는다.
 * 프로그래밍 오류를 잡는 가드다.
 */
public class ExpirationNotDueException extends RuntimeException {

    public ExpirationNotDueException(BookingId bookingId, Instant expiresAt, Instant now) {
        super("아직 만료 시각이 아니다: " + bookingId.value() + " expiresAt=" + expiresAt + " now=" + now);
    }
}
