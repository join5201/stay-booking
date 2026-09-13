package com.o2o.payment.application;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.o2o.payment.domain.AttemptInProgressException;
import com.o2o.payment.domain.MockMode;
import com.o2o.payment.domain.MockOutcome;
import com.o2o.payment.domain.NoApprovedAttemptException;
import com.o2o.payment.domain.Payment;
import com.o2o.payment.domain.PaymentAttempt;
import com.o2o.payment.domain.PaymentAttemptId;
import com.o2o.payment.domain.PaymentRepository;
import com.o2o.payment.domain.RefundReason;
import com.o2o.payment.domain.UnknownAttemptException;
import com.o2o.payment.infrastructure.InProcessMockPaymentGateway;
import com.o2o.shared.Money;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Y9와 Y10과 Y14, 그리고 Y7의 앱 서비스 몫. 설계 근거: 계약 8-1절, U3과 U4(06-2 3-4, 06-4 v5 1-1),
 * 06-2 5절 Payment 행(시도 추가와 콜백 기록은 루트를 잠근 뒤), 08-3 결정 3과 4, 11 PAY-02 처리
 * 규칙과 응답 모델 PaymentSummary와 Refund.
 *
 * 실제 MySQL에 붙는다(T3, BN4). 유일성은 DB 책임이고(06-4 1-4) 잠금은 두 트랜잭션이 실제로
 * 경합해야 보인다. 그래서 이 클래스는 테스트 트랜잭션으로 감싸지 않는다. Y10이 다른 스레드에서
 * 커밋된 행을 봐야 하기 때문이다. 예약 ID를 건마다 새로 만들어 격리한다. 결제는 예약을
 * 모르므로(06-1 R6) 예약 행이 없어도 문자열 하나면 된다.
 *
 * 시각을 고정한다. 응답 시각 단정을 위해서다. 오늘에서 세는 값이 없어 날짜 경계는 없다.
 */
@SpringBootTest
class PaymentApplicationServiceTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-10-01T03:00:00Z");
    private static final Money CHARGE = Money.krw(180_000);

    @TestConfiguration
    static class FixedClockConfiguration {

        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
        }
    }

    @Autowired
    private PaymentApplicationService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private InProcessMockPaymentGateway gateway;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private static String newBookingId() {
        return "bk_test_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    private static Throwable root(Throwable t) {
        Throwable cur = t;
        while (cur.getCause() != null && cur.getCause() != cur) {
            cur = cur.getCause();
        }
        return cur;
    }

    private MockEventCommand event(String eventId, PaymentAttemptView attempt, MockOutcome outcome) {
        String failureCode = outcome == MockOutcome.FAILED ? PaymentAttempt.MOCK_DECLINED : null;
        return new MockEventCommand(eventId, attempt.id(), attempt.pgTransactionId(), outcome,
                attempt.amount(), attempt.currency(), failureCode);
    }

    /**
     * Y9. U3. 같은 bookingId의 둘째 Payment를 DB 유니크가 거부한다. 앱 서비스의 잠금 조회가 첫 요청
     * 판정을 하지만 최종 방어는 DB다(06-4 1-4). 리포지토리로 직접 두 행을 넣어 그 방어선만 본다.
     */
    @Test
    void Y9_같은_bookingId의_둘째_Payment는_유니크가_거부한다() {
        String bookingId = newBookingId();
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        Exception e = assertThrows(Exception.class, () -> tx.execute(status -> {
            paymentRepository.save(Payment.open(bookingId, CHARGE, FIXED_NOW));
            paymentRepository.save(Payment.open(bookingId, CHARGE, FIXED_NOW));
            return null;
        }));
        assertTrue(root(e).getMessage().contains("uk_payment_booking"), root(e).getMessage());

        // 통과 쪽 짝. 다른 bookingId는 들어간다
        String other = newBookingId();
        tx.execute(status -> paymentRepository.save(Payment.open(other, CHARGE, FIXED_NOW)));
        assertTrue(paymentRepository.findByBookingId(other).isPresent());
    }

    /**
     * Y9. U4의 강제 수단. pg_transaction_id 단일 컬럼 유니크가 같은 값의 둘째 시도를 거부한다
     * (06-4 v5 1-1 U4, 08-3 결정 2). 거래 번호는 Mock PG가 유일하게 주므로 여기서는 손으로 붙인다.
     */
    @Test
    void Y9_같은_pgTransactionId의_둘째_시도는_유니크가_거부한다() {
        String duplicated = "mock_tx_dup_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        Exception e = assertThrows(Exception.class, () -> tx.execute(status -> {
            for (int i = 0; i < 2; i++) {
                Payment payment = Payment.open(newBookingId(), CHARGE, FIXED_NOW);
                PaymentAttempt attempt = payment.openAttempt(CHARGE, MockMode.DEFER, FIXED_NOW);
                payment.attachPgTransaction(attempt.id(), duplicated);
                paymentRepository.save(payment);
            }
            return null;
        }));
        assertTrue(root(e).getMessage().contains("uk_payment_attempt_pg_transaction"),
                root(e).getMessage());
    }

    /**
     * Y10. 두 트랜잭션이 실제 MySQL에서 같은 Payment 행을 두고 경합한다. 앞 묶음 V14의 방식이다.
     * 06-2 5절이 시도 추가는 루트를 잠근 뒤 하라고 적고 08-3 결정 3이 그 잠금 순서를 정한다.
     *
     * 방법. 테스트 트랜잭션이 Payment 행을 잠근 채 다른 스레드로 같은 예약의 openAttempt를 보낸다.
     * 잠금이 풀리기 전에 끝나면 잠금 없이 읽은 것이다. 풀린 뒤에는 그때의 상태(REQUESTED 하나)를
     * 보고 I9로 거절해야 한다. 순차 호출 둘로는 이것을 볼 수 없다.
     */
    @Test
    void Y10_잠긴_Payment_행의_openAttempt는_잠금이_풀린_뒤_I9로_거절된다() throws Exception {
        String bookingId = newBookingId();
        paymentService.openAttempt(bookingId, CHARGE, MockMode.DEFER);

        ExecutorService waiter = Executors.newSingleThreadExecutor();
        try {
            Future<PaymentAttemptView> request = new TransactionTemplate(transactionManager)
                    .execute(status -> {
                        Payment locked = paymentRepository.findByBookingIdForUpdate(bookingId).orElseThrow();
                        assertEquals(1, locked.attemptCount());
                        Future<PaymentAttemptView> sent = waiter.submit(
                                () -> paymentService.openAttempt(bookingId, CHARGE, MockMode.DEFER));
                        // 잠금을 쥔 동안 끝나면 잠금 없이 읽은 것이다
                        assertThrows(TimeoutException.class, () -> sent.get(2, TimeUnit.SECONDS));
                        return sent;
                    });

            ExecutionException e = assertThrows(ExecutionException.class,
                    () -> request.get(30, TimeUnit.SECONDS));
            assertInstanceOf(AttemptInProgressException.class, e.getCause(), String.valueOf(e.getCause()));
        } finally {
            waiter.shutdownNow();
        }
        assertEquals(1, paymentService.attemptsOf(bookingId).attemptCount());
    }

    /**
     * Y7의 앱 서비스 몫. 환불의 멱등키가 attemptId다(08-3 결정 4). Mock 환불은 Payment 잠금을 쥔 채
     * 같은 트랜잭션에서 부른다(08-3 결정 10). 두 번째 refund는 같은 환불을 돌려주고 PG를 다시
     * 부르지 않는다. REQUESTED에 refund는 NoApprovedAttempt, 없는 시도는 UnknownAttempt다.
     */
    @Test
    void Y7_refund는_attemptId를_멱등키로_Mock_환불을_부르고_두_번째는_같은_환불을_돌려준다() {
        String bookingId = newBookingId();
        PaymentAttemptView attempt = paymentService.openAttempt(bookingId, CHARGE, MockMode.DEFER);
        PaymentAttemptId attemptId = PaymentAttemptId.of(attempt.id());
        assertThrows(NoApprovedAttemptException.class,
                () -> paymentService.refund(attemptId, RefundReason.BOOKING_CANCELED));
        paymentService.handleMockEvent(event("ev_" + bookingId, attempt, MockOutcome.APPROVED));

        RefundView first = paymentService.refund(attemptId, RefundReason.BOOKING_CANCELED);
        RefundView second = paymentService.refund(attemptId, RefundReason.LATE_APPROVAL);

        assertTrue(gateway.hasRefunded(attemptId));
        assertEquals(first, second);
        assertEquals(RefundReason.BOOKING_CANCELED, second.reason());
        assertEquals(FIXED_NOW, second.refundedAt());
        assertThrows(UnknownAttemptException.class,
                () -> paymentService.refund(PaymentAttemptId.of("attempt_none"), RefundReason.BOOKING_CANCELED));
    }

    /**
     * Y14. attemptsOf. 11 PAY-02 처리 규칙(attemptNumber 오름차순, NORMAL만)과 응답 모델
     * PaymentSummary(approvedAttemptId는 환불해도 유지)와 Refund(7개 필드)와 PaymentAttempt(11개 필드).
     * 환불된 시도는 status APPROVED로 나온다(계약 7절 D-5).
     */
    @Test
    void Y14_attemptsOf는_오름차순_목록과_attemptCount와_approvedAttemptId와_환불_뷰를_준다() {
        String bookingId = newBookingId();
        PaymentAttemptView first = paymentService.openAttempt(bookingId, CHARGE, MockMode.DEFER);
        paymentService.handleMockEvent(event("ev_1_" + bookingId, first, MockOutcome.FAILED));

        PaymentSummaryView afterFailure = paymentService.attemptsOf(bookingId);
        assertEquals(1, afterFailure.attemptCount());
        assertNull(afterFailure.approvedAttemptId());
        assertNull(afterFailure.refund());
        assertEquals("FAILED", afterFailure.attempts().get(0).status());
        assertEquals(PaymentAttempt.MOCK_DECLINED, afterFailure.attempts().get(0).failureCode());

        PaymentAttemptView second = paymentService.openAttempt(bookingId, CHARGE, MockMode.DEFER);
        paymentService.handleMockEvent(event("ev_2_" + bookingId, second, MockOutcome.APPROVED));

        PaymentSummaryView afterApproval = paymentService.attemptsOf(bookingId);
        assertEquals(2, afterApproval.attemptCount());
        assertEquals(second.id(), afterApproval.approvedAttemptId());
        assertNull(afterApproval.refund());
        assertEquals(List.of(1, 2), afterApproval.attempts().stream()
                .map(PaymentAttemptView::attemptNumber).toList());
        PaymentAttemptView approved = afterApproval.attempts().get(1);
        assertEquals(second.id(), approved.id());
        assertEquals(bookingId, approved.bookingId());
        assertEquals("APPROVED", approved.status());
        assertEquals(180_000L, approved.amount());
        assertEquals("KRW", approved.currency());
        assertTrue(approved.pgTransactionId().startsWith("mock_tx_"), approved.pgTransactionId());
        assertEquals(MockMode.DEFER, approved.mockMode());
        assertEquals(FIXED_NOW, approved.requestedAt());
        assertEquals(FIXED_NOW, approved.completedAt());
        assertNull(approved.failureCode());

        paymentService.refund(PaymentAttemptId.of(second.id()), RefundReason.BOOKING_CANCELED);

        PaymentSummaryView afterRefund = paymentService.attemptsOf(bookingId);
        assertEquals(2, afterRefund.attemptCount());
        assertEquals(second.id(), afterRefund.approvedAttemptId());
        assertEquals("APPROVED", afterRefund.attempts().get(1).status());
        RefundView refund = afterRefund.refund();
        assertNotNull(refund);
        assertEquals("refund_" + second.id().substring("attempt_".length()), refund.id());
        assertEquals(second.id(), refund.paymentAttemptId());
        assertEquals(180_000L, refund.amount());
        assertEquals("KRW", refund.currency());
        assertEquals("REFUNDED", refund.status());
        assertEquals(RefundReason.BOOKING_CANCELED, refund.reason());
        assertEquals(FIXED_NOW, refund.refundedAt());
    }

    @Test
    void Y14_Payment가_없는_예약은_0과_빈_목록과_null이다() {
        PaymentSummaryView summary = paymentService.attemptsOf(newBookingId());

        assertEquals(0, summary.attemptCount());
        assertNull(summary.approvedAttemptId());
        assertEquals(List.of(), summary.attempts());
        assertNull(summary.refund());
    }

    @Test
    void openAttempt는_bookingId와_amount를_검사한다() {
        // 계약 2절 openAttempt 표 1행. 호출자 잘못은 IllegalArgument다
        assertThrows(IllegalArgumentException.class,
                () -> paymentService.openAttempt(" ", CHARGE, MockMode.DEFER));
        assertThrows(IllegalArgumentException.class,
                () -> paymentService.openAttempt("b".repeat(65), CHARGE, MockMode.DEFER));
        assertThrows(IllegalArgumentException.class,
                () -> paymentService.openAttempt(newBookingId(), Money.krw(0), MockMode.DEFER));
        assertEquals("REQUESTED", paymentService.openAttempt(newBookingId(), CHARGE, MockMode.DEFER).status());
    }
}
