package com.o2o.payment.application;

import java.time.Instant;

import com.o2o.payment.domain.MockMode;
import com.o2o.payment.domain.PaymentAttempt;
import com.o2o.payment.domain.PaymentAttemptStatus;

/**
 * 시도 뷰. 설계 근거: 11 응답 모델 PaymentAttempt의 11개 필드 그대로다(BN1).
 *
 * status가 REQUESTED, APPROVED, FAILED 셋인 이유는 그 모델이 그렇게 적어서다. 내부 상태가
 * REFUNDED인 시도는 APPROVED로 낸다(11 결제 접수와 환불 절의 환불 후에도 시도는 APPROVED,
 * 계약 7절 D-5). 환불 사실은 RefundView가 따로 든다. 시각은 Instant이고 문자열 형식은 api가
 * ApiTime으로 만든다. 예약 2차가 PaymentSummary를 낼 때 이 뷰를 쓴다.
 */
public record PaymentAttemptView(String id, String bookingId, int attemptNumber, String status,
                                 long amount, String currency, String pgTransactionId,
                                 MockMode mockMode, Instant requestedAt, Instant completedAt,
                                 String failureCode) {

    public static PaymentAttemptView of(String bookingId, PaymentAttempt attempt) {
        String status = attempt.status() == PaymentAttemptStatus.REFUNDED
                ? PaymentAttemptStatus.APPROVED.name()
                : attempt.status().name();
        return new PaymentAttemptView(attempt.id().value(), bookingId, attempt.attemptNumber(), status,
                attempt.amount().amount(), attempt.amount().currency(), attempt.pgTransactionId(),
                attempt.mockMode(), attempt.requestedAt(), attempt.completedAt(), attempt.failureCode());
    }
}
