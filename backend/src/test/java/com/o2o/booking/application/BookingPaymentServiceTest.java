package com.o2o.booking.application;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import com.o2o.booking.BookingFixtures;
import com.o2o.booking.BookingLifecycleTestConfiguration;
import com.o2o.booking.MutableClock;
import com.o2o.booking.domain.Booking;
import com.o2o.booking.domain.BookingExpiredException;
import com.o2o.booking.domain.BookingId;
import com.o2o.booking.domain.BookingNotFoundException;
import com.o2o.booking.domain.BookingStatus;
import com.o2o.booking.domain.ExpirationReason;
import com.o2o.booking.domain.InvalidStateTransitionException;
import com.o2o.booking.domain.PaymentAttemptsExhaustedException;
import com.o2o.booking.domain.PaymentInProgressException;
import com.o2o.payment.application.PaymentApplicationService;
import com.o2o.payment.application.PaymentAttemptView;
import com.o2o.payment.application.PaymentSummaryView;
import com.o2o.payment.domain.MockMode;

import static com.o2o.booking.BookingFixtures.CHECK_IN;
import static com.o2o.booking.BookingFixtures.GUEST;
import static com.o2o.booking.BookingFixtures.OTHER_GUEST;
import static com.o2o.booking.BookingFixtures.RATE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * L6. 설계 근거: task-S9-booking-lifecycle 8-1절, 11 결제 접수와 환불 절의 새 시도 처리 순서(멱등,
 * TTL 만료 여부, 예약 상태, 진행 중 시도, 시도 한도), 2절 PAY-01 표 6부터 10, 7절 D-2 가(잠금 전
 * 별도 트랜잭션의 선만료), 6절 오류 코드 매핑 행, T14와 T15.
 *
 * 앱 서비스를 직접 부른다. 멱등과 HTTP는 6단계가 본다. 운영 구독자와 자동 결과 어댑터가 살아
 * 있으므로 APPROVE 요청은 돌아올 때 이미 확정돼 있다. 응답 뷰가 REQUESTED인 것과 저장 상태가
 * CONFIRMED인 것을 같이 보면 접수와 결과의 차이(11 PAY-01 202)가 드러난다. HELD로 남겨야 하는
 * 경우는 DEFER를 쓴다.
 */
@SpringBootTest
@Import(BookingLifecycleTestConfiguration.class)
class BookingPaymentServiceTest {

    @Autowired
    private BookingPaymentService bookingPaymentService;

    @Autowired
    private BookingApplicationService bookingApplicationService;

    @Autowired
    private PaymentApplicationService paymentService;

    @Autowired
    private BookingExpirationService expirationService;

    @Autowired
    private BookingFixtures fixtures;

    @Autowired
    private MutableClock clock;

    @BeforeEach
    void resetClock() {
        clock.reset();
    }

    @Test
    void L6_유효한_HELD는_REQUESTED_뷰를_돌려주고_청구액이_스냅샷_총액이며_APPROVE는_돌아올_때_확정돼_있다() {
        Booking booking = fixtures.held(2);

        PaymentAttemptView view = bookingPaymentService.requestPayment(
                new RequestPaymentCommand(GUEST, booking.id(), MockMode.APPROVE));

        assertEquals("REQUESTED", view.status());
        assertEquals(1, view.attemptNumber());
        assertEquals(booking.id().value(), view.bookingId());
        assertEquals(RATE * 2, view.amount());
        assertEquals("KRW", view.currency());
        assertEquals(MockMode.APPROVE, view.mockMode());
        assertNull(view.completedAt());

        Booking after = fixtures.reload(booking.id());
        assertEquals(BookingStatus.CONFIRMED, after.status());
        assertEquals(1L, after.version());
        assertEquals(0, fixtures.heldCount(booking.roomTypeId(), CHECK_IN));
        assertEquals(1, fixtures.soldCount(booking.roomTypeId(), CHECK_IN));
        PaymentSummaryView summary = bookingPaymentService.paymentAttempts(GUEST, booking.id());
        assertEquals(view.id(), summary.approvedAttemptId());
        assertEquals("APPROVED", summary.attempts().get(0).status());
    }

    @Test
    void L6_DEFER는_HELD로_남고_같은_예약에_새_요청은_PaymentInProgress다() {
        // T15. 결제의 AttemptInProgress를 예약 예외로 감싼다. 원인이 cause에 남는다
        Booking booking = fixtures.held(1);
        bookingPaymentService.requestPayment(new RequestPaymentCommand(GUEST, booking.id(), MockMode.DEFER));
        assertEquals(BookingStatus.HELD, fixtures.reload(booking.id()).status());

        PaymentInProgressException e = assertThrows(PaymentInProgressException.class,
                () -> bookingPaymentService.requestPayment(
                        new RequestPaymentCommand(GUEST, booking.id(), MockMode.APPROVE)));

        assertNotNull(e.getCause());
        assertEquals(1, paymentService.attemptsOf(booking.id().value()).attemptCount());
        assertEquals(BookingStatus.HELD, fixtures.reload(booking.id()).status());
    }

    @Test
    void L6_실패_셋이_쌓인_HELD에_새_요청은_PaymentAttemptsExhausted다() {
        // 정상 흐름은 셋째 실패가 P3로 만료시켜 BOOKING_EXPIRED가 먼저다(L15). 여기는 실패 셋을 결제
        // 집합체에 직접 심어 P3가 닿지 않은 좁은 창을 만든다(T6). 결제 한도 I9를 예약 예외로 감싼다
        Booking booking = fixtures.held(1);
        fixtures.seedFailed(booking, 3);
        assertEquals(3, paymentService.attemptsOf(booking.id().value()).attemptCount());

        PaymentAttemptsExhaustedException e = assertThrows(PaymentAttemptsExhaustedException.class,
                () -> bookingPaymentService.requestPayment(
                        new RequestPaymentCommand(GUEST, booking.id(), MockMode.APPROVE)));

        assertNotNull(e.getCause());
        assertEquals(3, paymentService.attemptsOf(booking.id().value()).attemptCount());
        assertEquals(BookingStatus.HELD, fixtures.reload(booking.id()).status());
    }

    @Test
    void L6_EXPIRED에는_BookingExpired이고_시도가_열리지_않는다() {
        Booking booking = fixtures.held(1);
        clock.advance(Duration.ofMinutes(10));
        만료(booking.id());
        assertEquals(BookingStatus.EXPIRED, fixtures.reload(booking.id()).status());

        assertThrows(BookingExpiredException.class, () -> bookingPaymentService.requestPayment(
                new RequestPaymentCommand(GUEST, booking.id(), MockMode.APPROVE)));

        assertEquals(0, paymentService.attemptsOf(booking.id().value()).attemptCount());
    }

    @Test
    void L6_만료_시각_지난_HELD는_먼저_만료와_선점_반환이_저장된_뒤_BookingExpired다() {
        // 7절 D-2 가. 409를 내며 던진 예외가 자기 트랜잭션을 되돌려도 만료는 별도 트랜잭션에 남아 있다.
        // 경계는 정확히 expiresAt이다(11 시간 경계)
        Booking booking = fixtures.held(2);
        assertEquals(1, fixtures.heldCount(booking.roomTypeId(), CHECK_IN));
        clock.set(booking.expiresAt());

        assertThrows(BookingExpiredException.class, () -> bookingPaymentService.requestPayment(
                new RequestPaymentCommand(GUEST, booking.id(), MockMode.APPROVE)));

        Booking after = fixtures.reload(booking.id());
        assertEquals(BookingStatus.EXPIRED, after.status());
        assertEquals(ExpirationReason.TTL_EXPIRED, after.expirationReason());
        assertEquals(booking.expiresAt(), after.expiredAt());
        assertEquals(0, fixtures.heldCount(booking.roomTypeId(), CHECK_IN));
        assertEquals(0, fixtures.heldCount(booking.roomTypeId(), CHECK_IN.plusDays(1)));
        assertEquals(0, paymentService.attemptsOf(booking.id().value()).attemptCount());
    }

    @Test
    void L6_만료_시각_1초_전에는_시도가_열린다() {
        // 위 테스트의 짝(T2). 경계 바로 앞은 유효하다
        Booking booking = fixtures.held(1);
        clock.set(booking.expiresAt().minusSeconds(1));

        PaymentAttemptView view = bookingPaymentService.requestPayment(
                new RequestPaymentCommand(GUEST, booking.id(), MockMode.DEFER));

        assertEquals("REQUESTED", view.status());
        assertEquals(BookingStatus.HELD, fixtures.reload(booking.id()).status());
        assertEquals(1, fixtures.heldCount(booking.roomTypeId(), CHECK_IN));
    }

    @Test
    void L6_CONFIRMED와_CANCELED에는_InvalidStateTransition이다() {
        Booking booking = fixtures.held(1);
        bookingPaymentService.requestPayment(new RequestPaymentCommand(GUEST, booking.id(), MockMode.APPROVE));
        assertEquals(BookingStatus.CONFIRMED, fixtures.reload(booking.id()).status());

        InvalidStateTransitionException confirmed = assertThrows(InvalidStateTransitionException.class,
                () -> bookingPaymentService.requestPayment(
                        new RequestPaymentCommand(GUEST, booking.id(), MockMode.APPROVE)));
        assertEquals(BookingStatus.CONFIRMED, confirmed.from());

        bookingApplicationService.cancelBooking(new CancelBookingCommand(GUEST, booking.id(), "사정"));
        assertEquals(BookingStatus.CANCELED, fixtures.reload(booking.id()).status());

        InvalidStateTransitionException canceled = assertThrows(InvalidStateTransitionException.class,
                () -> bookingPaymentService.requestPayment(
                        new RequestPaymentCommand(GUEST, booking.id(), MockMode.APPROVE)));
        assertEquals(BookingStatus.CANCELED, canceled.from());
        assertEquals(1, paymentService.attemptsOf(booking.id().value()).attemptCount());
    }

    @Test
    void L6_남의_예약과_없는_예약은_자원_정보_없이_BookingNotFound다() {
        // T02의 결제 구간. PAY-01과 PAY-02 둘 다
        Booking booking = fixtures.held(1);

        assertThrows(BookingNotFoundException.class, () -> bookingPaymentService.requestPayment(
                new RequestPaymentCommand(OTHER_GUEST, booking.id(), MockMode.APPROVE)));
        assertThrows(BookingNotFoundException.class, () -> bookingPaymentService.paymentAttempts(
                OTHER_GUEST, booking.id()));
        assertThrows(BookingNotFoundException.class, () -> bookingPaymentService.requestPayment(
                new RequestPaymentCommand(GUEST, BookingId.of("booking_none"), MockMode.APPROVE)));

        assertEquals(0, paymentService.attemptsOf(booking.id().value()).attemptCount());
        assertEquals(BookingStatus.HELD, fixtures.reload(booking.id()).status());
    }

    @Test
    void L6_시도_목록은_본인_예약의_전부를_번호_순서로_주고_시도가_없으면_비어_있다() {
        Booking booking = fixtures.held(1);
        PaymentSummaryView empty = bookingPaymentService.paymentAttempts(GUEST, booking.id());
        assertEquals(0, empty.attemptCount());
        assertTrue(empty.attempts().isEmpty());
        assertNull(empty.approvedAttemptId());
        assertNull(empty.refund());

        bookingPaymentService.requestPayment(new RequestPaymentCommand(GUEST, booking.id(), MockMode.DECLINE));
        bookingPaymentService.requestPayment(new RequestPaymentCommand(GUEST, booking.id(), MockMode.APPROVE));

        PaymentSummaryView summary = bookingPaymentService.paymentAttempts(GUEST, booking.id());
        assertEquals(2, summary.attemptCount());
        assertEquals(1, summary.attempts().get(0).attemptNumber());
        assertEquals("FAILED", summary.attempts().get(0).status());
        assertEquals(2, summary.attempts().get(1).attemptNumber());
        assertEquals("APPROVED", summary.attempts().get(1).status());
        assertEquals(summary.attempts().get(1).id(), summary.approvedAttemptId());
        assertEquals(summary, bookingPaymentService.paymentSummaryOf(booking.id()));
    }

    /** 만료 시각을 지난 HELD를 T1의 건별 처리로 끝낸다. 시계를 먼저 돌려 둔다 */
    private void 만료(BookingId bookingId) {
        assertEquals(BookingExpirationService.Outcome.EXPIRED, expirationService.expireIfDue(bookingId));
    }
}
