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
import com.o2o.booking.MutableClock;
import com.o2o.booking.domain.Booking;
import com.o2o.booking.domain.BookingStatus;
import com.o2o.booking.domain.ExpirationReason;
import com.o2o.inventory.domain.InventoryReleased;
import com.o2o.payment.application.MockEventCommand;
import com.o2o.payment.application.PaymentApplicationService;
import com.o2o.payment.application.PaymentAttemptView;
import com.o2o.payment.application.RefundView;
import com.o2o.payment.domain.MockMode;
import com.o2o.payment.domain.MockOutcome;
import com.o2o.payment.domain.PaymentApproved;
import com.o2o.payment.domain.PaymentAttempt;
import com.o2o.payment.domain.PaymentAttemptId;
import com.o2o.payment.domain.PaymentFailed;
import com.o2o.payment.domain.PaymentId;
import com.o2o.payment.domain.RefundReason;

import static com.o2o.booking.BookingFixtures.CHECK_IN;
import static com.o2o.booking.BookingFixtures.GUEST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * L7과 L8. 설계 근거: task-S9-booking-lifecycle 8-1절, 2절 P1 표와 P3 문단, 06-4 2-2 정책 카드
 * (결제 승인 시 예약 확정, 승인 지연 시 자동 환불, 결제 실패 시 만료), 11 상태 전이와 시간 경계
 * (정확히 expiresAt이면 만료), 11 INTERNAL-01 규칙 5와 6, R4, T16부터 T18과 T20, 08-3 결정 5.
 *
 * 결제 앱 서비스를 직접 불러 실제 길로 이벤트를 낸다. 결제가 커밋하면 운영 구독자 어댑터가
 * P1과 P3를 부른다. 예약 중계(PAY-01)를 거치지 않는 이유는 그 중계가 만료 시각 지난 HELD를
 * 선만료해 버려 경계의 P1을 볼 수 없어서다. 시각을 정해 승인을 넣을 때는 DEFER 시도를 열고
 * INTERNAL-01 처리 길(handleMockEvent)로 결과를 넣는다. 종착 상태의 무시는 이벤트를 손으로
 * 만들어 서비스를 직접 부른다. 같은 거래의 재전달은 결제가 막아 실제 길로는 두 번 오지 않는다.
 */
@SpringBootTest
@Import(BookingLifecycleTestConfiguration.class)
class PaymentOutcomeServiceTest {

    @Autowired
    private PaymentOutcomeService outcomeService;

    @Autowired
    private BookingExpirationService expirationService;

    @Autowired
    private BookingApplicationService bookingApplicationService;

    @Autowired
    private PaymentApplicationService paymentService;

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
    }

    @Test
    void L7_HELD이고_만료_전_승인이면_CONFIRMED이고_날짜마다_held_1_감소_sold_1_증가와_BookingConfirmed_1건이다() {
        Booking booking = fixtures.held(2);

        PaymentAttemptView view = paymentService.openAttempt(booking.id().value(),
                fixtures.charge(booking), MockMode.APPROVE);

        Booking after = fixtures.reload(booking.id());
        assertEquals(BookingStatus.CONFIRMED, after.status());
        assertEquals(clock.instant(), after.confirmedAt());
        assertEquals(1L, after.version());
        for (int i = 0; i < 2; i++) {
            assertEquals(0, fixtures.heldCount(booking.roomTypeId(), CHECK_IN.plusDays(i)), "held " + i);
            assertEquals(1, fixtures.soldCount(booking.roomTypeId(), CHECK_IN.plusDays(i)), "sold " + i);
        }
        assertEquals(1, events.confirmedOf(booking.id()));
        assertEquals(2, events.committedOf(booking.roomTypeId()).size());
        assertEquals(0, events.expiredOf(booking.id()));
        assertEquals(view.id(), paymentService.attemptsOf(booking.id().value()).approvedAttemptId());
        assertNull(paymentService.attemptsOf(booking.id().value()).refund());
    }

    @Test
    void L7_승인_시각이_정확히_expiresAt이면_TTL_EXPIRED로_만료되고_held가_줄고_환불_1건이다() {
        // T18. 경계의 만료 쪽. 잠금 순서대로 환불(Payment)이 재고 반환보다 앞이다
        Booking booking = fixtures.held(1);
        PaymentAttemptView deferred = paymentService.openAttempt(booking.id().value(),
                fixtures.charge(booking), MockMode.DEFER);
        clock.set(booking.expiresAt());

        paymentService.handleMockEvent(승인(deferred));

        Booking after = fixtures.reload(booking.id());
        assertEquals(BookingStatus.EXPIRED, after.status());
        assertEquals(ExpirationReason.TTL_EXPIRED, after.expirationReason());
        assertEquals(booking.expiresAt(), after.expiredAt());
        assertEquals(1L, after.version());
        assertEquals(0, fixtures.heldCount(booking.roomTypeId(), CHECK_IN));
        assertEquals(0, fixtures.soldCount(booking.roomTypeId(), CHECK_IN));
        RefundView refund = paymentService.attemptsOf(booking.id().value()).refund();
        assertNotNull(refund);
        assertEquals(deferred.id(), refund.paymentAttemptId());
        assertEquals(RefundReason.LATE_APPROVAL, refund.reason());
        assertEquals(1, events.expiredOf(booking.id()));
        assertEquals(1, events.releasedOf(booking.roomTypeId(), InventoryReleased.Source.HELD).size());
        assertEquals(0, events.confirmedOf(booking.id()));
    }

    @Test
    void L7_승인_시각이_expiresAt_1초_전이면_확정이다() {
        // 위 테스트의 짝(T2). 같은 길로 1초 앞은 확정이다
        Booking booking = fixtures.held(1);
        PaymentAttemptView deferred = paymentService.openAttempt(booking.id().value(),
                fixtures.charge(booking), MockMode.DEFER);
        clock.set(booking.expiresAt().minusSeconds(1));

        paymentService.handleMockEvent(승인(deferred));

        Booking after = fixtures.reload(booking.id());
        assertEquals(BookingStatus.CONFIRMED, after.status());
        assertEquals(1, fixtures.soldCount(booking.roomTypeId(), CHECK_IN));
        assertNull(paymentService.attemptsOf(booking.id().value()).refund());
    }

    @Test
    void L7_이미_EXPIRED인_예약의_승인은_환불_1건이고_재고_변화가_없다() {
        // T20. 시계가 먼저 방을 풀었고 승인이 늦게 왔다
        Booking booking = fixtures.held(1);
        PaymentAttemptView deferred = paymentService.openAttempt(booking.id().value(),
                fixtures.charge(booking), MockMode.DEFER);
        clock.advance(Duration.ofMinutes(11));
        assertEquals(BookingExpirationService.Outcome.EXPIRED, expirationService.expireIfDue(booking.id()));
        assertEquals(0, fixtures.heldCount(booking.roomTypeId(), CHECK_IN));
        events.clear();

        paymentService.handleMockEvent(승인(deferred));

        Booking after = fixtures.reload(booking.id());
        assertEquals(BookingStatus.EXPIRED, after.status());
        assertEquals(1L, after.version());
        assertEquals(0, fixtures.heldCount(booking.roomTypeId(), CHECK_IN));
        assertEquals(0, fixtures.soldCount(booking.roomTypeId(), CHECK_IN));
        RefundView refund = paymentService.attemptsOf(booking.id().value()).refund();
        assertNotNull(refund);
        assertEquals(RefundReason.LATE_APPROVAL, refund.reason());
        assertEquals(0, events.expiredOf(booking.id()));
        assertEquals(0, events.inventoryReleased.size());
    }

    @Test
    void L7_CONFIRMED와_CANCELED에_온_승인은_로그_후_무시이고_아무것도_바뀌지_않는다() {
        // 08-3 결정 5. 정책 경로의 종착 상태는 오류가 아니다. 결제가 재전달을 막으므로 실제 길로는
        // 오지 않는 이벤트를 손으로 만들어 넣는다
        Booking booking = fixtures.held(1);
        paymentService.openAttempt(booking.id().value(), fixtures.charge(booking), MockMode.APPROVE);
        Booking confirmed = fixtures.reload(booking.id());
        assertEquals(BookingStatus.CONFIRMED, confirmed.status());
        events.clear();

        outcomeService.onApproved(가짜_승인(booking));

        Booking still = fixtures.reload(booking.id());
        assertEquals(BookingStatus.CONFIRMED, still.status());
        assertEquals(confirmed.version(), still.version());
        assertEquals(1, fixtures.soldCount(booking.roomTypeId(), CHECK_IN));
        assertEquals(0, events.confirmed.size() + events.expired.size() + events.canceled.size());

        bookingApplicationService.cancelBooking(new CancelBookingCommand(GUEST, booking.id(), "사정"));
        Booking canceled = fixtures.reload(booking.id());
        assertEquals(BookingStatus.CANCELED, canceled.status());
        events.clear();

        outcomeService.onApproved(가짜_승인(booking));

        assertEquals(canceled.version(), fixtures.reload(booking.id()).version());
        assertEquals(0, fixtures.soldCount(booking.roomTypeId(), CHECK_IN));
        assertEquals(0, events.confirmed.size() + events.expired.size() + events.canceled.size());
    }

    @Test
    void L7_없는_예약의_승인은_불변_위반_예외다() {
        // 2절 P1 표의 예약 없음 행. 어댑터가 잡아 로그한다
        Booking booking = fixtures.held(1);
        PaymentApproved unknown = new PaymentApproved(PaymentId.newId(), "booking_none",
                PaymentAttemptId.newId(), "mock_tx_none", fixtures.charge(booking), 1, clock.instant());

        assertThrows(IllegalStateException.class, () -> outcomeService.onApproved(unknown));
    }

    @Test
    void L8_실패_한_번과_두_번은_HELD와_held가_그대로다() {
        // T16
        Booking booking = fixtures.held(1);

        paymentService.openAttempt(booking.id().value(), fixtures.charge(booking), MockMode.DECLINE);
        assertEquals(BookingStatus.HELD, fixtures.reload(booking.id()).status());
        paymentService.openAttempt(booking.id().value(), fixtures.charge(booking), MockMode.DECLINE);

        Booking after = fixtures.reload(booking.id());
        assertEquals(BookingStatus.HELD, after.status());
        assertEquals(0L, after.version());
        assertEquals(1, fixtures.heldCount(booking.roomTypeId(), CHECK_IN));
        assertEquals(2, paymentService.attemptsOf(booking.id().value()).attemptCount());
        assertEquals(0, events.expiredOf(booking.id()));
    }

    @Test
    void L8_셋째_실패가_만료_전이면_PAYMENT_FAILED로_만료되고_held_감소가_한_번이다() {
        // T17
        Booking booking = fixtures.held(2);
        paymentService.openAttempt(booking.id().value(), fixtures.charge(booking), MockMode.DECLINE);
        paymentService.openAttempt(booking.id().value(), fixtures.charge(booking), MockMode.DECLINE);

        paymentService.openAttempt(booking.id().value(), fixtures.charge(booking), MockMode.DECLINE);

        Booking after = fixtures.reload(booking.id());
        assertEquals(BookingStatus.EXPIRED, after.status());
        assertEquals(ExpirationReason.PAYMENT_FAILED, after.expirationReason());
        assertEquals(clock.instant(), after.expiredAt());
        assertEquals(1L, after.version());
        for (int i = 0; i < 2; i++) {
            assertEquals(0, fixtures.heldCount(booking.roomTypeId(), CHECK_IN.plusDays(i)), "held " + i);
            assertEquals(2, fixtures.inventory(booking.roomTypeId(), CHECK_IN.plusDays(i)).availableCount());
        }
        assertEquals(1, events.expiredOf(booking.id()));
        assertEquals(ExpirationReason.PAYMENT_FAILED, events.expired.get(0).reason());
        assertEquals(2, events.releasedOf(booking.roomTypeId(), InventoryReleased.Source.HELD).size());
    }

    @Test
    void L8_셋째_실패가_만료_시각_이상이면_TTL_EXPIRED가_우선한다() {
        // 11 시간 경계. 아직 HELD면 TTL_EXPIRED가 이긴다. 정확히 expiresAt에 셋째 실패가 온다
        Booking booking = fixtures.held(1);
        paymentService.openAttempt(booking.id().value(), fixtures.charge(booking), MockMode.DECLINE);
        paymentService.openAttempt(booking.id().value(), fixtures.charge(booking), MockMode.DECLINE);
        clock.set(booking.expiresAt());

        paymentService.openAttempt(booking.id().value(), fixtures.charge(booking), MockMode.DECLINE);

        Booking after = fixtures.reload(booking.id());
        assertEquals(BookingStatus.EXPIRED, after.status());
        assertEquals(ExpirationReason.TTL_EXPIRED, after.expirationReason());
        assertEquals(0, fixtures.heldCount(booking.roomTypeId(), CHECK_IN));
        assertEquals(1, events.expiredOf(booking.id()));
    }

    @Test
    void L8_이미_EXPIRED거나_CONFIRMED면_실패_처리가_아무것도_바꾸지_않는다() {
        Booking expired = fixtures.held(1);
        clock.advance(Duration.ofMinutes(10));
        assertEquals(BookingExpirationService.Outcome.EXPIRED, expirationService.expireIfDue(expired.id()));
        Booking expiredBefore = fixtures.reload(expired.id());
        events.clear();

        outcomeService.onFailed(가짜_실패(expired, 3));

        Booking expiredAfter = fixtures.reload(expired.id());
        assertEquals(ExpirationReason.TTL_EXPIRED, expiredAfter.expirationReason());
        assertEquals(expiredBefore.version(), expiredAfter.version());
        assertEquals(0, fixtures.heldCount(expired.roomTypeId(), CHECK_IN));
        assertEquals(0, events.expiredOf(expired.id()));

        clock.reset();
        Booking confirmed = fixtures.held(1);
        paymentService.openAttempt(confirmed.id().value(), fixtures.charge(confirmed), MockMode.APPROVE);
        assertEquals(BookingStatus.CONFIRMED, fixtures.reload(confirmed.id()).status());

        outcomeService.onFailed(가짜_실패(confirmed, 3));

        assertEquals(BookingStatus.CONFIRMED, fixtures.reload(confirmed.id()).status());
        assertEquals(1, fixtures.soldCount(confirmed.roomTypeId(), CHECK_IN));
    }

    /** INTERNAL-01 승인. DEFER 시도의 거래 번호와 금액을 그대로 싣는다 */
    private static MockEventCommand 승인(PaymentAttemptView attempt) {
        return new MockEventCommand("evt_" + UUID.randomUUID(), attempt.id(), attempt.pgTransactionId(),
                MockOutcome.APPROVED, attempt.amount(), attempt.currency(), null);
    }

    private PaymentApproved 가짜_승인(Booking booking) {
        return new PaymentApproved(PaymentId.newId(), booking.id().value(), PaymentAttemptId.newId(),
                "mock_tx_fake", fixtures.charge(booking), 1, clock.instant());
    }

    private PaymentFailed 가짜_실패(Booking booking, int attemptCount) {
        return new PaymentFailed(PaymentId.newId(), booking.id().value(), PaymentAttemptId.newId(),
                "mock_tx_fake", fixtures.charge(booking), attemptCount, PaymentAttempt.MOCK_DECLINED,
                clock.instant());
    }
}
