package com.o2o.shared;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * 시각 문자열 형식. 설계 근거: 11 공통 요청과 응답 규칙의 UTC YYYY-MM-DDTHH:mm:ss.SSSZ.
 *
 * catalog/api의 ApiTime과 같은 형식이다. 소수점 자리가 셋으로 고정이라 도구 기본값에 맡기지
 * 않는다. 프로모션이 두 번째 사용처라 shared에 둔다(backend/CLAUDE.md 3-2). catalog 쪽을
 * 옮기는 것은 이번 범위 밖이다.
 */
public final class ApiTime {

    private static final DateTimeFormatter FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);

    private ApiTime() {
    }

    public static String format(Instant instant) {
        return FORMAT.format(instant);
    }
}
