package com.o2o.payment.domain;

/**
 * Mock PG의 결과 방식. 설계 근거: 11 결제 접수와 환불 절(APPROVE는 승인, DECLINE은 실패 결과를
 * 자동 전달한다. DEFER는 테스트용 수동 이벤트를 기다린다), 11 응답 모델 PaymentAttempt의 mockMode.
 *
 * 값이 PaymentRequested 이벤트에 실리는 이유는 자동 결과 어댑터의 분기 입력이라서다
 * (계약 6절 이벤트 페이로드 행, 7절 D-1).
 */
public enum MockMode {
    APPROVE,
    DECLINE,
    DEFER;

    /** 커밋 뒤 자동 결과가 있는 방식. 재시작 재개(T26)의 대상도 이 둘이다 */
    public boolean isAutomatic() {
        return this != DEFER;
    }

    /** 자동 결과의 내용. DEFER는 결과가 없다 */
    public MockOutcome autoOutcome() {
        return switch (this) {
            case APPROVE -> MockOutcome.APPROVED;
            case DECLINE -> MockOutcome.FAILED;
            case DEFER -> throw new IllegalStateException("DEFER는 자동 결과가 없다");
        };
    }
}
