package com.o2o.booking.application;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import com.o2o.booking.BookingFixtures;
import com.o2o.booking.BookingLifecycleTestConfiguration;
import com.o2o.booking.CommittedBookingEvents;
import com.o2o.booking.FailingOnceInventoryAllocationService;
import com.o2o.booking.MutableClock;
import com.o2o.booking.TestHookException;
import com.o2o.booking.domain.Booking;
import com.o2o.booking.domain.BookingNotFoundException;
import com.o2o.booking.domain.BookingStatus;
import com.o2o.booking.domain.CancellationNotAllowedException;
import com.o2o.booking.domain.InvalidStateTransitionException;
import com.o2o.inventory.domain.InventoryReleased;
import com.o2o.payment.application.PaymentApplicationService;
import com.o2o.payment.application.PaymentSummaryView;
import com.o2o.payment.application.RefundView;
import com.o2o.payment.domain.MockMode;
import com.o2o.payment.domain.RefundReason;
import com.o2o.shared.SeoulDate;

import static com.o2o.booking.BookingFixtures.CHECK_IN;
import static com.o2o.booking.BookingFixtures.GUEST;
import static com.o2o.booking.BookingFixtures.OTHER_GUEST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * L10과 L11. 설계 근거: task-S9-booking-lifecycle 8-1절, 2절 BOOK-04 표 7부터 12, 11 BOOK-04 처리
 * 규칙(취소 상태와 환불 기록과 재고 반환이 함께 저장돼야 200. 부분 결과 없음), 정책 P05(체크인
 * 전날까지. 서울 날짜), 08-3 결정 10(환불이 재고 반환보다 앞)과 11-2(취소 환불 동기), T24, T25.
 *
 * 확정된 예약은 실제 길(결제 APPROVE와 운영 구독자)로 만든다. 체크인 날짜 조건은 시계를 날짜 단위로
 * 돌려 본다. 서울 날짜 기준이라 UTC 20시는 서울 다음 날 5시다. 취소 원자성(L11)은 테스트 전용
 * 훅이 환불 뒤 재고 반환에서 한 번 예외를 내게 해서 본다.
 */
@SpringBootTest
@Import(BookingLifecycleTestConfiguration.class)
class CancelBookingTest {

    /** 서울 10월 9일 05시. checkIn 10월 10일의 전날이다 */
    private static final Instant DAY_BEFORE_CHECK_IN = Instant.parse("2026-10-08T20:00:00Z");

    @Autowired
    private BookingApplicationService bookingApplicationService;

    @Autowired
    private PaymentApplicationService paymentService;

    @Autowired
    private BookingExpirationService expirationService;

    @Autowired
    private FailingOnceInventoryAllocationService inventoryHook;

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
        inventoryHook.disarm();
    }

    @Test
    void L10_CONFIRMED이고_서울_오늘이_checkIn_전이면_CANCELED와_환불_1건과_날짜마다_sold_1_감소와_BookingCanceled_1건이다() {
        Booking booking = 확정(2);
        events.clear();
        clock.set(DAY_BEFORE_CHECK_IN);
        assertEquals(CHECK_IN.minusDays(1), SeoulDate.today(clock));

        Booking canceled = bookingApplicationService.cancelBooking(
                new CancelBookingCommand(GUEST, booking.id(), "일정 변경"));

        assertEquals(BookingStatus.CANCELED, canceled.status());
        Booking after = fixtures.reload(booking.id());
        assertEquals(BookingStatus.CANCELED, after.status());
        assertEquals("일정 변경", after.cancellationReason());
        assertEquals(DAY_BEFORE_CHECK_IN, after.canceledAt());
        assertNotNull(after.confirmedAt());
        assertEquals(2L, after.version());
        for (int i = 0; i < 2; i++) {
            assertEquals(0, fixtures.soldCount(booking.roomTypeId(), CHECK_IN.plusDays(i)), "sold " + i);
            assertEquals(0, fixtures.heldCount(booking.roomTypeId(), CHECK_IN.plusDays(i)), "held " + i);
            assertEquals(2, fixtures.inventory(booking.roomTypeId(), CHECK_IN.plusDays(i)).availableCount());
        }
        PaymentSummaryView summary = paymentService.attemptsOf(booking.id().value());
        RefundView refund = summary.refund();
        assertNotNull(refund);
        assertEquals(RefundReason.BOOKING_CANCELED, refund.reason());
        assertEquals(summary.approvedAttemptId(), refund.paymentAttemptId());
        assertEquals("APPROVED", summary.attempts().get(0).status());
        assertEquals(1, events.canceledOf(booking.id()));
        assertEquals(2, events.releasedOf(booking.roomTypeId(), InventoryReleased.Source.SOLD).size());
    }

    @Test
    void L10_사유_생략은_빈_문자열이다() {
        Booking booking = 확정(1);

        bookingApplicationService.cancelBooking(new CancelBookingCommand(GUEST, booking.id(), null));

        assertEquals("", fixtures.reload(booking.id()).cancellationReason());
    }

    @Test
    void L10_HELD와_EXPIRED와_CANCELED는_InvalidStateTransition이고_수량이_그대로다() {
        Booking held = fixtures.held(1);
        InvalidStateTransitionException fromHeld = assertThrows(InvalidStateTransitionException.class,
                () -> bookingApplicationService.cancelBooking(new CancelBookingCommand(GUEST, held.id(), "사정")));
        assertEquals(BookingStatus.HELD, fromHeld.from());
        assertEquals(BookingStatus.CANCELED, fromHeld.to());
        assertEquals(1, fixtures.heldCount(held.roomTypeId(), CHECK_IN));

        Booking expired = fixtures.held(1);
        clock.advance(Duration.ofMinutes(10));
        assertEquals(BookingExpirationService.Outcome.EXPIRED, expirationService.expireIfDue(expired.id()));
        clock.reset();
        assertThrows(InvalidStateTransitionException.class, () -> bookingApplicationService.cancelBooking(
                new CancelBookingCommand(GUEST, expired.id(), "사정")));

        Booking canceled = 확정(1);
        bookingApplicationService.cancelBooking(new CancelBookingCommand(GUEST, canceled.id(), "첫 취소"));
        // 사람 경로의 재취소는 오류다(08-3 결정 5). 정책 경로의 종착 무해와 다르다
        assertThrows(InvalidStateTransitionException.class, () -> bookingApplicationService.cancelBooking(
                new CancelBookingCommand(GUEST, canceled.id(), "둘째 취소")));
        assertEquals("첫 취소", fixtures.reload(canceled.id()).cancellationReason());
        assertEquals(0, fixtures.soldCount(canceled.roomTypeId(), CHECK_IN));
        assertEquals(1, events.canceledOf(canceled.id()));
    }

    @Test
    void L10_checkIn이_서울_오늘이면_CancellationNotAllowed이고_어제여도_같으며_전날은_통과다() {
        // P05. 경계 셋. 시계를 날짜로 돌린다. 통과 쪽은 첫 테스트와 같은 전날이다
        Booking onCheckIn = 확정(1);
        clock.set(DAY_BEFORE_CHECK_IN.plus(Duration.ofDays(1)));
        assertEquals(CHECK_IN, SeoulDate.today(clock));
        assertThrows(CancellationNotAllowedException.class, () -> bookingApplicationService.cancelBooking(
                new CancelBookingCommand(GUEST, onCheckIn.id(), "당일")));
        assertEquals(BookingStatus.CONFIRMED, fixtures.reload(onCheckIn.id()).status());
        assertEquals(1, fixtures.soldCount(onCheckIn.roomTypeId(), CHECK_IN));
        assertNull(paymentService.attemptsOf(onCheckIn.id().value()).refund());

        clock.set(DAY_BEFORE_CHECK_IN.plus(Duration.ofDays(2)));
        assertEquals(CHECK_IN.plusDays(1), SeoulDate.today(clock));
        assertThrows(CancellationNotAllowedException.class, () -> bookingApplicationService.cancelBooking(
                new CancelBookingCommand(GUEST, onCheckIn.id(), "지난 뒤")));
        assertEquals(BookingStatus.CONFIRMED, fixtures.reload(onCheckIn.id()).status());

        clock.set(DAY_BEFORE_CHECK_IN);
        assertEquals(BookingStatus.CANCELED, bookingApplicationService.cancelBooking(
                new CancelBookingCommand(GUEST, onCheckIn.id(), "전날")).status());
    }

    @Test
    void L10_남의_예약과_없는_예약은_BookingNotFound다() {
        Booking booking = 확정(1);

        assertThrows(BookingNotFoundException.class, () -> bookingApplicationService.cancelBooking(
                new CancelBookingCommand(OTHER_GUEST, booking.id(), "남의 것")));

        assertEquals(BookingStatus.CONFIRMED, fixtures.reload(booking.id()).status());
        assertEquals(1, fixtures.soldCount(booking.roomTypeId(), CHECK_IN));
    }

    @Test
    void L11_환불_뒤_재고_반환이_깨지면_CANCELED도_환불도_남지_않고_다시_취소하면_정상_완료다() {
        // 11 BOOK-04 규칙의 부분 결과 없음. 훅이 releaseSold 한 번을 예외로 만든다. 환불(Payment)은
        // 같은 트랜잭션에 있어 함께 되돌아간다. 결제의 REFUNDED가 남았다면 이 테스트가 붉어진다
        Booking booking = 확정(2);
        events.clear();
        inventoryHook.failNextReleaseSold();

        assertThrows(TestHookException.class, () -> bookingApplicationService.cancelBooking(
                new CancelBookingCommand(GUEST, booking.id(), "깨지는 취소")));

        Booking after = fixtures.reload(booking.id());
        assertEquals(BookingStatus.CONFIRMED, after.status());
        assertEquals(1L, after.version());
        assertNull(after.canceledAt());
        assertNull(paymentService.attemptsOf(booking.id().value()).refund());
        assertEquals("APPROVED", paymentService.attemptsOf(booking.id().value()).attempts().get(0).status());
        for (int i = 0; i < 2; i++) {
            assertEquals(1, fixtures.soldCount(booking.roomTypeId(), CHECK_IN.plusDays(i)), "sold " + i);
        }
        assertEquals(0, events.canceledOf(booking.id()));
        assertEquals(0, events.releasedOf(booking.roomTypeId(), InventoryReleased.Source.SOLD).size());

        Booking canceled = bookingApplicationService.cancelBooking(
                new CancelBookingCommand(GUEST, booking.id(), "다시 취소"));

        assertEquals(BookingStatus.CANCELED, canceled.status());
        assertEquals(2L, fixtures.reload(booking.id()).version());
        assertEquals(RefundReason.BOOKING_CANCELED, paymentService.attemptsOf(booking.id().value()).refund().reason());
        assertEquals(0, fixtures.soldCount(booking.roomTypeId(), CHECK_IN));
        assertEquals(1, events.canceledOf(booking.id()));
    }

    /** 실제 길로 확정한다. 결제 APPROVE의 자동 결과가 커밋된 뒤 운영 구독자가 P1을 부른다 */
    private Booking 확정(int nights) {
        Booking booking = fixtures.held(nights);
        paymentService.openAttempt(booking.id().value(), fixtures.charge(booking), MockMode.APPROVE);
        Booking confirmed = fixtures.reload(booking.id());
        assertEquals(BookingStatus.CONFIRMED, confirmed.status());
        return confirmed;
    }
}
