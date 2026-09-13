package com.o2o.payment.domain;

/**
 * I6 위반. 설계 근거: 06-4 1-2 openAttempt의 위반 예외 AttemptLimitExceeded, 06-2 3-1 I6.
 *
 * 11 PAY-01의 PAYMENT_ATTEMPTS_EXHAUSTED가 이 예외의 HTTP 모양이고 그 매핑은 예약 2차 몫이다.
 * 검증 항목은 T17의 결제 몫(Y2)이다.
 */
public class AttemptLimitExceededException extends RuntimeException {

    public AttemptLimitExceededException(String bookingId, int attemptCount) {
        super("결제 시도 한도를 넘었다: " + bookingId + " 시도 " + attemptCount);
    }
}
