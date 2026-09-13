package com.o2o.payment.application;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

import com.o2o.payment.domain.MockMode;
import com.o2o.payment.domain.Payment;
import com.o2o.payment.domain.PaymentApproved;
import com.o2o.payment.domain.PaymentAttempt;
import com.o2o.payment.domain.PaymentFailed;
import com.o2o.payment.domain.PaymentRefunded;
import com.o2o.payment.domain.PaymentRepository;
import com.o2o.payment.domain.PaymentRequested;
import com.o2o.payment.infrastructure.MockAutoResultResumeRunner;
import com.o2o.shared.Money;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Y11과 Y12와 Y13. 설계 근거: 계약 7절 D-1(요청은 트랜잭션 안, 자동 결과는 커밋 뒤 어댑터가
 * REQUIRES_NEW로), layers.md 3-3 E1과 E2(구독자는 AFTER_COMMIT. 첫 구독자가 생기는 묶음은 롤백된
 * 요청의 이벤트가 구독자에 닿지 않는 테스트를 필수로), T26과 11 결제 접수와 환불 절의 재시작
 * 문장, 08-3 결정 6.
 *
 * PaymentEventTest가 발행을 센다면 여기는 커밋 뒤 도착을 센다. 그래서 테스트 트랜잭션으로
 * 감싸지 않고 실제 MySQL에 커밋한다(T3). 도착을 보는 도구는 테스트 전용 구독자다. 운영
 * 구독자(자동 결과 어댑터)와 같은 방식(@TransactionalEventListener)으로 걸어 같은 조건에서
 * 받는다. 예약 2차의 구독자가 이 자리에 선다.
 *
 * 예약 ID를 건마다 새로 만들어 격리한다. 시각은 고정한다.
 */
@SpringBootTest
class PaymentAutoResultTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-10-01T03:00:00Z");
    private static final Money CHARGE = Money.krw(180_000);

    /** 커밋된 사실만 받는 테스트 전용 구독자. 예약 2차의 자리를 흉내 낸다 */
    static class CommittedEventRecorder {

        final List<PaymentRequested> requested = new CopyOnWriteArrayList<>();
        final List<PaymentApproved> approved = new CopyOnWriteArrayList<>();
        final List<PaymentFailed> failed = new CopyOnWriteArrayList<>();
        final List<PaymentRefunded> refunded = new CopyOnWriteArrayList<>();

        @TransactionalEventListener
        public void onRequested(PaymentRequested event) {
            requested.add(event);
        }

        @TransactionalEventListener
        public void onApproved(PaymentApproved event) {
            approved.add(event);
        }

        @TransactionalEventListener
        public void onFailed(PaymentFailed event) {
            failed.add(event);
        }

        @TransactionalEventListener
        public void onRefunded(PaymentRefunded event) {
            refunded.add(event);
        }

        long requestedOf(String attemptId) {
            return requested.stream().filter(e -> e.paymentAttemptId().value().equals(attemptId)).count();
        }

        long approvedOf(String attemptId) {
            return approved.stream().filter(e -> e.paymentAttemptId().value().equals(attemptId)).count();
        }

        long failedOf(String attemptId) {
            return failed.stream().filter(e -> e.paymentAttemptId().value().equals(attemptId)).count();
        }
    }

    @TestConfiguration
    static class AutoResultTestConfiguration {

        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
        }

        @Bean
        CommittedEventRecorder committedEventRecorder() {
            return new CommittedEventRecorder();
        }
    }

    @Autowired
    private PaymentApplicationService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private MockAutoResultResumeRunner resumeRunner;

    @Autowired
    private CommittedEventRecorder recorder;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private static String newBookingId() {
        return "bk_test_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    private PaymentAttemptView attemptOf(String bookingId, String attemptId) {
        return paymentService.attemptsOf(bookingId).attempts().stream()
                .filter(a -> a.id().equals(attemptId))
                .findFirst()
                .orElseThrow();
    }

    /**
     * Y11. APPROVE. 202가 접수 결과이지 승인 결과가 아니라는 것(11 결제 접수와 환불 절)이 여기서
     * 보인다. 돌려받은 뷰는 REQUESTED이고 커밋 뒤에 APPROVED가 된다.
     */
    @Test
    void Y11_APPROVE는_뷰가_REQUESTED이고_커밋_뒤_시도가_APPROVED이며_PaymentApproved가_한_번_닿는다() {
        String bookingId = newBookingId();

        PaymentAttemptView view = paymentService.openAttempt(bookingId, CHARGE, MockMode.APPROVE);

        assertEquals("REQUESTED", view.status());
        assertNull(view.completedAt());
        PaymentAttemptView stored = attemptOf(bookingId, view.id());
        assertEquals("APPROVED", stored.status());
        assertEquals(FIXED_NOW, stored.completedAt());
        assertNull(stored.failureCode());
        assertEquals(view.id(), paymentService.attemptsOf(bookingId).approvedAttemptId());
        assertEquals(1, recorder.requestedOf(view.id()));
        assertEquals(1, recorder.approvedOf(view.id()));
        assertEquals(0, recorder.failedOf(view.id()));
        PaymentApproved payload = recorder.approved.stream()
                .filter(e -> e.paymentAttemptId().value().equals(view.id())).findFirst().orElseThrow();
        assertEquals(bookingId, payload.bookingId());
        assertEquals(1, payload.attemptCount());
        assertEquals(view.pgTransactionId(), payload.pgTransactionId());
    }

    @Test
    void Y11_DECLINE은_커밋_뒤_시도가_FAILED이고_PaymentFailed가_한_번_닿는다() {
        String bookingId = newBookingId();

        PaymentAttemptView view = paymentService.openAttempt(bookingId, CHARGE, MockMode.DECLINE);

        assertEquals("REQUESTED", view.status());
        PaymentAttemptView stored = attemptOf(bookingId, view.id());
        assertEquals("FAILED", stored.status());
        assertEquals(PaymentAttempt.MOCK_DECLINED, stored.failureCode());
        assertEquals(FIXED_NOW, stored.completedAt());
        assertNull(paymentService.attemptsOf(bookingId).approvedAttemptId());
        assertEquals(1, recorder.requestedOf(view.id()));
        assertEquals(1, recorder.failedOf(view.id()));
        assertEquals(0, recorder.approvedOf(view.id()));
        // 실패 뒤에는 2번이 열린다. 자동 결과가 REQUESTED를 풀어 줬다는 뜻이다
        assertEquals(2, paymentService.openAttempt(bookingId, CHARGE, MockMode.DEFER).attemptNumber());
    }

    @Test
    void Y11_DEFER는_REQUESTED로_남고_결과_이벤트가_없다() {
        String bookingId = newBookingId();

        PaymentAttemptView view = paymentService.openAttempt(bookingId, CHARGE, MockMode.DEFER);

        assertEquals("REQUESTED", attemptOf(bookingId, view.id()).status());
        assertEquals(1, recorder.requestedOf(view.id()));
        assertEquals(0, recorder.approvedOf(view.id()));
        assertEquals(0, recorder.failedOf(view.id()));
    }

    /**
     * Y12. E2. 바깥 트랜잭션이 openAttempt를 감싸고 롤백한다. 발행은 트랜잭션 안에서 했지만
     * 커밋이 없으므로 어느 구독자에도 닿지 않고 자동 결과 어댑터도 움직이지 않는다. 시도 행도
     * 없다. 발행을 커밋 뒤로 옮기지 않은 이유가 layers.md 3-3에 있다.
     */
    @Test
    void Y12_롤백된_openAttempt의_PaymentRequested는_어느_구독자에도_닿지_않고_시도가_없다() {
        String bookingId = newBookingId();
        int requestedBefore = recorder.requested.size();
        int approvedBefore = recorder.approved.size();

        PaymentAttemptView view = new TransactionTemplate(transactionManager).execute(status -> {
            PaymentAttemptView opened = paymentService.openAttempt(bookingId, CHARGE, MockMode.APPROVE);
            status.setRollbackOnly();
            return opened;
        });

        assertEquals("REQUESTED", view.status());
        assertTrue(paymentRepository.findByBookingId(bookingId).isEmpty());
        assertEquals(0, paymentService.attemptsOf(bookingId).attemptCount());
        assertEquals(0, recorder.requestedOf(view.id()));
        assertEquals(0, recorder.approvedOf(view.id()));
        assertEquals(requestedBefore, recorder.requested.size());
        assertEquals(approvedBefore, recorder.approved.size());

        // 통과 쪽 짝. 같은 호출을 커밋하면 닿는다
        PaymentAttemptView committed = new TransactionTemplate(transactionManager)
                .execute(status -> paymentService.openAttempt(bookingId, CHARGE, MockMode.APPROVE));
        assertEquals(1, recorder.requestedOf(committed.id()));
        assertEquals(1, recorder.approvedOf(committed.id()));
        assertEquals("APPROVED", attemptOf(bookingId, committed.id()).status());
    }

    /**
     * Y13. T26. 앱이 자동 결과를 넣기 전에 죽은 상태를 흉내 낸다. 그 상태는 이 묶음의 서비스로
     * 만들 수 없다(커밋되면 어댑터가 바로 결과를 넣는다). 그래서 리포지토리로 REQUESTED이고
     * mockMode가 APPROVE인 시도를 직접 심는다(T6). 이벤트를 발행하지 않으니 어댑터는 모른다.
     * 재개 러너가 그 시도를 찾아 결과를 넣고, 두 번째 호출은 후보가 없어 아무것도 바꾸지 않는다.
     * DEFER 시도는 INTERNAL-01을 기다리므로 재개 대상이 아니다.
     */
    @Test
    void Y13_재개_러너는_REQUESTED_APPROVE_시도를_APPROVED로_만들고_다시_불러도_바뀌지_않으며_DEFER는_두지_않는다() {
        String autoBooking = newBookingId();
        String deferBooking = newBookingId();
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        String autoAttemptId = tx.execute(status -> seedRequested(autoBooking, MockMode.APPROVE));
        String deferAttemptId = tx.execute(status -> seedRequested(deferBooking, MockMode.DEFER));
        assertEquals("REQUESTED", attemptOf(autoBooking, autoAttemptId).status());
        assertEquals(0, recorder.approvedOf(autoAttemptId));

        int firstRun = resumeRunner.resumeAutoResults();

        assertEquals(1, firstRun);
        PaymentAttemptView resumed = attemptOf(autoBooking, autoAttemptId);
        assertEquals("APPROVED", resumed.status());
        assertEquals(FIXED_NOW, resumed.completedAt());
        assertEquals(1, recorder.approvedOf(autoAttemptId));
        assertEquals("REQUESTED", attemptOf(deferBooking, deferAttemptId).status());
        assertEquals(0, recorder.approvedOf(deferAttemptId));

        int approvedBefore = recorder.approved.size();
        int secondRun = resumeRunner.resumeAutoResults();

        assertEquals(0, secondRun);
        assertEquals("APPROVED", attemptOf(autoBooking, autoAttemptId).status());
        assertEquals("REQUESTED", attemptOf(deferBooking, deferAttemptId).status());
        assertEquals(approvedBefore, recorder.approved.size());
        assertEquals(1, recorder.approvedOf(autoAttemptId));
    }

    /** T6. 서비스를 거치지 않고 REQUESTED 시도를 심는다. 거래 번호는 Mock PG 대신 손으로 붙인다 */
    private String seedRequested(String bookingId, MockMode mockMode) {
        Payment payment = Payment.open(bookingId, CHARGE, FIXED_NOW);
        PaymentAttempt attempt = payment.openAttempt(CHARGE, mockMode, FIXED_NOW);
        payment.attachPgTransaction(attempt.id(), "mock_tx_seed_" + attempt.id().value());
        paymentRepository.save(payment);
        return attempt.id().value();
    }
}
