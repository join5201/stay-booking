package com.o2o.payment.domain;

import java.time.Instant;

import com.o2o.shared.Money;

/**
 * 결제 승인됨. 설계 근거: 06-4 1-2 recordApproval의 Post 열(PaymentApproved 발행, 시도 수 탑재),
 * 03 결제 이벤트 행, 06-4 v5 2-4(paymentId, bookingId, attemptId, pgTransactionId, attemptCount),
 * 계약 6절 이벤트 페이로드 행.
 *
 * 구독자는 예약 2차의 결제 승인 시 예약 확정과 승인 지연 시 자동 환불이다(06-4 2-2 P1). 이 묶음에
 * 구독자는 없다. attemptCount는 06-1 R6대로 이벤트에 실려 간다. 같은 거래의 콜백이 몇 번 와도
 * 이 이벤트는 한 번만 난다(U4, T21). 검증 항목은 Y8과 Y16이다.
 */
public record PaymentApproved(PaymentId paymentId, String bookingId, PaymentAttemptId paymentAttemptId,
                              String pgTransactionId, Money amount, int attemptCount,
                              Instant occurredAt) {

    public static PaymentApproved of(Payment payment, PaymentAttempt attempt) {
        return new PaymentApproved(payment.id(), payment.bookingId(), attempt.id(),
                attempt.pgTransactionId(), attempt.amount(), payment.attemptCount(),
                attempt.completedAt());
    }
}
