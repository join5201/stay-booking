package com.o2o.payment.domain;

/**
 * Mock 거래 결과. 설계 근거: 11 INTERNAL-01 요청 표의 outcome(APPROVED / FAILED).
 *
 * 시도 상태와 다른 타입인 이유는 결과가 입력이고 상태가 그 입력이 만든 사실이기 때문이다.
 * APPROVED 결과는 recordApproval, FAILED 결과는 recordFailure로 간다(06-4 1-2 결제 표).
 */
public enum MockOutcome {
    APPROVED,
    FAILED
}
