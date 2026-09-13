package com.o2o.payment.domain;

/**
 * 전이 폐쇄 위반. 설계 근거: 06-4 1-3 결제 시도 표의 금지 전이(모든 역행), 11 INTERNAL-01 규칙 4
 * (이미 FAILED인데 APPROVED가 오거나 그 반대면 MOCK_EVENT_CONFLICT).
 *
 * 같은 거래의 같은 결과는 이 예외가 아니라 무해 반환(false)이다. 반대 결과만 여기다.
 * 계약 7절 D-4가 이 응답을 409로 확정했다. 검증 항목은 Y5와 Y17이다.
 */
public class InvalidAttemptTransitionException extends RuntimeException {

    public InvalidAttemptTransitionException(PaymentAttemptId attemptId, PaymentAttemptStatus from,
                                             PaymentAttemptStatus to) {
        super("허용되지 않은 전이다: " + attemptId.value() + " " + from + "에서 " + to);
    }
}
