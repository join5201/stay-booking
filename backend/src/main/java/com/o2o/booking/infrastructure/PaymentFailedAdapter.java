package com.o2o.booking.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import com.o2o.booking.application.PaymentOutcomeService;
import com.o2o.payment.domain.PaymentFailed;

/**
 * 결제 실패 구독자. 설계 근거: 08-3 결정 6, layers.md 3-3 E1, 06-4 v5 0-3, 2차 계약 2절 P3 문단.
 * PaymentApprovedAdapter와 같은 모양이고 이벤트만 다르다.
 *
 * 결제가 커밋한 뒤 PaymentFailed를 받아 예약 앱 서비스의 P3(onFailed)를 부른다. 시도 수가 3
 * 미만이면 앱 서비스가 아무것도 하지 않는다(T16). 여기서 실패한 만료는 다음 실패 이벤트나 T1의
 * TTL 만료가 닫는다. 그때까지 예약은 HELD로 남고 새 시도는 결제가 한도(I9)로 막는다.
 */
@Component
public class PaymentFailedAdapter {

    private static final Logger log = LoggerFactory.getLogger(PaymentFailedAdapter.class);

    private final PaymentOutcomeService outcomeService;

    public PaymentFailedAdapter(PaymentOutcomeService outcomeService) {
        this.outcomeService = outcomeService;
    }

    // phase 기본값이 AFTER_COMMIT이다. 발행이 트랜잭션 밖이면 오지 않는다(fallbackExecution 기본 false)
    @TransactionalEventListener
    public void onPaymentFailed(PaymentFailed event) {
        try {
            outcomeService.onFailed(event);
        } catch (RuntimeException e) {
            log.error("결제 실패 처리 실패. T1의 TTL 만료가 닫는다. booking={} attempt={} attemptCount={}",
                    event.bookingId(), event.paymentAttemptId().value(), event.attemptCount(), e);
        }
    }
}
