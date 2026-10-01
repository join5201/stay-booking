package com.o2o.booking.infrastructure;

import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.o2o.booking.BookingFixtures;
import com.o2o.booking.BookingLifecycleTestConfiguration;
import com.o2o.booking.FailingOncePaymentOutcomeService;
import com.o2o.booking.MutableClock;
import com.o2o.booking.application.ExpireDueBookings;
import com.o2o.booking.domain.Booking;
import com.o2o.booking.domain.BookingConfirmed;
import com.o2o.booking.domain.BookingStatus;
import com.o2o.payment.application.MockEventCommand;
import com.o2o.payment.application.PaymentApplicationService;
import com.o2o.payment.application.PaymentAttemptView;
import com.o2o.payment.domain.MockMode;
import com.o2o.payment.domain.MockOutcome;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 관측 3-2(이슈 228). 업무 지표가 커밋된 사실만 세는지.
 *
 * 첫 테스트는 ApprovalLossRecoveryTest(L13)와 같은 길이다. 장애 실험 C1의 축소판이라 지표가 이 길에서
 * 맞아야 실험에서도 맞는다. 컨텍스트와 DB를 다른 테스트와 나눠 쓰므로 절대값이 아니라 증가분을 본다.
 */
@SpringBootTest
@Import(BookingLifecycleTestConfiguration.class)
class BookingMetricsTest {

    @Autowired
    private MeterRegistry registry;

    @Autowired
    private ExpireDueBookings expireDueBookings;

    @Autowired
    private PaymentApplicationService paymentService;

    @Autowired
    private FailingOncePaymentOutcomeService outcomeHook;

    @Autowired
    private BookingFixtures fixtures;

    @Autowired
    private MutableClock clock;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void reset() {
        clock.reset();
        outcomeHook.disarm();
    }

    @Test
    void 승인_처리가_한_번_깨지면_승인은_늘고_확정은_그대로이며_처리_실패가_하나_늘고_만료_스캔이_확정을_하나_더한다() {
        Booking booking = fixtures.held(1);
        PaymentAttemptView deferred = paymentService.openAttempt(booking.id().value(),
                fixtures.charge(booking), MockMode.DEFER);
        double approved = count("o2o.payment.approved");
        double confirmed = count("o2o.booking.confirmed");
        double resultFailures = count("o2o.booking.payment.result.failures", "result", "approved");
        double expired = count("o2o.booking.expired", "reason", "ttl_expired");
        double lateRefunds = count("o2o.payment.refunded", "reason", "late_approval");

        outcomeHook.failNextApproval();
        paymentService.handleMockEvent(new MockEventCommand("evt_" + UUID.randomUUID(), deferred.id(),
                deferred.pgTransactionId(), MockOutcome.APPROVED, deferred.amount(), deferred.currency(),
                null));

        // 결제는 커밋됐고 예약 확정은 롤백됐다. 이 차이가 보류다
        assertEquals(BookingStatus.HELD, fixtures.reload(booking.id()).status());
        assertEquals(approved + 1, count("o2o.payment.approved"));
        assertEquals(confirmed, count("o2o.booking.confirmed"));
        assertEquals(resultFailures + 1, count("o2o.booking.payment.result.failures", "result", "approved"));

        // 스캔은 다른 테스트가 남긴 due 예약도 함께 집는다. 그래서 1이 아니라 스캔이 직접 센 수와 맞댄다
        clock.advance(Duration.ofMinutes(10));
        ExpireDueBookings.Summary summary = expireDueBookings.runOnce();

        assertEquals(BookingStatus.CONFIRMED, fixtures.reload(booking.id()).status());
        assertTrue(summary.confirmed() >= 1);
        assertEquals(confirmed + summary.confirmed(), count("o2o.booking.confirmed"));
        assertEquals(expired + summary.expired(), count("o2o.booking.expired", "reason", "ttl_expired"));
        assertTrue(count("o2o.payment.refunded", "reason", "late_approval") - lateRefunds <= summary.expired());
        assertNull(paymentService.attemptsOf(booking.id().value()).refund());
    }

    @Test
    void 롤백된_트랜잭션의_이벤트는_세지_않고_커밋된_것만_센다() {
        Booking booking = fixtures.held(1);
        BookingConfirmed event = new BookingConfirmed(booking.id(), clock.instant());
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        double before = count("o2o.booking.confirmed");

        tx.executeWithoutResult((status) -> {
            eventPublisher.publishEvent(event);
            status.setRollbackOnly();
        });

        assertEquals(before, count("o2o.booking.confirmed"));

        tx.executeWithoutResult((status) -> eventPublisher.publishEvent(event));

        assertEquals(before + 1, count("o2o.booking.confirmed"));
    }

    private double count(String name, String... tags) {
        Counter counter = registry.find(name).tags(tags).counter();
        assertEquals(true, counter != null, "미리 만들어 둔 지표: " + name);
        return counter.count();
    }
}
