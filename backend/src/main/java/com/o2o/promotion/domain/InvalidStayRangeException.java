package com.o2o.promotion.domain;

import java.time.LocalDate;

/**
 * 조회 숙박 구간 위반. 설계 근거: 11 PROMO-05와 SEARCH-01부터 03의 Query 표 checkIn과 checkOut
 * 행. checkIn은 서버의 오늘 이상, checkOut은 checkIn보다 뒤이고 최대 30박이다.
 *
 * 재고 묶음의 InvalidStayPeriodException과 이름을 가른 이유는 값이 다르기 때문이다. 그쪽은
 * 관리 조회의 366일 상한이고 이쪽은 숙박 구간의 30박 상한이다. 계약 6절 숙박 기간 상한 행.
 * 셋 다 11 에러 응답 표의 400 INVALID_DATE_RANGE로 나간다.
 */
public class InvalidStayRangeException extends RuntimeException {

    public InvalidStayRangeException(String reason, LocalDate checkIn, LocalDate checkOut) {
        super(reason + ". checkIn " + checkIn + ", checkOut " + checkOut);
    }
}
