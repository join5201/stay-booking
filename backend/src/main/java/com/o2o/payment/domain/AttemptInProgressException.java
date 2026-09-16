package com.o2o.payment.domain;

/**
 * I9 위반. 설계 근거: 06-4 1-2 openAttempt의 위반 예외 AttemptInProgress, 06-2 3-1 I9.
 *
 * 11 PAY-01의 PAYMENT_IN_PROGRESS가 이 예외의 HTTP 모양이고 그 매핑은 예약 2차 몫이다
 * (계약 2절 openAttempt 표 아래 문단). 검증 항목은 T15와 T16의 결제 몫(Y1)이다.
 */
public class AttemptInProgressException extends RuntimeException {

    public AttemptInProgressException(String bookingId) {
        super("진행 중인 결제 시도가 있다: " + bookingId);
    }
}
