package com.o2o.payment.infrastructure;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.o2o.payment.domain.MockPaymentEvent;
import com.o2o.payment.domain.MockPaymentEventRepository;

/**
 * domain이 선언한 MockPaymentEventRepository의 구현. 설계 근거: 11 INTERNAL-01 규칙 2, 06-4 1-4.
 * JpaPaymentRepository와 같은 구조다(layers.md 3-1).
 */
@Repository
public class JpaMockPaymentEventRepository implements MockPaymentEventRepository {

    private final MockPaymentEventJpaRepository jpaRepository;

    public JpaMockPaymentEventRepository(MockPaymentEventJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public MockPaymentEvent save(MockPaymentEvent event) {
        return jpaRepository.save(event);
    }

    @Override
    public Optional<MockPaymentEvent> findByEventId(String eventId) {
        return jpaRepository.findById(eventId);
    }
}
