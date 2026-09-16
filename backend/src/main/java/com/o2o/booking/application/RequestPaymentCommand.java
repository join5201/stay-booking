package com.o2o.booking.application;

import java.util.Objects;

import com.o2o.booking.domain.BookingId;
import com.o2o.booking.domain.UserId;
import com.o2o.payment.domain.MockMode;

/**
 * PAY-01의 입력. 설계 근거: 11 PAY-01 요청 표(mockMode는 선택이고 생략은 APPROVE). 청구액은
 * 요청에 없다. 예약의 스냅샷 총액이 청구액이다(I4, 11 결제 접수와 환불 절). 멱등키는 여기
 * 없다. 실행기가 바깥에서 감싼다(1차 계약 D-1 가).
 */
public record RequestPaymentCommand(UserId userId, BookingId bookingId, MockMode mockMode) {

    public RequestPaymentCommand {
        Objects.requireNonNull(userId, "userId는 null일 수 없다");
        Objects.requireNonNull(bookingId, "bookingId는 null일 수 없다");
        Objects.requireNonNull(mockMode, "mockMode는 null일 수 없다");
    }
}
