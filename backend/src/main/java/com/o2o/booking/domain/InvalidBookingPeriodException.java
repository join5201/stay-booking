package com.o2o.booking.domain;

import java.time.LocalDate;

/**
 * 숙박 기간 위반. I3(checkIn < checkOut)과 11 명세 BOOK-01 요청 표의 최대 30박과 서버의
 * 오늘 이상. 11 에러 응답 표가 날짜 순서와 박수 제한을 400 INVALID_DATE_RANGE 한 줄로 묶는다.
 */
public class InvalidBookingPeriodException extends RuntimeException {

    public InvalidBookingPeriodException(String reason, LocalDate checkIn, LocalDate checkOut) {
        super(reason + ": " + checkIn + " ~ " + checkOut);
    }
}
