package com.o2o.booking.api;

import java.util.List;

import com.o2o.payment.application.PaymentSummaryView;

/**
 * 응답 모델 PaymentAttemptList. 설계 근거: 11 응답 모델 PaymentAttemptList(2586행)의 3개 필드
 * 그대로(BN1), 11 PAY-02 처리 규칙(최대 3개라 쪽을 나누지 않는다. attemptNumber 오름차순).
 * 순서는 결제의 뷰가 이미 지킨다. 환불 여부는 여기 없고 예약 상세의 payment.refund다.
 */
public record PaymentAttemptListResponse(
        String bookingId,
        int attemptCount,
        List<PaymentAttemptResponse> items) {

    public static PaymentAttemptListResponse from(String bookingId, PaymentSummaryView summary) {
        return new PaymentAttemptListResponse(bookingId, summary.attemptCount(),
                summary.attempts().stream().map(PaymentAttemptResponse::from).toList());
    }
}
