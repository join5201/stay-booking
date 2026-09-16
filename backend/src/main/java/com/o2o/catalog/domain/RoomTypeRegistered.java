package com.o2o.catalog.domain;

import java.time.Instant;

import com.o2o.shared.PropertyId;
import com.o2o.shared.RoomTypeId;

/**
 * 객실 타입이 등록됨. 설계 근거: 06-4 1-2 카탈로그 계약표 registerRoomType의 Post 열,
 * 03 1절 이벤트 목록, 05-2 2절.
 *
 * PropertyRegistered와 같은 성격이다. 구독자가 없고 값 명세도 없다.
 * 소속 숙소를 싣는 이유는 06-2 1절이 RoomType의 내부 요소에 PropertyId를 적기 때문이다.
 */
public record RoomTypeRegistered(RoomTypeId roomTypeId, PropertyId propertyId, Instant occurredAt) {

    public static RoomTypeRegistered of(RoomType roomType) {
        return new RoomTypeRegistered(roomType.id(), roomType.propertyId(), roomType.createdAt());
    }
}
