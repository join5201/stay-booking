package com.o2o.shared;

import java.util.Objects;

/**
 * 호스트 식별자. 설계 근거: 05-3 1절 호스트 정의, 06-1 4절 공유 커널의 ID 값 타입 등, P07.
 *
 * 값을 이 코드가 만들지 않는다. 11 인증과 접근 제어가 X-Dev-Actor-Id 헤더의 행위자에서
 * 채우라고 적고, body의 hostId로 소유자를 정하지 않는다고 못박는다. 그래서 newId가 없다.
 */
public record HostId(String value) {

    public HostId {
        Objects.requireNonNull(value, "HostId는 null일 수 없다");
        if (value.isBlank() || value.length() > 64) {
            throw new IllegalArgumentException("HostId는 1자 이상 64자 이하여야 한다: " + value);
        }
    }

    public static HostId of(String value) {
        return new HostId(value);
    }
}
