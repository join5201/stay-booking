package com.o2o.catalog.infrastructure;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.o2o.catalog.domain.RoomType;
import com.o2o.catalog.domain.RoomTypeRepository;
import com.o2o.shared.PageQuery;
import com.o2o.shared.PageResult;
import com.o2o.shared.PropertyId;
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

    /** CAT-09. 숙소 하나에 속한 객실 타입 목록이다 */
    @Override
    public PageResult<RoomType> findByPropertyId(PropertyId propertyId, PageQuery pageQuery) {
        return SpringPage.toResult(jpaRepository.findAllByPropertyId(
                propertyId.value(), SpringPage.toPageable(pageQuery)));
    }

    /** SEARCH-01. 쪽 없는 숙소 읽기, id 오름차순. 2026-09-12 추가 */
    @Override
    public List<RoomType> findAllByPropertyId(PropertyId propertyId) {
        return jpaRepository.findAllByPropertyId(propertyId.value());
    }
}
