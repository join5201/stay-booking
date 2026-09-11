package com.o2o.payment.domain;

import java.util.Optional;

/**
 * 설계 근거: 11 INTERNAL-01 규칙 2(eventId를 유일하게 저장한다). 기록 하나가 루트 하나다.
 * 유일성은 기본키가 맡는다(06-4 1-4 유일성은 DB).
 */
public interface MockPaymentEventRepository {

    MockPaymentEvent save(MockPaymentEvent event);

    Optional<MockPaymentEvent> findByEventId(String eventId);
}
