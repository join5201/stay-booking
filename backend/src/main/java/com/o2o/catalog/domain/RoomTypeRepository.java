package com.o2o.catalog.domain;

import java.util.Optional;

import com.o2o.shared.RoomTypeId;

/**
 * 설계 근거: 06-2 6절 카탈로그 CRC의 CatalogApplicationService 협력자 RoomTypeRepository.
 * 06-2 1절이 RoomType을 애그리거트 루트로 적는다.
 */
public interface RoomTypeRepository {

    RoomType save(RoomType roomType);

    Optional<RoomType> findById(RoomTypeId roomTypeId);
}
