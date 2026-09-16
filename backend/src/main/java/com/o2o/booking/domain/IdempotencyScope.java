package com.o2o.booking.domain;

import java.util.Objects;

/**
 * 멱등 기록의 범위. 설계 근거: 11 명세 멱등 규칙 1. 행위자 ID + HTTP 메서드 + 실제 자원 경로 +
 * 키다. 서로 다른 예약의 결제 요청은 경로가 달라 범위가 다르다. 2차의 PAY-01과 BOOK-04가
 * 같은 타입으로 자기 경로를 넣는다.
 */
public record IdempotencyScope(String actorId, String method, String path, IdempotencyKey key) {

    public IdempotencyScope {
        Objects.requireNonNull(actorId, "actorId는 null일 수 없다");
        Objects.requireNonNull(method, "method는 null일 수 없다");
        Objects.requireNonNull(path, "path는 null일 수 없다");
        Objects.requireNonNull(key, "key는 null일 수 없다");
        if (actorId.isBlank() || method.isBlank() || path.isBlank()) {
            throw new IllegalArgumentException("멱등 범위의 값은 비어 있을 수 없다");
        }
    }
}
