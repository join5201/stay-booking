package com.o2o.booking.domain;

/**
 * 멱등키 누락 또는 형식 오류. 11 에러 응답 표의 400 IDEMPOTENCY_KEY_REQUIRED. 검증 항목은 K14다.
 */
public class InvalidIdempotencyKeyException extends RuntimeException {

    public InvalidIdempotencyKeyException(String value) {
        super(value == null ? "Idempotency-Key가 없다" : "Idempotency-Key 형식이 틀리다: " + value);
    }
}
