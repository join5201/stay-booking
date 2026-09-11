package com.o2o.catalog.infrastructure;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.o2o.catalog.domain.Property;

/**
 * 스프링 데이터가 구현을 만들어 주는 인터페이스. 설계 근거: 06-4 1-4의 유일성과 무결성은 DB.
 *
 * domain의 PropertyRepository와 이름이 다른 이유는 둘이 다른 것이기 때문이다. 저쪽은 도메인이
 * 선언한 항구이고 이것은 그 항구를 채우는 어댑터의 부품이다. 이 타입이 domain에 올라가면
 * domain이 스프링 데이터를 참조하게 되고 레이어 역전 축에 걸린다.
 *
 * 식별자 타입이 String인 이유는 Property가 식별자를 문자열 열로 저장하기 때문이다.
 * 그 결정의 이유는 Property 머리 주석에 있다.
 *
 * 조회 조건을 메서드 이름 규칙이 아니라 JPQL로 적는다. 이름 규칙은 임베더블 속성을 탈 때
 * 해석이 갈릴 수 있고 그 실패가 컴파일이 아니라 컨텍스트 기동에서 난다.
 */
public interface PropertyJpaRepository extends JpaRepository<Property, String> {

    @Query("select p from Property p where p.region.code = :regionCode")
    Page<Property> findAllByRegionCode(@Param("regionCode") String regionCode, Pageable pageable);

    @Query("select p from Property p where p.hostId = :hostId")
    Page<Property> findAllByHostId(@Param("hostId") String hostId, Pageable pageable);

    // SEARCH-01. 쪽 없는 지역 읽기. 정렬을 JPQL에 박는다. 2026-09-12 추가
    @Query("select p from Property p where p.region.code = :regionCode order by p.id")
    List<Property> findAllByRegionCode(@Param("regionCode") String regionCode);
}
