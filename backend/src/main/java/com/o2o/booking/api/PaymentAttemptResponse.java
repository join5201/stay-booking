package com.o2o.booking.api;

import com.o2o.payment.application.PaymentAttemptView;

/**
 * 응답 모델 PaymentAttempt. 설계 근거: 11 응답 모델 PaymentAttempt(2514행)의 11개 필드 그대로.
 * 필드를 더하지 않는다(BN1). 값은 결제의 뷰에서 옮긴다(2차 계약 2절 응답 모델 문단). PAY-01의
 * body, PAY-02의 items, Booking.payment.attempts가 같은 모델이다.
 *
 * status는 뷰가 이미 REQUESTED, APPROVED, FAILED 셋으로 낸다(환불된 시도도 APPROVED. 11 PAY-02
 * 처리 규칙). 시각은 이 컨텍스트의 형식(UTC 밀리초 Z)으로 만든다.
 */
public record PaymentAttemptResponse(
        String id,
        String bookingId,
        int attemptNumber,
        String status,
        long amount,
        String currency,
        String pgTransactionId,
        String mockMode,
        String requestedAt,
        String completedAt,
        String failureCode) {

    public static PaymentAttemptResponse from(PaymentAttemptView view) {
        return new PaymentAttemptResponse(
                view.id(),
                view.bookingId(),
                view.attemptNumber(),
                view.status(),
                view.amount(),
                view.currency(),
                view.pgTransactionId(),
                view.mockMode().name(),
                ApiFormat.time(view.requestedAt()),
                view.completedAt() == null ? null : ApiFormat.time(view.completedAt()),
                view.failureCode());
    }
}
