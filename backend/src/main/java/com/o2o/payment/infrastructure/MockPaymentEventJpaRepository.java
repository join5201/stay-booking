package com.o2o.payment.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import com.o2o.payment.domain.MockPaymentEvent;

/**
 * 설계 근거: 11 INTERNAL-01 규칙 2(eventId를 유일하게 저장한다). 기본키가 eventId라 findById로
 * 충분하고 질의를 더하지 않는다. 유일성은 DB가 맡는다(06-4 1-4).
 */
public interface MockPaymentEventJpaRepository extends JpaRepository<MockPaymentEvent, String> {
}
