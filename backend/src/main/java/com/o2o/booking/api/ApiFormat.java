package com.o2o.booking.api;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * 날짜와 시각의 문자열 형식. 11 공통 요청과 응답 규칙의 date(YYYY-MM-DD)와 timestamp(UTC,
 * 밀리초, Z). 카탈로그의 ApiTime과 재고의 ApiDate와 같은 규칙이다. 그 둘은 패키지 안에만
 * 보이고 컨텍스트 사이 api 층이 서로를 가리키지 않아 여기 한 번 더 있다. 세 번째 사본이라
 * layers.md 3-2의 shared 후보다. 옮기는 일은 두 컨텍스트의 api를 같이 고쳐야 해서 이 묶음
 * 밖이다(계약 3절).
 */
final class ApiFormat {

    private static final DateTimeFormatter TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);

    private ApiFormat() {
    }

    static LocalDate parseDate(String field, String value) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw new InvalidDateFormatException(field, value);
        }
    }

    static String date(LocalDate date) {
        return date.toString();
    }

    static String time(Instant instant) {
        return TIME.format(instant);
    }
}
