package com.o2o.catalog.infrastructure;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.o2o.catalog.domain.RoomType;

/**
 * 설계 근거: 06-4 1-4의 유일성과 무결성은 DB. PropertyJpaRepository와 같은 성격이다.
 */
public interface RoomTypeJpaRepository extends JpaRepository<RoomType, String> {

    @Query("select r from RoomType r where r.propertyId = :propertyId")
    Page<RoomType> findAllByPropertyId(@Param("propertyId") String propertyId, Pageable pageable);

    // SEARCH-01. 쪽 없는 숙소 읽기. 정렬을 JPQL에 박는다. 2026-09-12 추가
    @Query("select r from RoomType r where r.propertyId = :propertyId order by r.id")
    List<RoomType> findAllByPropertyId(@Param("propertyId") String propertyId);
}
