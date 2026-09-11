package com.o2o.booking.domain;

/**
 * 규칙 2 위반. 같은 범위와 키인데 body가 다르다. 11 에러 응답 표의 409 IDEMPOTENCY_KEY_REUSED.
 * 검증 항목은 T11이다.
 */
public class IdempotencyKeyReusedException extends RuntimeException {

    public IdempotencyKeyReusedException(IdempotencyScope scope) {
        super("같은 멱등키에 다른 body다: " + scope.key().value());
    }
}
