package com.o2o.payment;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.transaction.event.TransactionalEventListener;

import com.o2o.payment.domain.PaymentApproved;
import com.o2o.payment.domain.PaymentFailed;
import com.o2o.payment.domain.PaymentRefunded;
import com.o2o.payment.domain.PaymentRequested;

/**
 * 커밋된 사실만 받는 테스트 전용 구독자. 예약 2차의 구독자 자리를 흉내 낸다. 운영 구독자(자동
 * 결과 어댑터)와 같은 방식(@TransactionalEventListener, 기본 phase AFTER_COMMIT)으로 걸어 같은
 * 조건에서 받는다(layers.md 3-3 E1). 테스트 클래스가 @TestConfiguration의 @Bean으로 등록한다.
 *
 * ApplicationEvents가 발행 시점에 기록해 롤백된 요청도 세는 것과 달리 여기는 도착만 센다.
 * 그래서 Y11부터 Y13과 Y15부터 Y22처럼 커밋 뒤를 보는 테스트가 쓴다.
 */
public class CommittedPaymentEvents {

    public final List<PaymentRequested> requested = new CopyOnWriteArrayList<>();
    public final List<PaymentApproved> approved = new CopyOnWriteArrayList<>();
    public final List<PaymentFailed> failed = new CopyOnWriteArrayList<>();
    public final List<PaymentRefunded> refunded = new CopyOnWriteArrayList<>();

    @TransactionalEventListener
    public void onRequested(PaymentRequested event) {
        requested.add(event);
    }

    @TransactionalEventListener
    public void onApproved(PaymentApproved event) {
        approved.add(event);
    }

    @TransactionalEventListener
    public void onFailed(PaymentFailed event) {
        failed.add(event);
    }

    @TransactionalEventListener
    public void onRefunded(PaymentRefunded event) {
        refunded.add(event);
    }

    public long requestedOf(String attemptId) {
        return requested.stream().filter(e -> e.paymentAttemptId().value().equals(attemptId)).count();
    }

    public long approvedOf(String attemptId) {
        return approved.stream().filter(e -> e.paymentAttemptId().value().equals(attemptId)).count();
    }

    public long failedOf(String attemptId) {
        return failed.stream().filter(e -> e.paymentAttemptId().value().equals(attemptId)).count();
    }

    public long refundedOf(String attemptId) {
        return refunded.stream().filter(e -> e.paymentAttemptId().value().equals(attemptId)).count();
    }
}
