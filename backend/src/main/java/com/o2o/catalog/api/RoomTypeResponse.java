package com.o2o.catalog.api;

import com.o2o.catalog.domain.RoomType;

/**
 * 설계 근거: 11 응답 모델 RoomType. 여덟 필드가 그 표 순서 그대로다.
 */
public record RoomTypeResponse(
        String id,
        String propertyId,
        String name,
        int maxOccupancy,
        String description,
        long version,
        String createdAt,
        String updatedAt) {

    public static RoomTypeResponse from(RoomType roomType) {
        return new RoomTypeResponse(
                roomType.id().value(),
                roomType.propertyId().value(),
                roomType.name(),
                roomType.maxOccupancy(),
                roomType.description(),
                roomType.version(),
                ApiTime.format(roomType.createdAt()),
                ApiTime.format(roomType.updatedAt()));
    }
}
