package com.o2o.payment.infrastructure;

import java.util.Locale;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import com.o2o.payment.domain.PaymentApproved;
import com.o2o.payment.domain.PaymentFailed;
import com.o2o.payment.domain.PaymentRefunded;
import com.o2o.payment.domain.RefundReason;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * 결제 업무 지표. 관측 3-2(이슈 228). 커밋 뒤 이벤트로 센다(layers.md 3-3 E1).
 * 승인 수와 예약 확정 수의 차이가 돈은 받고 예약은 대기 중인 보류다.
 */
@Component
public class PaymentMetrics {

    private final MeterRegistry registry;
    private final Counter approved;
    private final Counter failed;

    public PaymentMetrics(MeterRegistry registry) {
        this.registry = registry;
        this.approved = Counter.builder("o2o.payment.approved").register(registry);
        this.failed = Counter.builder("o2o.payment.failed").register(registry);
        for (RefundReason reason : RefundReason.values()) {
            refunded(reason);
        }
    }

    @TransactionalEventListener
    public void on(PaymentApproved event) {
        approved.increment();
    }

    @TransactionalEventListener
    public void on(PaymentFailed event) {
        failed.increment();
    }

    @TransactionalEventListener
    public void on(PaymentRefunded event) {
        refunded(event.reason()).increment();
    }

    private Counter refunded(RefundReason reason) {
        return Counter.builder("o2o.payment.refunded")
                .tag("reason", reason.name().toLowerCase(Locale.ROOT))
                .register(registry);
    }
}
