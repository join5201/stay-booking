package com.o2o.booking.application;

/**
 * 멱등 실행의 결과. replayed가 참이면 저장된 최초 응답이고 응답 헤더에
 * Idempotency-Replayed: true를 붙인다(11 멱등 규칙 3, 공통 헤더 71행).
 */
public record IdempotentResult(StoredResponse response, boolean replayed) {
}
