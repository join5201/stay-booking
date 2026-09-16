package com.o2o.payment.domain;

/**
 * 거래 번호 불일치. 설계 근거: 11 INTERNAL-01 규칙 1(다른 거래 ID는 MOCK_EVENT_CONFLICT),
 * 계약 2절 INTERNAL-01 표 5행, 06-2 3-4 U4.
 *
 * Mock에서는 거래 번호가 접수 때 정해지므로 다른 거래 번호의 결과는 있을 수 없는 콜백이다
 * (계약 7절 D-4). 그래서 고아로 기록하지 않고 거절한다. 검증 항목은 Y6과 Y17이다.
 */
public class PgTransactionMismatchException extends RuntimeException {

    public PgTransactionMismatchException(PaymentAttemptId attemptId, String own, String given) {
        super("거래 번호가 시도의 것과 다르다: " + attemptId.value() + " 시도 " + own + ", 이벤트 " + given);
    }
}
