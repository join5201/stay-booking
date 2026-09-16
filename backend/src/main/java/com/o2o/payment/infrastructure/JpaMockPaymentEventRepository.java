package com.o2o.payment.infrastructure;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.o2o.payment.domain.MockPaymentEvent;
import com.o2o.payment.domain.MockPaymentEventRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * domain이 선언한 MockPaymentEventRepository의 구현. 설계 근거: 11 INTERNAL-01 규칙 2, 06-4 1-4.
 * JpaPaymentRepository와 같은 구조다(layers.md 3-1).
 *
 * 저장은 persist로 한다(S9-R1-A-01). 배정 기본키에 @Version이 없어 Spring Data의 save는 merge라
 * 그 키의 행을 SELECT한 뒤 있으면 UPDATE한다. 서로 다른 Payment의 시도가 같은 eventId를 같은
 * 순간 넣으면 각자 자기 루트만 잠가 줄을 서지 못하고, 먼저 커밋한 쪽의 기록을 뒤엣것이 덮어
 * 둘 다 PROCESSED가 되어 규칙 2(같은 eventId는 유일, 다른 body면 충돌)가 깨진다. persist는 순수
 * INSERT라 덮어쓰지 않고 같은 기본키를 flush에서 막는다. flush로 그 충돌을 저장 시점에 드러내
 * 앱 서비스가 규칙 2의 MOCK_EVENT_CONFLICT로 답하게 한다.
 */
@Repository
public class JpaMockPaymentEventRepository implements MockPaymentEventRepository {

    private final MockPaymentEventJpaRepository jpaRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public JpaMockPaymentEventRepository(MockPaymentEventJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public MockPaymentEvent save(MockPaymentEvent event) {
        entityManager.persist(event);
        entityManager.flush();
        return event;
    }

    @Override
    public Optional<MockPaymentEvent> findByEventId(String eventId) {
        return jpaRepository.findById(eventId);
    }
}
