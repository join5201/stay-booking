package com.o2o.inventory.api;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * 날짜 문자열을 읽고 쓴다. 설계 근거: 11 공통 요청과 응답 규칙, 11 에러 응답 표.
 *
 * 직렬화 도구에 맡기지 않는 이유가 둘이다. 첫째는 ApiTime과 같다. 명세가 date 타입을
 * YYYY-MM-DD로 못박는데 도구 기본값에 맡기면 판이 바뀔 때 문자열이 흔들린다.
 *
 * 둘째가 더 크다. 도구가 날짜를 못 읽으면 본문을 못 읽은 것이 되어 400 INVALID_REQUEST가
 * 나간다. 그런데 11 에러 응답 표는 날짜 형식 오류를 400 INVALID_DATE_RANGE로 적는다.
 * 코드가 달라진다. 그래서 문자열로 받아 여기서 읽고 실패를 날짜 오류로 던진다.
 */
final class ApiDate {

    private ApiDate() {
    }

    static LocalDate parse(String field, String value) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw new InvalidDateFormatException(field, value);
        }
    }

    /** LocalDate.toString이 ISO의 YYYY-MM-DD다. 명세의 date 형식과 같다 */
    static String format(LocalDate date) {
        return date.toString();
    }
}
