package com.o2o.booking.application;

import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import com.o2o.booking.BookingFixtures;
import com.o2o.booking.BookingLifecycleTestConfiguration;
import com.o2o.booking.CommittedBookingEvents;
import com.o2o.booking.FailingOncePaymentOutcomeService;
import com.o2o.booking.MutableClock;
import com.o2o.booking.domain.Booking;
import com.o2o.booking.domain.BookingStatus;
import com.o2o.payment.application.MockEventCommand;
import com.o2o.payment.application.PaymentApplicationService;
import com.o2o.payment.application.PaymentAttemptView;
import com.o2o.payment.domain.MockEventResult;
import com.o2o.payment.domain.MockMode;
import com.o2o.payment.domain.MockOutcome;

import static com.o2o.booking.BookingFixtures.CHECK_IN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * L13. T23의 예약 몫. 설계 근거: task-S9-booking-lifecycle 8-1절, 6절 충돌 하나 행(P1이 커밋 뒤 별도
 * 트랜잭션이라 결제 승인과 예약 확정이 원자적이지 않다), 7절 D-1 나(T2 순찰 대신 T1의 확정 우선이
 * 유실을 닫는다), 08-3 결정 6과 11-1, 11 INTERNAL-01 규칙 2(같은 eventId 재전달은 DUPLICATE), 결제
 * 계약 Y21과 짝.
 *
 * 테스트 전용 훅이 P1 처리를 커밋 직전에 한 번 깨뜨린다. 그 REQUIRES_NEW 트랜잭션이 되돌아가 예약은
 * HELD, 재고는 그대로이고 결제는 앞 트랜잭션에서 커밋된 APPROVED로 남는다. 같은 이벤트를 다시
 * 넣어도 결제가 DUPLICATE로 답해 새 PaymentApproved가 없다. 시계를 만료 시각 뒤로 돌려 T1 스캔을
 * 부르면 승인 시도가 있으니 만료 대신 확정한다.
 */
@SpringBootTest
@Import(BookingLifecycleTestConfiguration.class)
class ApprovalLossRecoveryTest {

    @Autowired
    private ExpireDueBookings expireDueBookings;

    @Autowired
    private PaymentApplicationService paymentService;

    @Autowired
    private FailingOncePaymentOutcomeService outcomeHook;

    @Autowired
    private BookingFixtures fixtures;

    @Autowired
    private CommittedBookingEvents events;

    @Autowired
    private MutableClock clock;

    @BeforeEach
    void reset() {
        clock.reset();
        events.clear();
        outcomeHook.disarm();
    }

    @Test
    void L13_P1이_한_번_깨지면_HELD와_재고가_그대로이고_결제는_APPROVED이며_재전달은_DUPLICATE이고_T1_스캔이_확정_우선으로_닫는다() {
        Booking booking = fixtures.held(2);
        PaymentAttemptView deferred = paymentService.openAttempt(booking.id().value(),
                fixtures.charge(booking), MockMode.DEFER);
        MockEventCommand approval = new MockEventCommand("evt_" + UUID.randomUUID(), deferred.id(),
                deferred.pgTransactionId(), MockOutcome.APPROVED, deferred.amount(), deferred.currency(),
                null);
        outcomeHook.failNextApproval();

        MockEventResult first = paymentService.handleMockEvent(approval);

        // 결제 몫은 커밋됐고 예약 몫은 되돌아갔다. 부분 결과 롤백
        assertEquals(MockEventResult.Result.PROCESSED, first.result());
        assertEquals(deferred.id(), paymentService.attemptsOf(booking.id().value()).approvedAttemptId());
        Booking afterLoss = fixtures.reload(booking.id());
        assertEquals(BookingStatus.HELD, afterLoss.status());
        assertEquals(0L, afterLoss.version());
        assertNull(afterLoss.confirmedAt());
        for (int i = 0; i < 2; i++) {
            assertEquals(1, fixtures.heldCount(booking.roomTypeId(), CHECK_IN.plusDays(i)), "held " + i);
            assertEquals(0, fixtures.soldCount(booking.roomTypeId(), CHECK_IN.plusDays(i)), "sold " + i);
        }
        assertEquals(0, events.confirmedOf(booking.id()));
        assertEquals(0, events.inventoryCommitted.size());

        // 같은 이벤트의 재전달. 결제가 규칙 2로 막아 P1이 다시 돌지 않는다(결제 Y21과 짝)
        MockEventResult replay = paymentService.handleMockEvent(approval);

        assertEquals(MockEventResult.Result.DUPLICATE, replay.result());
        assertEquals(BookingStatus.HELD, fixtures.reload(booking.id()).status());
        assertEquals(0, events.confirmedOf(booking.id()));

        // 자가 치유. 만료 시각 뒤의 T1 스캔이 승인 시도를 보고 만료 대신 확정한다
        clock.advance(Duration.ofMinutes(10));
        ExpireDueBookings.Summary summary = expireDueBookings.runOnce();

        Booking healed = fixtures.reload(booking.id());
        assertEquals(BookingStatus.CONFIRMED, healed.status());
        assertEquals(clock.instant(), healed.confirmedAt());
        assertEquals(1L, healed.version());
        for (int i = 0; i < 2; i++) {
            assertEquals(0, fixtures.heldCount(booking.roomTypeId(), CHECK_IN.plusDays(i)), "held " + i);
            assertEquals(1, fixtures.soldCount(booking.roomTypeId(), CHECK_IN.plusDays(i)), "sold " + i);
        }
        assertEquals(1, events.confirmedOf(booking.id()));
        assertEquals(2, events.committedOf(booking.roomTypeId()).size());
        assertEquals(0, events.expiredOf(booking.id()));
        assertTrue(summary.confirmed() >= 1);
        assertNull(paymentService.attemptsOf(booking.id().value()).refund());
    }

    @Test
    void L13_훅이_없으면_같은_길이_바로_확정이다() {
        // 위 테스트의 짝. 훅을 무장하지 않은 같은 길은 P1이 정상으로 돈다
        Booking booking = fixtures.held(1);
        PaymentAttemptView deferred = paymentService.openAttempt(booking.id().value(),
                fixtures.charge(booking), MockMode.DEFER);

        paymentService.handleMockEvent(new MockEventCommand("evt_" + UUID.randomUUID(), deferred.id(),
                deferred.pgTransactionId(), MockOutcome.APPROVED, deferred.amount(), deferred.currency(),
                null));

        assertEquals(BookingStatus.CONFIRMED, fixtures.reload(booking.id()).status());
        assertEquals(1, fixtures.soldCount(booking.roomTypeId(), CHECK_IN));
        assertEquals(1, events.confirmedOf(booking.id()));
    }
}
