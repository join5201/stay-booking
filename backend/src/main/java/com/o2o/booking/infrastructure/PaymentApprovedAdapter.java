package com.o2o.booking.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import com.o2o.booking.application.PaymentOutcomeService;
import com.o2o.payment.domain.PaymentApproved;

/**
 * 결제 승인 구독자. 설계 근거: 08-3 결정 6(@TransactionalEventListener가 붙은 얇은 어댑터가 try와
 * catch로 REQUIRES_NEW 서비스 메서드를 부른다), layers.md 3-3 E1(구독자는 AFTER_COMMIT을 지킨다),
 * 06-4 v5 0-3, 2차 계약 2절 P1 표. 결제의 MockAutoResultAdapter와 같은 모양이다(계약 4절 본보기 행).
 *
 * 결제가 자기 트랜잭션을 커밋한 뒤 PaymentApproved를 받아 예약 앱 서비스의 P1(onApproved)을
 * 부른다. 롤백된 결제의 이벤트는 오지 않는다. 예외를 잡아 로그하는 것은 트랜잭션 밖의 이 어댑터다.
 * REQUIRES_NEW 안에서 삼키면 부분 커밋이 되므로 본체는 던지고 여기서 잡는다. 여기서 실패한
 * 승인(P1 유실)은 T1의 확정 우선이 닫는다(7절 D-1 나).
 */
@Component
public class PaymentApprovedAdapter {

    private static final Logger log = LoggerFactory.getLogger(PaymentApprovedAdapter.class);

    private final PaymentOutcomeService outcomeService;

    public PaymentApprovedAdapter(PaymentOutcomeService outcomeService) {
        this.outcomeService = outcomeService;
    }

    // phase 기본값이 AFTER_COMMIT이다. 발행이 트랜잭션 밖이면 오지 않는다(fallbackExecution 기본 false)
    @TransactionalEventListener
    public void onPaymentApproved(PaymentApproved event) {
        try {
            outcomeService.onApproved(event);
        } catch (RuntimeException e) {
            log.error("결제 승인 처리 실패. T1의 확정 우선이 닫는다. booking={} attempt={}",
                    event.bookingId(), event.paymentAttemptId().value(), e);
        }
    }
}
