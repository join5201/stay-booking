package com.o2o.booking.application;

import java.time.Duration;
import java.util.List;

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
import com.o2o.booking.domain.BookingId;
import com.o2o.booking.domain.BookingRepository;
import com.o2o.booking.domain.BookingStatus;
import com.o2o.booking.domain.ExpirationReason;
import com.o2o.payment.application.PaymentApplicationService;
import com.o2o.payment.domain.MockMode;
import com.o2o.payment.domain.PaymentAttempt;

import static com.o2o.booking.BookingFixtures.CHECK_IN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * L9. 설계 근거: task-S9-booking-lifecycle 8-1절, 2절 T1 표(잠금 뒤 재확인 넷), 11 P01(서버가 주기적으로
 * 스캔해 만료), 08-3 결정 11의 11-1(확정 우선)과 11-4(배치 크기), 7절 D-1 나(P1 유실의 자가 치유 자리).
 *
 * 스케줄러는 테스트 설정에서 꺼져 있다(7절 D-3 가). 여기는 스케줄러가 부르는 한 바퀴(runOnce)를 직접
 * 부른다. 배치 크기는 생성자로 받으므로 작은 값의 인스턴스를 테스트가 따로 만든다.
 *
 * 스캔은 DB 전체의 due HELD를 본다. 다른 테스트가 남긴 due HELD가 섞이지 않게 배치 테스트는 먼저
 * 기본 인스턴스로 한 바퀴 돌려 비운 뒤 자기 예약 셋을 만든다.
 */
@SpringBootTest
@Import(BookingLifecycleTestConfiguration.class)
class ExpireDueBookingsTest {

    @Autowired
    private ExpireDueBookings expireDueBookings;

    @Autowired
    private BookingExpirationService expirationService;

    @Autowired
    private BookingRepository bookingRepository;

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
    void L9_만료_시각_지난_HELD는_TTL_EXPIRED가_되고_held가_줄며_지나지_않은_HELD는_그대로다() {
        Booking due = fixtures.held(2);
        clock.advance(Duration.ofMinutes(5));
        Booking notDue = fixtures.held(1);
        clock.set(due.expiresAt());

        ExpireDueBookings.Summary summary = expireDueBookings.runOnce();

        assertTrue(summary.scanned() >= 1);
        assertTrue(summary.expired() >= 1);
        Booking expired = fixtures.reload(due.id());
        assertEquals(BookingStatus.EXPIRED, expired.status());
        assertEquals(ExpirationReason.TTL_EXPIRED, expired.expirationReason());
        assertEquals(due.expiresAt(), expired.expiredAt());
        assertEquals(0, fixtures.heldCount(due.roomTypeId(), CHECK_IN));
        assertEquals(0, fixtures.heldCount(due.roomTypeId(), CHECK_IN.plusDays(1)));
        assertEquals(1, events.expiredOf(due.id()));

        assertEquals(BookingStatus.HELD, fixtures.reload(notDue.id()).status());
        assertEquals(1, fixtures.heldCount(notDue.roomTypeId(), CHECK_IN));
        assertEquals(0, events.expiredOf(notDue.id()));
        assertFalse(bookingRepository.findDueIds(clock.instant(), 100).contains(due.id()));
    }

    @Test
    void L9_만료_시각이_지났지만_승인_시도가_있으면_만료_대신_확정하고_commit한다() {
        // 확정 우선. P1이 유실된 상태(HELD인데 승인 이력 있음)를 결제 집합체에 직접 심어 만든다(T6)
        Booking booking = fixtures.held(2);
        PaymentAttempt approved = fixtures.seedApproved(booking);
        assertEquals(approved.id().value(), paymentService.attemptsOf(booking.id().value()).approvedAttemptId());
        assertEquals(BookingStatus.HELD, fixtures.reload(booking.id()).status());
        clock.advance(Duration.ofMinutes(10));

        assertEquals(BookingExpirationService.Outcome.CONFIRMED, expirationService.expireIfDue(booking.id()));

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
        // 두 번째 처리는 스킵이다. 종착 무해
        assertEquals(BookingExpirationService.Outcome.SKIPPED, expirationService.expireIfDue(booking.id()));
        assertEquals(1L, fixtures.reload(booking.id()).version());
    }

    @Test
    void L9_이미_CONFIRMED거나_아직_만료_전이거나_없는_예약은_스킵이다() {
        Booking confirmed = fixtures.held(1);
        paymentService.openAttempt(confirmed.id().value(), fixtures.charge(confirmed), MockMode.APPROVE);
        assertEquals(BookingStatus.CONFIRMED, fixtures.reload(confirmed.id()).status());
        Booking fresh = fixtures.held(1);

        assertEquals(BookingExpirationService.Outcome.SKIPPED, expirationService.expireIfDue(fresh.id()));
        assertEquals(BookingStatus.HELD, fixtures.reload(fresh.id()).status());
        clock.advance(Duration.ofMinutes(10));
        assertEquals(BookingExpirationService.Outcome.SKIPPED, expirationService.expireIfDue(confirmed.id()));
        assertEquals(BookingExpirationService.Outcome.SKIPPED,
                expirationService.expireIfDue(BookingId.of("booking_none")));

        assertEquals(BookingStatus.CONFIRMED, fixtures.reload(confirmed.id()).status());
        assertEquals(1, fixtures.soldCount(confirmed.roomTypeId(), CHECK_IN));
        // 짝. 만료 전에 스킵된 같은 예약이 만료 시각을 지나면 EXPIRED다
        assertEquals(BookingExpirationService.Outcome.EXPIRED, expirationService.expireIfDue(fresh.id()));
        assertEquals(BookingStatus.EXPIRED, fixtures.reload(fresh.id()).status());
    }

    @Test
    void L9_배치_크기보다_많으면_오래된_것부터_배치만큼만_처리한다() {
        // 먼저 남아 있는 due HELD를 전부 비운다. 그 뒤 만료 시각이 서로 다른 셋을 만든다
        clock.advance(Duration.ofHours(1));
        expireDueBookings.runOnce();
        assertTrue(bookingRepository.findDueIds(clock.instant(), 100).isEmpty());
        clock.reset();
        Booking oldest = fixtures.held(1);
        clock.advance(Duration.ofMinutes(1));
        Booking middle = fixtures.held(1);
        clock.advance(Duration.ofMinutes(1));
        Booking newest = fixtures.held(1);
        clock.advance(Duration.ofHours(1));
        assertEquals(List.of(oldest.id(), middle.id(), newest.id()),
                bookingRepository.findDueIds(clock.instant(), 100));
        ExpireDueBookings twoAtATime = new ExpireDueBookings(bookingRepository, expirationService, clock, 2);

        ExpireDueBookings.Summary first = twoAtATime.runOnce();

        assertEquals(2, first.scanned());
        assertEquals(2, first.expired());
        assertEquals(BookingStatus.EXPIRED, fixtures.reload(oldest.id()).status());
        assertEquals(BookingStatus.EXPIRED, fixtures.reload(middle.id()).status());
        assertEquals(BookingStatus.HELD, fixtures.reload(newest.id()).status());
        assertEquals(List.of(newest.id()), bookingRepository.findDueIds(clock.instant(), 100));

        ExpireDueBookings.Summary second = twoAtATime.runOnce();

        assertEquals(1, second.scanned());
        assertEquals(1, second.expired());
        assertEquals(BookingStatus.EXPIRED, fixtures.reload(newest.id()).status());
        assertEquals(0, twoAtATime.runOnce().scanned());
    }
}
