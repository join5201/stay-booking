package com.o2o.inventory.api;

/**
 * 400 INVALID_DATE_RANGE의 형식 몫. 설계 근거: 11 에러 응답 표 400 INVALID_DATE_RANGE가
 * 날짜 형식과 순서와 박수 제한 오류를 한 줄로 묶는다.
 *
 * 순서와 상한은 도메인의 InvalidStayPeriodException이 던지고 형식만 이 층이 던진다.
 * 형식은 도메인에 닿기 전에 걸러지므로 도메인이 알 필요가 없다.
 */
class InvalidDateFormatException extends RuntimeException {

    InvalidDateFormatException(String field, String value) {
        super("날짜 형식이 올바르지 않다. " + field + "=" + value);
    }
}
