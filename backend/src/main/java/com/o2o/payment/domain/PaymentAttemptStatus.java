package com.o2o.payment.domain;

/**
 * 결제 시도 상태. 설계 근거: 06-4 1-3 결제 시도 상태 표, 05-3 6절 PaymentStatus.
 *
 * 허용 전이는 REQUESTED에서 APPROVED 또는 FAILED로, APPROVED에서 REFUNDED로 셋뿐이다.
 * 역행은 없고 FAILED는 종착이며 재시도는 새 시도 생성이다. 환불이 별도 객체가 아니라
 * 시도의 상태인 근거는 계약 7절 D-5다. 11 응답 모델 PaymentAttempt는 REFUNDED를 모르므로
 * 응답 투영에서는 REFUNDED를 APPROVED로 낸다(11 결제 접수와 환불 절의 환불 후에도 시도는
 * APPROVED). ORPHAN 행의 REFUND_PENDING은 v1에 고아 생성 경로가 없어 두지 않는다(계약 7절 D-4).
 */
public enum PaymentAttemptStatus {
    REQUESTED,
    APPROVED,
    FAILED,
    REFUNDED;

    /** I7의 승인 이력. APPROVED 또는 그로부터 전이된 REFUNDED다(06-2 3-1 I7) */
    public boolean isApprovalHistory() {
        return this == APPROVED || this == REFUNDED;
    }
}
