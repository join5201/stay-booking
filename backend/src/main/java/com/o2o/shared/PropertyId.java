package com.o2o.shared;

import java.util.Objects;
import java.util.UUID;

/**
 * 숙소 식별자. 설계 근거: 06-2 1절 Property 식별자 열, 06-1 4절 공유 커널.
 *
 * shared에 두는 근거는 06-1 4절이다. ID 값 타입을 공유 커널로 판정하고 확정은 Step 9
 * 패키지 구조에서 하라고 적는다. 지금이 그 Step 9다.
 *
 * 값 형식의 근거는 11 공통 요청과 응답 규칙이다. 최대 64자의 비어 있지 않은 문자열이고
 * 클라이언트가 내부 구조를 해석하지 않는다. 그래서 접두사 뒤를 UUID로 채운다. 명세의
 * prop_001은 예시이지 형식 규정이 아니다. prop_ 다섯 자에 하이픈 없는 UUID 32자로 37자다.
 */
public record PropertyId(String value) {

    private static final String PREFIX = "prop_";

    public PropertyId {
        Objects.requireNonNull(value, "PropertyId는 null일 수 없다");
        if (value.isBlank() || value.length() > 64) {
            throw new IllegalArgumentException("PropertyId는 1자 이상 64자 이하여야 한다: " + value);
        }
    }

    public static PropertyId newId() {
        return new PropertyId(PREFIX + UUID.randomUUID().toString().replace("-", ""));
    }

    public static PropertyId of(String value) {
        return new PropertyId(value);
    }
}
