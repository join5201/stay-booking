package com.o2o.booking.infrastructure;

import java.util.Locale;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import com.o2o.booking.domain.BookingCanceled;
import com.o2o.booking.domain.BookingConfirmed;
import com.o2o.booking.domain.BookingExpired;
import com.o2o.booking.domain.ExpirationReason;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * 예약 업무 지표. 관측 3-2(이슈 228).
 *
 * 전이 셋은 커밋 뒤 이벤트로 센다(layers.md 3-3 E1). 앱 서비스 안에서 세면 롤백된 확정도 1로 남아
 * 장애 실험에서 지표가 DB와 어긋난다. 이벤트가 없는 처리 실패 둘은 예외를 잡는 어댑터가 부른다.
 *
 * 태그 값마다 카운터를 시작할 때 만든다. 한 번도 안 일어난 지표는 Prometheus에 줄이 없어 증가율
 * 경보가 걸리지 않는다.
 */
@Component
public class BookingMetrics {

    private final MeterRegistry registry;
    private final Counter confirmed;
    private final Counter canceled;
    private final Counter expireScanFailures;

    public BookingMetrics(MeterRegistry registry) {
        this.registry = registry;
        this.confirmed = Counter.builder("o2o.booking.confirmed").register(registry);
        this.canceled = Counter.builder("o2o.booking.canceled").register(registry);
        this.expireScanFailures = Counter.builder("o2o.booking.expire.scan.failures").register(registry);
        for (ExpirationReason reason : ExpirationReason.values()) {
            expired(reason);
        }
        paymentResultFailures("approved");
        paymentResultFailures("failed");
    }

    @TransactionalEventListener
    public void on(BookingConfirmed event) {
        confirmed.increment();
    }

    @TransactionalEventListener
    public void on(BookingExpired event) {
        expired(event.reason()).increment();
    }

    @TransactionalEventListener
    public void on(BookingCanceled event) {
        canceled.increment();
    }

    /** 결제 결과를 받은 예약 쪽 처리가 예외로 끝났다. result는 approved 또는 failed */
    public void paymentResultHandlingFailed(String result) {
        paymentResultFailures(result).increment();
    }

    /** 만료 스캔 한 바퀴가 예외로 끝났다 */
    public void expireScanFailed() {
        expireScanFailures.increment();
    }

    private Counter expired(ExpirationReason reason) {
        return Counter.builder("o2o.booking.expired")
                .tag("reason", reason.name().toLowerCase(Locale.ROOT))
                .register(registry);
    }

    private Counter paymentResultFailures(String result) {
        return Counter.builder("o2o.booking.payment.result.failures")
                .tag("result", result)
                .register(registry);
    }
}
