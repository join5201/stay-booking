package com.o2o.catalog.infrastructure;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.o2o.catalog.domain.RoomType;
import com.o2o.catalog.domain.RoomTypeRepository;
import com.o2o.shared.RoomTypeId;

/**
 * domain이 선언한 RoomTypeRepository의 구현. 설계 근거: 06-2 6절 CRC 협력자, 06-4 1-4.
 */
@Repository
public class JpaRoomTypeRepository implements RoomTypeRepository {

    private final RoomTypeJpaRepository jpaRepository;

    public JpaRoomTypeRepository(RoomTypeJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public RoomType save(RoomType roomType) {
        return jpaRepository.save(roomType);
    }

    @Override
    public Optional<RoomType> findById(RoomTypeId roomTypeId) {
        return jpaRepository.findById(roomTypeId.value());
    }
}
