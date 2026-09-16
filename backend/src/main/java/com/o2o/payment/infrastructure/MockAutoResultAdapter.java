package com.o2o.payment.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import com.o2o.payment.application.PaymentApplicationService;
import com.o2o.payment.domain.PaymentRequested;

/**
 * 커밋 뒤 자동 결과 어댑터. 설계 근거: 계약 7절 D-1(요청은 트랜잭션 안, 결과는 커밋 뒤), 08-3 결정 6
 * (@TransactionalEventListener가 붙은 얇은 어댑터가 try와 catch로 REQUIRES_NEW 서비스 메서드를
 * 부른다), layers.md 3-3 E1(구독자는 AFTER_COMMIT을 지킨다), 06-4 v5 0-3.
 *
 * 이 묶음의 첫 구독자다. PaymentRequested를 커밋 뒤에 받아 mockMode가 APPROVE나 DECLINE이면
 * 앱 서비스의 deliverAutoResult를 부른다. 롤백된 요청의 이벤트는 오지 않는다(Y12). DEFER는
 * INTERNAL-01을 기다리므로 아무것도 하지 않는다(Y11).
 *
 * 예외를 잡아 로그하는 것은 트랜잭션 밖의 이 어댑터다. REQUIRES_NEW 안에서 삼키면 부분 커밋이
 * 되므로 본체는 던지고 여기서 잡는다(06-4 v5 0-3). 놓친 결과는 재시작 재개 러너가 다시 줍는다.
 */
@Component
public class MockAutoResultAdapter {

    private static final Logger log = LoggerFactory.getLogger(MockAutoResultAdapter.class);

    private final PaymentApplicationService paymentService;

    public MockAutoResultAdapter(PaymentApplicationService paymentService) {
        this.paymentService = paymentService;
    }

    // phase 기본값이 AFTER_COMMIT이다. 발행이 트랜잭션 밖이면 오지 않는다(fallbackExecution 기본 false)
    @TransactionalEventListener
    public void onPaymentRequested(PaymentRequested event) {
        if (!event.mockMode().isAutomatic()) {
            return;
        }
        try {
            paymentService.deliverAutoResult(event.paymentAttemptId());
        } catch (RuntimeException e) {
            log.error("자동 결과 전달 실패. attemptId={} mockMode={}", event.paymentAttemptId().value(),
                    event.mockMode(), e);
        }
    }
}
