package com.o2o.booking.api;

import com.o2o.booking.application.RequestPaymentCommand;
import com.o2o.booking.domain.BookingId;
import com.o2o.booking.domain.UserId;
import com.o2o.payment.domain.MockMode;

import jakarta.validation.constraints.Pattern;

/**
 * PAY-01 요청 본문. 설계 근거: 11 PAY-01 요청 표(mockMode 하나. 선택이고 APPROVE, DECLINE, DEFER
 * 중 하나. 생략하면 APPROVE), 2차 계약 2절 PAY-01 표 1행(body 없음은 빈 객체와 같다. 미정의 필드
 * 거절). 미정의 필드는 전역 설정(fail-on-unknown-properties)이 400으로 막는다.
 *
 * 청구액과 카드 정보는 받지 않는다. 청구액은 예약 스냅샷 총액이다(11 PAY-01 처리 규칙, I4).
 */
public record RequestPaymentRequest(
        @Pattern(regexp = "APPROVE|DECLINE|DEFER") String mockMode) {

    /** body가 없을 때. 계약 2절 PAY-01 표 1행 */
    static RequestPaymentRequest empty() {
        return new RequestPaymentRequest(null);
    }

    /** 생략은 APPROVE다(11 PAY-01 요청 표) */
    public MockMode resolvedMockMode() {
        return mockMode == null ? MockMode.APPROVE : MockMode.valueOf(mockMode);
    }

    public RequestPaymentCommand toCommand(UserId userId, BookingId bookingId) {
        return new RequestPaymentCommand(userId, bookingId, resolvedMockMode());
    }

    /**
     * 멱등 규칙 2의 body 대조 값. 생략과 APPROVE 명시는 같은 요청이므로 정규형은 해석된 값으로
     * 만든다. 형식 검증을 통과한 뒤에 만들므로 거절된 body는 지문이 생기지 않는다.
     */
    public String fingerprint() {
        return BodyFingerprint.sha256("mockMode=" + resolvedMockMode().name());
    }
}
