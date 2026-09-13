package com.o2o.payment.domain;

/**
 * 시도 없음. 설계 근거: 06-4 v5 1-2 recordApproval과 recordFailure의 위반 예외 UnknownAttempt,
 * 11 INTERNAL-01 규칙 1(없는 시도는 404).
 *
 * refund의 없는 시도도 이 예외다(계약 2절 refund 검사 순서). HTTP 매핑은 api 층이 한다.
 */
public class UnknownAttemptException extends RuntimeException {

    private final PaymentAttemptId attemptId;

    public UnknownAttemptException(PaymentAttemptId attemptId) {
        super("결제 시도를 찾을 수 없다: " + attemptId.value());
        this.attemptId = attemptId;
    }

    public PaymentAttemptId attemptId() {
        return attemptId;
    }
}
