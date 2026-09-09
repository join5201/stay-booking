package com.o2o.catalog.infrastructure;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.o2o.catalog.domain.Property;
import com.o2o.catalog.domain.PropertyRepository;
import com.o2o.shared.HostId;
import com.o2o.shared.PageQuery;
import com.o2o.shared.PageResult;
import com.o2o.shared.PropertyId;

/**
 * domain이 선언한 PropertyRepository의 구현. 설계 근거: 06-2 6절 CRC 협력자, 06-4 1-4.
 *
 * 이 어댑터가 하는 일은 값 객체와 문자열 식별자를 오가는 번역뿐이다. 조회 조건이나 규칙을
 * 여기 넣지 않는다. 규칙은 도메인이 지킨다(06-4 1-4).
 */
@Repository
public class JpaPropertyRepository implements PropertyRepository {

    private final PropertyJpaRepository jpaRepository;

    public JpaPropertyRepository(PropertyJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Property save(Property property) {
        return jpaRepository.save(property);
    }

    @Override
    public Optional<Property> findById(PropertyId propertyId) {
        return jpaRepository.findById(propertyId.value());
    }

    @Override
    public boolean existsById(PropertyId propertyId) {
        return jpaRepository.existsById(propertyId.value());
    }

    /**
     * CAT-04. 지역 코드가 null이면 전체다. 11 필드표가 그 값을 선택으로 적는다.
     * 널 분기를 JPQL 한 줄에 넣지 않고 메서드를 갈랐다. 조건이 하나 늘 때마다 쿼리 하나가
     * 복잡해지는 것보다 호출 지점에서 갈라 두는 편이 읽힌다.
     */
    @Override
    public PageResult<Property> findAll(String regionCode, PageQuery pageQuery) {
        var pageable = SpringPage.toPageable(pageQuery);
        return SpringPage.toResult(regionCode == null
                ? jpaRepository.findAll(pageable)
                : jpaRepository.findAllByRegionCode(regionCode, pageable));
    }

    /** CAT-05. 행위자의 hostId로 범위를 제한한다. 11 CAT-05 처리 규칙 */
    @Override
    public PageResult<Property> findByHostId(HostId hostId, PageQuery pageQuery) {
        return SpringPage.toResult(
                jpaRepository.findAllByHostId(hostId.value(), SpringPage.toPageable(pageQuery)));
    }
}
