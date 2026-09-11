package com.o2o.booking.domain;

import java.time.LocalDate;
import java.util.List;

/**
 * A6 위반. 설계 근거: 06-2 3-2 A6(숙박 기간 모든 날짜에 요금 존재. 예약 앱 서비스 선행조건).
 * 11 BOOK-01의 409 RATE_NOT_CONFIGURED. 재고 미개설과 갈라 알린다(T07의 오류 코드 구분).
 * 가격 포트 어댑터가 요금이 빠진 날짜를 실어 던진다.
 */
public class RateNotConfiguredException extends RuntimeException {

    private final List<LocalDate> missingDates;

    public RateNotConfiguredException(List<LocalDate> missingDates) {
        super("요금이 없는 날짜가 있다: " + missingDates);
        this.missingDates = List.copyOf(missingDates);
    }

    public List<LocalDate> missingDates() {
        return missingDates;
    }
}
