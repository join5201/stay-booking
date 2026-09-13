package com.o2o.booking.domain;

/**
 * 결제 시도 한도(3회)를 다 썼다. 설계 근거: 11 PAY-01 에러 표의 409 PAYMENT_ATTEMPTS_EXHAUSTED,
 * 결제 불변식 I9(시도 최대 셋), 11 결제 접수와 환불 절의 처리 순서(시도 한도). 결제 컨텍스트의
 * AttemptLimitExceeded를 예약 앱 서비스가 이것으로 감싼다(2차 계약 6절 오류 코드 매핑 행).
 *
 * 정상 흐름에서는 세 번째 실패가 예약을 만료시켜 넷째 요청은 BOOKING_EXPIRED가 먼저다. 여기
 * 오는 것은 실패 처리(P3)가 아직 닿지 않은 좁은 창이다.
 */
public class PaymentAttemptsExhaustedException extends RuntimeException {

    public PaymentAttemptsExhaustedException(BookingId bookingId, Throwable cause) {
        super("결제 시도 한도를 다 썼다: " + bookingId.value(), cause);
    }
}
