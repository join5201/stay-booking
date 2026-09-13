package com.o2o.booking.domain;

/**
 * 진행 중인 결제 시도가 있어 새 시도를 열 수 없다. 설계 근거: 11 PAY-01 에러 표의 409
 * PAYMENT_IN_PROGRESS, 결제 불변식 I6(REQUESTED는 하나), T15. 결제 컨텍스트의 AttemptInProgress를
 * 예약 앱 서비스가 이것으로 감싼다(2차 계약 6절 오류 코드 매핑 행). 핸들러는 결제 예외를 모른다.
 */
public class PaymentInProgressException extends RuntimeException {

    public PaymentInProgressException(BookingId bookingId, Throwable cause) {
        super("진행 중인 결제 시도가 있다: " + bookingId.value(), cause);
    }
}
