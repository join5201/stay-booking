package com.o2o.payment.domain;

/**
 * 환불 대상 없음. 설계 근거: 06-4 1-2 refund의 위반 예외 NoApprovedAttempt.
 *
 * REQUESTED나 FAILED 시도에 환불을 부르면 난다. REFUNDED는 예외가 아니라 무해 반환이다
 * (06-4 0절 종착 재호출 무해). 호출자는 예약 2차다. 검증 항목은 Y7이다.
 */
public class NoApprovedAttemptException extends RuntimeException {

    public NoApprovedAttemptException(PaymentAttemptId attemptId, PaymentAttemptStatus status) {
        super("승인된 시도가 아니라 환불할 수 없다: " + attemptId.value() + " 상태 " + status);
    }
}
