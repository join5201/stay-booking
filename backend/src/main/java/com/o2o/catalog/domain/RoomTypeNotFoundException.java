package com.o2o.catalog.domain;

import com.o2o.shared.RoomTypeId;

/**
 * 설계 근거: 06-4 1-2 카탈로그 계약표 updateRoomType의 대상 존재 선행조건. 11 CAT-08 조회의 404.
 */
public class RoomTypeNotFoundException extends RuntimeException {

    public RoomTypeNotFoundException(RoomTypeId roomTypeId) {
        super("객실 타입을 찾을 수 없다: " + roomTypeId.value());
    }
}
