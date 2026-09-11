package com.o2o.payment.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * 결제 식별자. 설계 근거: 06-2 1절 Payment 행의 식별자 열(PaymentId + 유니크(bookingId)).
 *
 * shared가 아니라 여기 있는 이유는 layers.md 3-2다. 두 번째 컨텍스트가 쓰기 시작하면
 * 올린다. 형식은 11 공통 규칙의 64자 이하 문자열이고 접두어는 계약 6절 ID 형식 행이다.
 */
public record PaymentId(String value) {

    private static final String PREFIX = "pay_";

    public PaymentId {
        Objects.requireNonNull(value, "PaymentId는 null일 수 없다");
        if (value.isBlank() || value.length() > 64) {
            throw new IllegalArgumentException("PaymentId는 1자 이상 64자 이하여야 한다: " + value);
        }
    }

    public static PaymentId newId() {
        return new PaymentId(PREFIX + UUID.randomUUID().toString().replace("-", ""));
    }

    public static PaymentId of(String value) {
        return new PaymentId(value);
    }
}
