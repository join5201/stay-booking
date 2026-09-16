package com.o2o.payment.domain;

import java.time.Instant;

import com.o2o.shared.Money;

/**
 * 결제 실패됨. 설계 근거: 06-4 1-2 recordFailure의 Post 열(PaymentFailed 발행, 시도 수 탑재),
 * 03 결제 이벤트 행, 계약 6절 이벤트 페이로드 행.
 *
 * 실패마다 낸다. 06-4 v5 1-2는 3회째에만 내지만 main의 v4와 03과 프롬프트 접점 표가 실패마다
 * attemptCount를 실어 내라 적고 계약 6절 PaymentFailed 발행 시점 행이 그렇게 확정했다. 예약
 * 2차의 결제 실패 시 만료 정책(06-4 2-2 P3)이 attemptCount 3 이상을 가드로 본다. 셋째 실패의
 * attemptCount가 3인 것이 Y8이다.
 */
public record PaymentFailed(PaymentId paymentId, String bookingId, PaymentAttemptId paymentAttemptId,
                            String pgTransactionId, Money amount, int attemptCount,
                            String failureCode, Instant occurredAt) {

    public static PaymentFailed of(Payment payment, PaymentAttempt attempt) {
        return new PaymentFailed(payment.id(), payment.bookingId(), attempt.id(),
                attempt.pgTransactionId(), attempt.amount(), payment.attemptCount(),
                attempt.failureCode(), attempt.completedAt());
    }
}
