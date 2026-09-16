package com.o2o.payment.domain;

/**
 * 환불 사유. 설계 근거: 11 응답 모델 Refund의 reason(BOOKING_CANCELED / LATE_APPROVAL).
 *
 * 어느 쪽인지는 호출자인 예약 2차가 정한다. 취소 시 환불(06-4 2-2 P5)이 BOOKING_CANCELED,
 * 승인 지연 시 자동 환불(P1의 EXPIRED 분기)이 LATE_APPROVAL이다. 결제는 값을 받기만 한다
 * (계약 2절 refund 검사 순서).
 */
public enum RefundReason {
    BOOKING_CANCELED,
    LATE_APPROVAL
}
