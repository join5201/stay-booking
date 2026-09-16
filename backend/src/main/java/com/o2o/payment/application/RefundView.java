package com.o2o.payment.application;

import java.time.Instant;

import com.o2o.payment.domain.PaymentAttempt;
import com.o2o.payment.domain.PaymentAttemptStatus;
import com.o2o.payment.domain.RefundReason;

/**
 * 환불 뷰. 설계 근거: 11 응답 모델 Refund의 7개 필드 그대로다(BN1).
 *
 * 별도 테이블이 아니라 REFUNDED 시도에서 투영한다(계약 7절 D-5). id는 발급하지 않고 attemptId에서
 * 유도한다(08-3 결정 4와 같은 원리). status는 그 모델대로 REFUNDED 하나다.
 */
public record RefundView(String id, String paymentAttemptId, long amount, String currency,
                         String status, RefundReason reason, Instant refundedAt) {

    public static RefundView of(PaymentAttempt attempt) {
        if (attempt.status() != PaymentAttemptStatus.REFUNDED) {
            throw new IllegalArgumentException("환불된 시도가 아니다: " + attempt.id().value());
        }
        return new RefundView(attempt.id().refundId(), attempt.id().value(), attempt.amount().amount(),
                attempt.amount().currency(), PaymentAttemptStatus.REFUNDED.name(),
                attempt.refundReason(), attempt.refundedAt());
    }
}
