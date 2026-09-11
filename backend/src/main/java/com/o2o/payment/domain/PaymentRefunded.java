package com.o2o.payment.domain;

import java.time.Instant;

import com.o2o.shared.Money;

/**
 * 결제 환불됨. 설계 근거: 06-4 1-2 refund의 Post 열, 03 결제 이벤트 행, 06-4 v5 2-4(paymentId,
 * bookingId, kind, 환불액), 계약 6절 이벤트 페이로드 행(시도 ID와 reason을 더한다).
 *
 * kind는 08-3 결정 2의 NORMAL과 ORPHAN이고 v1은 NORMAL만 낸다(계약 7절 D-4). 구독자는 없다.
 * REFUNDED 재호출에는 나지 않는다(06-4 0절 종착 재호출 무해). 검증 항목은 Y7과 Y8이다.
 */
public record PaymentRefunded(PaymentId paymentId, String bookingId, PaymentAttemptId paymentAttemptId,
                              AttemptKind kind, Money amount, RefundReason reason,
                              Instant occurredAt) {

    public static PaymentRefunded of(Payment payment, PaymentAttempt attempt) {
        return new PaymentRefunded(payment.id(), payment.bookingId(), attempt.id(), attempt.kind(),
                attempt.amount(), attempt.refundReason(), attempt.refundedAt());
    }
}
