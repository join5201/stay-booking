package com.o2o.booking.application;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.o2o.booking.BookingFixtures;
import com.o2o.booking.BookingLifecycleTestConfiguration;
import com.o2o.booking.CommittedBookingEvents;
import com.o2o.booking.MutableClock;
import com.o2o.booking.domain.Booking;
import com.o2o.booking.domain.BookingCanceled;
import com.o2o.booking.domain.BookingConfirmed;
import com.o2o.booking.domain.BookingExpired;
import com.o2o.booking.domain.BookingStatus;
import com.o2o.booking.domain.ExpirationReason;
import com.o2o.inventory.domain.InventoryCommitted;
import com.o2o.inventory.domain.InventoryReleased;
import com.o2o.payment.application.PaymentApplicationService;
import com.o2o.payment.domain.MockMode;

import static com.o2o.booking.BookingFixtures.CHECK_IN;
import static com.o2o.booking.BookingFixtures.GUEST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * L12. 설계 근거: task-S9-booking-lifecycle 8-1절, layers.md 3-3 E1(구독자는 AFTER_COMMIT)과 E2(롤백된
 * 요청의 이벤트는 구독자에 닿지 않는다), 06-2 4절 생성 경계의 커밋 후 이벤트, 06-4 v5 2-4의 재고
 * 이벤트(계약 개정 2), 1차 K12 방식.
 *
 * 전이 셋의 이벤트와 재고 이벤트를 커밋 뒤 도착으로 센다. 확정은 InventoryCommitted N건, 만료는
 * InventoryReleased N건(HELD), 취소는 InventoryReleased N건(SOLD)이 같이 닿는다. 롤백은 취소를
 * 바깥 트랜잭션으로 감싸 되돌린다. 발행은 트랜잭션 안에서 했으므로 @EventListener로 걸었다면 닿았을
 * 이벤트다.
 */
@SpringBootTest
@Import(BookingLifecycleTestConfiguration.class)
class BookingLifecycleEventTest {

    @Autowired
    private BookingApplicationService bookingApplicationService;

    @Autowired
    private BookingExpirationService expirationService;

    @Autowired
    private PaymentApplicationService paymentService;

    @Autowired
    private PlatformTransactionManager transactionManager;

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
    void L12_확정하면_BookingConfirmed_1건과_InventoryCommitted_N건이_커밋_뒤_닿는다() {
        Booking booking = fixtures.held(3);

        paymentService.openAttempt(booking.id().value(), fixtures.charge(booking), MockMode.APPROVE);

        assertEquals(1, events.confirmedOf(booking.id()));
        BookingConfirmed confirmed = events.confirmed.stream()
                .filter((e) -> e.bookingId().equals(booking.id())).findFirst().orElseThrow();
        assertEquals(clock.instant(), confirmed.occurredAt());
        List<InventoryCommitted> committed = events.committedOf(booking.roomTypeId());
        assertEquals(3, committed.size());
        assertEquals(List.of(CHECK_IN, CHECK_IN.plusDays(1), CHECK_IN.plusDays(2)),
                committed.stream().map(InventoryCommitted::stayDate).toList());
        assertTrue(committed.stream().allMatch((e) -> e.soldCount() == 1));
        assertEquals(0, events.expiredOf(booking.id()) + events.canceledOf(booking.id()));
        assertEquals(0, events.inventoryReleased.size());
    }

    @Test
    void L12_만료하면_BookingExpired_1건과_InventoryReleased_HELD_N건이_커밋_뒤_닿는다() {
        Booking booking = fixtures.held(2);
        clock.advance(Duration.ofMinutes(10));

        expirationService.expireIfDue(booking.id());

        assertEquals(1, events.expiredOf(booking.id()));
        BookingExpired expired = events.expired.stream()
                .filter((e) -> e.bookingId().equals(booking.id())).findFirst().orElseThrow();
        assertEquals(ExpirationReason.TTL_EXPIRED, expired.reason());
        assertEquals(clock.instant(), expired.occurredAt());
        List<InventoryReleased> released = events.releasedOf(booking.roomTypeId(), InventoryReleased.Source.HELD);
        assertEquals(2, released.size());
        assertEquals(List.of(CHECK_IN, CHECK_IN.plusDays(1)),
                released.stream().map(InventoryReleased::stayDate).toList());
        assertTrue(released.stream().allMatch((e) -> e.availableCount() == 2));
        assertEquals(0, events.releasedOf(booking.roomTypeId(), InventoryReleased.Source.SOLD).size());
        assertEquals(0, events.confirmedOf(booking.id()));
    }

    @Test
    void L12_취소하면_BookingCanceled_1건과_InventoryReleased_SOLD_N건이_커밋_뒤_닿는다() {
        Booking booking = fixtures.held(2);
        paymentService.openAttempt(booking.id().value(), fixtures.charge(booking), MockMode.APPROVE);
        events.clear();

        bookingApplicationService.cancelBooking(new CancelBookingCommand(GUEST, booking.id(), "사정"));

        assertEquals(1, events.canceledOf(booking.id()));
        BookingCanceled canceled = events.canceled.stream()
                .filter((e) -> e.bookingId().equals(booking.id())).findFirst().orElseThrow();
        assertEquals(clock.instant(), canceled.occurredAt());
        List<InventoryReleased> released = events.releasedOf(booking.roomTypeId(), InventoryReleased.Source.SOLD);
        assertEquals(2, released.size());
        assertTrue(released.stream().allMatch((e) -> e.availableCount() == 2));
        assertEquals(0, events.releasedOf(booking.roomTypeId(), InventoryReleased.Source.HELD).size());
        assertEquals(0, events.inventoryCommitted.size());
    }

    @Test
    void L12_롤백된_취소의_이벤트는_구독자에_닿지_않고_상태도_그대로다() {
        // E2. 취소를 바깥 트랜잭션이 감싸고 되돌린다. 앱 서비스는 거기 합류하므로 커밋이 없다
        Booking booking = fixtures.held(1);
        paymentService.openAttempt(booking.id().value(), fixtures.charge(booking), MockMode.APPROVE);
        events.clear();

        Booking inside = new TransactionTemplate(transactionManager).execute((status) -> {
            Booking canceled = bookingApplicationService.cancelBooking(
                    new CancelBookingCommand(GUEST, booking.id(), "되돌릴 취소"));
            status.setRollbackOnly();
            return canceled;
        });

        assertEquals(BookingStatus.CANCELED, inside.status());
        assertEquals(BookingStatus.CONFIRMED, fixtures.reload(booking.id()).status());
        assertEquals(1, fixtures.soldCount(booking.roomTypeId(), CHECK_IN));
        assertEquals(0, events.canceledOf(booking.id()));
        assertEquals(0, events.inventoryReleased.size());
        assertEquals(1, paymentService.attemptsOf(booking.id().value()).attemptCount());
        assertNull(paymentService.attemptsOf(booking.id().value()).refund());

        // 통과 쪽 짝. 같은 취소를 커밋하면 닿는다
        bookingApplicationService.cancelBooking(new CancelBookingCommand(GUEST, booking.id(), "진짜 취소"));
        assertEquals(1, events.canceledOf(booking.id()));
        assertEquals(1, events.releasedOf(booking.roomTypeId(), InventoryReleased.Source.SOLD).size());
    }
}
