package com.o2o.catalog.infrastructure;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.o2o.catalog.domain.Property;
import com.o2o.catalog.domain.PropertyRepository;
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
}
