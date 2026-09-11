package com.o2o.promotion.domain;

/**
 * 숙박 기간 두 날짜 중 하나만 온 경우. 설계 근거: 11 PROMO-01 필드표의 stayStartDate 행.
 * stayEndDate와 함께 설정하거나 둘 다 null이어야 한다. 계약 8-1절 V4.
 *
 * InvalidPeriodException과 가른 이유는 오류 코드가 다르기 때문이다. 순서 위반은
 * INVALID_DATE_RANGE이고 짝 누락은 필수값 누락에 가까워 INVALID_REQUEST다.
 */
public class IncompleteStayWindowException extends RuntimeException {

    public IncompleteStayWindowException() {
        super("숙박 기간은 시작일과 종료일을 함께 주거나 둘 다 비워야 한다");
    }
}
