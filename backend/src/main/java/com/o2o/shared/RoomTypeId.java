package com.o2o.shared;

import java.util.Objects;
import java.util.UUID;

/**
 * 객실 타입 식별자. 설계 근거: 06-2 1절 RoomType 식별자 열, 06-1 4절 공유 커널.
 *
 * PropertyId보다 공유 커널 근거가 더 분명하다. 06-2 1절이 DailyInventory와 DailyRate와
 * Booking의 내부 요소에 RoomTypeId를 적어서 카탈로그 밖 컨텍스트 셋이 이 타입을 쓴다.
 */
public record RoomTypeId(String value) {

    private static final String PREFIX = "room_";

    public RoomTypeId {
        Objects.requireNonNull(value, "RoomTypeId는 null일 수 없다");
        if (value.isBlank() || value.length() > 64) {
            throw new IllegalArgumentException("RoomTypeId는 1자 이상 64자 이하여야 한다: " + value);
        }
    }

    public static RoomTypeId newId() {
        return new RoomTypeId(PREFIX + UUID.randomUUID().toString().replace("-", ""));
    }

    public static RoomTypeId of(String value) {
        return new RoomTypeId(value);
    }
}
