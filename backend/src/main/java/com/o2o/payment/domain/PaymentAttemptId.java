package com.o2o.payment.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * 결제 시도 식별자. 설계 근거: 06-4 v5 1-1 U5(같은 attemptId의 시도는 하나), 06-4 v5 1-2
 * openAttempt Post(attemptId 발급).
 *
 * 이 값이 Mock PG 요청과 환불의 멱등키다(08-3 결정 4, 06-4 v5 1-2 refund). 11 응답 모델
 * Refund의 id도 발급하지 않고 이 값에서 유도한다(계약 6절 08-3 결정 4 행). 예약 2차가
 * 이 타입을 쓰기 시작하면 shared로 올린다(layers.md 3-2).
 */
public record PaymentAttemptId(String value) {

    private static final String PREFIX = "attempt_";

    public PaymentAttemptId {
        Objects.requireNonNull(value, "PaymentAttemptId는 null일 수 없다");
        if (value.isBlank() || value.length() > 64) {
            throw new IllegalArgumentException(
                    "PaymentAttemptId는 1자 이상 64자 이하여야 한다: " + value);
        }
    }

    public static PaymentAttemptId newId() {
        return new PaymentAttemptId(PREFIX + UUID.randomUUID().toString().replace("-", ""));
    }

    public static PaymentAttemptId of(String value) {
        return new PaymentAttemptId(value);
    }

    /**
     * 11 응답 모델 Refund의 id. refund_ 접두어 뒤에 이 식별자의 접두어를 뺀 나머지를 붙인다.
     * 발급하지 않고 유도하는 근거는 08-3 결정 4와 계약 7절 D-5다.
     */
    public String refundId() {
        String suffix = value.startsWith(PREFIX) ? value.substring(PREFIX.length()) : value;
        return "refund_" + suffix;
    }
}
