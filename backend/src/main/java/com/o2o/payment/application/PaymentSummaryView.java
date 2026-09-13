package com.o2o.payment.application;

import java.util.List;

import com.o2o.payment.domain.Payment;
import com.o2o.payment.domain.PaymentAttempt;
import com.o2o.payment.domain.PaymentAttemptStatus;

/**
 * 예약 하나의 결제 요약. 설계 근거: 11 응답 모델 PaymentSummary의 4개 필드(attemptCount,
 * approvedAttemptId, attempts, refund), 06-4 v5 2-6(조회는 결제가 제공하고 예약이 부른다).
 *
 * attempts는 attemptNumber 오름차순이고 NORMAL 시도만이다(11 PAY-02 처리 규칙). approvedAttemptId는
 * 환불 뒤에도 유지된다(같은 모델 표). Payment가 없는 예약은 0과 빈 목록과 null이다.
 */
public record PaymentSummaryView(int attemptCount, String approvedAttemptId,
                                 List<PaymentAttemptView> attempts, RefundView refund) {

    public static PaymentSummaryView empty() {
        return new PaymentSummaryView(0, null, List.of(), null);
    }

    public static PaymentSummaryView of(Payment payment) {
        List<PaymentAttemptView> attempts = payment.normalAttempts().stream()
                .map(a -> PaymentAttemptView.of(payment.bookingId(), a))
                .toList();
        PaymentAttempt approved = payment.approvedAttempt().orElse(null);
        String approvedAttemptId = approved == null ? null : approved.id().value();
        RefundView refund = approved != null && approved.status() == PaymentAttemptStatus.REFUNDED
                ? RefundView.of(approved)
                : null;
        return new PaymentSummaryView(payment.attemptCount(), approvedAttemptId, attempts, refund);
    }
}
