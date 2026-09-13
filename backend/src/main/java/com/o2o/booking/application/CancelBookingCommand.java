package com.o2o.booking.application;

import java.util.Objects;

import com.o2o.booking.domain.BookingId;
import com.o2o.booking.domain.UserId;

/**
 * BOOK-04의 입력. 설계 근거: 11 BOOK-04 요청 표(reason은 선택이고 최대 300자). null은 이유 없음이고
 * Booking.cancel이 빈 문자열로 적는다(11 응답 모델 cancellationReason). 길이는 api가 먼저 보고
 * 도메인이 다시 본다. 멱등키는 실행기가 바깥에서 감싼다.
 */
public record CancelBookingCommand(UserId userId, BookingId bookingId, String reason) {

    public CancelBookingCommand {
        Objects.requireNonNull(userId, "userId는 null일 수 없다");
        Objects.requireNonNull(bookingId, "bookingId는 null일 수 없다");
    }
}
