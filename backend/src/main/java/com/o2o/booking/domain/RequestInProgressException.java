package com.o2o.booking.domain;

/**
 * 규칙 4. 최초 요청이 처리 중이다. 11 에러 응답 표의 409 REQUEST_IN_PROGRESS와 Retry-After: 1.
 * 새 작업을 시작하지 않는다. 검증 항목은 K17이다.
 */
public class RequestInProgressException extends RuntimeException {

    public RequestInProgressException(IdempotencyScope scope) {
        super("같은 멱등 요청이 처리 중이다: " + scope.key().value());
    }
}
