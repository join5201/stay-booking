package com.o2o.booking.domain;

import java.time.LocalDate;

/**
 * 취소 날짜 조건 위반. 설계 근거: 11 BOOK-04 에러 표의 409 CANCELLATION_NOT_ALLOWED, 정책 P05
 * (체크인 날짜 전까지 전체 취소. 체크인 당일부터 불가), 2차 계약 6절 P05 행. 판정은 서울
 * 오늘이 checkIn보다 앞인가다(11 공통 35행의 서울 날짜).
 */
public class CancellationNotAllowedException extends RuntimeException {

    public CancellationNotAllowedException(BookingId bookingId, LocalDate checkIn, LocalDate today) {
        super("체크인 당일부터는 취소할 수 없다: " + bookingId.value() + " checkIn=" + checkIn
                + " today=" + today);
    }
}
