package com.o2o.booking.api;

/**
 * 날짜 형식 오류. 11 에러 응답 표의 400 INVALID_DATE_RANGE(날짜 형식, 순서, 박수 제한 오류).
 * 형식은 이 층의 일이라(06-4 1-4) 예외도 이 층에 있다. 재고의 같은 이름 예외와 성격이 같다.
 */
class InvalidDateFormatException extends RuntimeException {

    InvalidDateFormatException(String field, String value) {
        super(field + "의 날짜 형식이 틀리다: " + value);
    }
}
