package com.o2o.shared;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * 날짜 문자열을 읽고 쓴다. 설계 근거: 11 공통 요청과 응답 규칙, 11 에러 응답 표.
 *
 * inventory/api의 ApiDate와 같은 규칙이다. 명세가 date 타입을 YYYY-MM-DD로 못박고, 날짜 형식
 * 오류는 도구의 400 INVALID_REQUEST가 아니라 400 INVALID_DATE_RANGE라서 문자열로 받아 여기서
 * 읽는다. 프로모션과 검색이 두 번째와 세 번째 사용처라 shared에 둔다(backend/CLAUDE.md 3-2).
 */
public final class ApiDate {

    private ApiDate() {
    }

    public static LocalDate parse(String field, String value) {
        if (value == null) {
            throw new InvalidDateFormatException(field, "null");
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw new InvalidDateFormatException(field, value);
        }
    }

    /** LocalDate.toString이 ISO의 YYYY-MM-DD다. 명세의 date 형식과 같다. null은 null이다 */
    public static String format(LocalDate date) {
        return date == null ? null : date.toString();
    }
}
