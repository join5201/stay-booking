package com.o2o.booking.api;

import com.o2o.booking.application.CancelBookingCommand;
import com.o2o.booking.domain.Booking;
import com.o2o.booking.domain.BookingId;
import com.o2o.booking.domain.UserId;

import jakarta.validation.constraints.Size;

/**
 * BOOK-04 요청 본문. 설계 근거: 11 BOOK-04 요청 표(reason 하나. 선택이고 최대 300자. 생략하면 빈
 * 문자열), 2차 계약 2절 BOOK-04 문단(body 없음은 빈 객체와 같다. 미정의 필드는 400). 미정의 필드는
 * 전역 설정(fail-on-unknown-properties)이 400으로 막는다. 길이는 여기가 먼저 보고 Booking.cancel이
 * 다시 본다(06-4 1-4).
 */
public record CancelBookingRequest(
        @Size(max = Booking.CANCELLATION_REASON_MAX_LENGTH) String reason) {

    /** body가 없을 때 */
    static CancelBookingRequest empty() {
        return new CancelBookingRequest(null);
    }

    public CancelBookingCommand toCommand(UserId userId, BookingId bookingId) {
        return new CancelBookingCommand(userId, bookingId, reason);
    }

    /**
     * 멱등 규칙 2의 body 대조 값. 생략과 빈 문자열은 같은 요청이므로 정규형은 빈 문자열로 맞춘다.
     * 형식 검증을 통과한 뒤에 만들므로 거절된 body는 지문이 생기지 않는다.
     */
    public String fingerprint() {
        return BodyFingerprint.sha256("reason=" + (reason == null ? "" : reason));
    }
}
