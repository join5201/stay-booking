package com.o2o.catalog.domain;

import java.time.Instant;

import com.o2o.shared.RoomTypeId;

/**
 * 객실 타입이 수정됨. 설계 근거: 06-4 1-2 updateRoomType의 Post 열, 03 1절, 05-2 2절.
 */
public record RoomTypeUpdated(RoomTypeId roomTypeId, long version, Instant occurredAt) {

    public static RoomTypeUpdated of(RoomType roomType) {
        return new RoomTypeUpdated(roomType.id(), roomType.version(), roomType.updatedAt());
    }
}
