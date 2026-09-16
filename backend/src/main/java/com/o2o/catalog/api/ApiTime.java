package com.o2o.catalog.api;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * 시각 문자열 형식. 설계 근거: 11 공통 요청과 응답 규칙.
 *
 * 그 절이 시각을 UTC의 YYYY-MM-DDTHH:mm:ss.SSSZ로 못박는다. 소수점 자리가 셋으로 고정이다.
 * 직렬화 도구의 기본값에 맡기면 0밀리초일 때 자리가 사라지거나 나노초가 붙는다. 그러면
 * 명세와 다른 문자열이 나가고 API 계약 준수 축에 걸린다. 그래서 형식을 손으로 고정한다.
 */
final class ApiTime {

    private static final DateTimeFormatter FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);

    private ApiTime() {
    }

    static String format(Instant instant) {
        return FORMAT.format(instant);
    }
}
