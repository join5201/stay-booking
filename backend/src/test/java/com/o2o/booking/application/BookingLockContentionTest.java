package com.o2o.booking.application;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
import com.o2o.booking.domain.BookingRepository;
import com.o2o.booking.domain.BookingStatus;
import com.o2o.booking.domain.ExpirationReason;
import com.o2o.payment.application.PaymentApplicationService;
import com.o2o.payment.application.RefundView;
import com.o2o.payment.domain.PaymentApproved;
import com.o2o.payment.domain.PaymentAttempt;
import com.o2o.payment.domain.PaymentId;
import com.o2o.payment.domain.RefundReason;

import static com.o2o.booking.BookingFixtures.CHECK_IN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * L23. 설계 근거: task-S9-booking-lifecycle 8-1절 L23, 2절 T1 표(잠금 뒤 재확인)와 P1 표, T19(만료
 * 처리와 승인 처리가 겹쳐도 한 경로만 남는다), 08-3 결정 3(잠금 순서 Booking, Payment, 재고)과
 * 11-1(확정 우선), 1차 K19의 두 트랜잭션 경합 방식.
 *
 * 테스트 트랜잭션이 Booking 행을 잠근 채 T1 건별 처리(expireIfDue)와 P1 승인 처리(onApproved)를
 * 스레드 둘로 보낸다. 둘 다 REQUIRES_NEW라 각자 새 트랜잭션에서 같은 행을 잠그려다 기다린다.
 * 잠금을 풀면 MySQL이 둘 중 하나를 먼저 들여보내고 남은 하나는 그 결과를 잠근 뒤 다시 읽는다.
 * 그래서 두 번째는 종착 상태를 보고 아무것도 하지 않는다. 승인 시도를 심어 두고(T6) 시계를 만료
 * 시각 뒤로 둔다.
 *
 * 기준 시점은 승인 기록의 서버 시각 하나다(2차 계약 개정 11. R1 평가 B-02 반영, 2026-09-15). 그래서
 * 누가 먼저 잠그든 결과가 하나다. 전에는 만료가 먼저면 확정, 승인이 먼저면 환불과 만료로 갈렸고
 * 이 테스트가 둘 다 허용했다.
 *
 * <ul>
 * <li>L23. 승인 기록이 만료 전: 어느 쪽이 먼저든 CONFIRMED다. 만료가 먼저면 확정 우선으로 확정하고
 * 뒤의 승인은 종착을 보고 무시한다. 승인이 먼저면 승인 시각으로 확정하고 뒤의 만료는 SKIPPED다.
 * 환불 없음, sold 1</li>
 * <li>L26. 승인 기록이 만료 시각과 같음(T18 경계): 어느 쪽이 먼저든 EXPIRED와 TTL_EXPIRED와 환불
 * LATE_APPROVAL 하나다. 만료가 먼저면 T1이 환불하고 만료시키며 뒤의 승인은 EXPIRED를 보고 환불만
 * 시도하나 이미 환불이라 전이가 없다. 승인이 먼저면 P1이 환불하고 만료시키며 뒤의 만료는 SKIPPED다.
 * sold 0</li>
 * </ul>
 *
 * 어느 순서로 들어가는지는 InnoDB가 정하므로 보내는 순서를 바꾼 짝을 둬 실행마다 두 순서가 모두
 * 밟히도록 기울인다. 재고는 어느 쪽이든 held 0이다.
 */
@SpringBootTest
@Import(BookingLifecycleTestConfiguration.class)
class BookingLockContentionTest {

    @Autowired
    private BookingExpirationService expirationService;

    @Autowired
    private PaymentOutcomeService outcomeService;

    @Autowired
    private PaymentApplicationService paymentService;

    @Autowired
    private BookingRepository bookingRepository;

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
    void L23_만료를_먼저_보내고_승인을_이어_보내도_만료_전_승인은_CONFIRMED_하나이고_held는_0이다() throws Exception {
        경합(true, true);
    }

    @Test
    void L23_승인을_먼저_보내고_만료를_이어_보내도_만료_전_승인은_CONFIRMED_하나이고_held는_0이다() throws Exception {
        경합(false, true);
    }

    @Test
    void L26_만료를_먼저_보내고_승인을_이어_보내도_만료_시각의_승인은_EXPIRED와_환불_하나이고_held는_0이다()
            throws Exception {
        경합(true, false);
    }

    @Test
    void L26_승인을_먼저_보내고_만료를_이어_보내도_만료_시각의_승인은_EXPIRED와_환불_하나이고_held는_0이다()
            throws Exception {
        경합(false, false);
    }

    private void 경합(boolean expiryFirst, boolean approvedBeforeExpiry) throws Exception {
        Booking booking = fixtures.held(1);
        if (!approvedBeforeExpiry) {
            // T18 경계. 승인 기록의 서버 시각이 정확히 expiresAt이면 지연 승인이다
            clock.set(booking.expiresAt());
        }
        PaymentAttempt approved = fixtures.seedApproved(booking);
        PaymentApproved event = new PaymentApproved(PaymentId.newId(), booking.id().value(),
                approved.id(), approved.pgTransactionId(), fixtures.charge(booking), 1, approved.completedAt());
        clock.set(booking.expiresAt().plus(Duration.ofMinutes(1)));

        ExecutorService threads = Executors.newFixedThreadPool(2);
        try {
            Outcomes outcomes = new TransactionTemplate(transactionManager).execute((status) -> {
                bookingRepository.findByIdForUpdate(booking.id()).orElseThrow();
                Future<BookingExpirationService.Outcome> expiry;
                Future<?> approval;
                // 먼저 보낸 쪽이 잠금 대기열에 먼저 서도록 사이를 둔다. 들여보내는 순서는 그래도 InnoDB 몫이다
                if (expiryFirst) {
                    expiry = threads.submit(() -> expirationService.expireIfDue(booking.id()));
                    pause();
                    approval = threads.submit(() -> outcomeService.onApproved(event));
                } else {
                    approval = threads.submit(() -> outcomeService.onApproved(event));
                    pause();
                    expiry = threads.submit(() -> expirationService.expireIfDue(booking.id()));
                }
                // 잠금을 쥔 동안 둘 다 끝나지 않는다. 끝났으면 Booking을 잠그지 않고 전이한 것이다
                assertThrows(TimeoutException.class, () -> expiry.get(2, TimeUnit.SECONDS));
                assertThrows(TimeoutException.class, () -> approval.get(2, TimeUnit.SECONDS));
                return new Outcomes(expiry, approval);
            });

            BookingExpirationService.Outcome expiryOutcome = outcomes.expiry().get(30, TimeUnit.SECONDS);
            outcomes.approval().get(30, TimeUnit.SECONDS);

            Booking after = fixtures.reload(booking.id());
            RefundView refund = paymentService.attemptsOf(booking.id().value()).refund();
            assertEquals(1L, after.version(), "전이는 한 번이다");
            assertEquals(0, fixtures.heldCount(booking.roomTypeId(), CHECK_IN));
            if (approvedBeforeExpiry) {
                // 결과는 하나다. 만료가 먼저면 확정 우선(CONFIRMED), 승인이 먼저면 뒤의 만료가 SKIPPED
                assertEquals(BookingStatus.CONFIRMED, after.status());
                assertTrue(expiryOutcome == BookingExpirationService.Outcome.CONFIRMED
                        || expiryOutcome == BookingExpirationService.Outcome.SKIPPED, "" + expiryOutcome);
                assertEquals(clock.instant(), after.confirmedAt());
                assertNull(refund);
                assertEquals(1, fixtures.soldCount(booking.roomTypeId(), CHECK_IN));
                assertEquals(1, events.confirmedOf(booking.id()));
                assertEquals(0, events.expiredOf(booking.id()));
            } else {
                // 결과는 하나다. 만료가 먼저면 T1이 환불하고 만료(EXPIRED), 승인이 먼저면 뒤의 만료가 SKIPPED
                assertEquals(BookingStatus.EXPIRED, after.status());
                assertEquals(ExpirationReason.TTL_EXPIRED, after.expirationReason());
                assertTrue(expiryOutcome == BookingExpirationService.Outcome.EXPIRED
                        || expiryOutcome == BookingExpirationService.Outcome.SKIPPED, "" + expiryOutcome);
                assertNotNull(refund);
                assertEquals(approved.id().value(), refund.paymentAttemptId());
                assertEquals(RefundReason.LATE_APPROVAL, refund.reason());
                assertEquals(0, fixtures.soldCount(booking.roomTypeId(), CHECK_IN));
                assertEquals(0, events.confirmedOf(booking.id()));
                assertEquals(1, events.expiredOf(booking.id()));
            }
            assertEquals(0, events.canceledOf(booking.id()));
        } finally {
            threads.shutdownNow();
        }
    }

    private static void pause() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private record Outcomes(Future<BookingExpirationService.Outcome> expiry, Future<?> approval) {
    }
}
