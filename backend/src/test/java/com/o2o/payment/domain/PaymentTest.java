package com.o2o.payment.domain;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.o2o.shared.Money;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Y1부터 Y7. 설계 근거: I6과 I7과 I9(06-2 3-1, 06-4 v5 1-1), U4(06-2 3-4), 06-4 1-2 결제 계약표
 * 네 행, 06-4 1-3 결제 시도 전이표, 06-4 0절 종착 재호출 무해.
 *
 * 스프링을 띄우지 않는다. 불변식과 전이 폐쇄는 애그리거트가 스스로 지키는 규칙이라 컨테이너도
 * DB도 필요 없다(06-4 1-4). 잠금과 유일성과 롤백은 PaymentApplicationServiceTest와
 * PaymentEventTest가 실제 MySQL에서 본다(T3).
 *
 * 실패 케이스와 통과 케이스를 짝으로 붙인다(T1). 경계는 양쪽을 본다(T2). 넷째 시도 거절이면
 * 셋째 시도 통과도 본다. 거래 번호는 앱 서비스가 Mock PG에서 받아 붙이는 것이라 여기서는
 * 손으로 붙인다. 시각은 상수다. 오늘에서 세는 값이 없어서다(T4의 예외).
 */
class PaymentTest {

    private static final Instant NOW = Instant.parse("2026-10-01T00:00:00Z");
    private static final Instant LATER = NOW.plusSeconds(60);
    private static final String BOOKING = "bk_test_0001";
    private static final Money CHARGE = Money.krw(180_000);

    private static Payment payment() {
        return Payment.open(BOOKING, CHARGE, NOW);
    }

    /** 시도를 열고 거래 번호를 붙인다. 앱 서비스가 Mock PG로 하는 두 걸음이다 */
    private static PaymentAttempt requested(Payment payment, MockMode mode) {
        PaymentAttempt attempt = payment.openAttempt(CHARGE, mode, NOW);
        payment.attachPgTransaction(attempt.id(), "mock_tx_" + attempt.attemptNumber());
        return attempt;
    }

    private static PaymentAttempt failed(Payment payment) {
        PaymentAttempt attempt = requested(payment, MockMode.DEFER);
        assertTrue(payment.recordFailure(attempt.id(), attempt.pgTransactionId(),
                PaymentAttempt.MOCK_DECLINED, NOW));
        return attempt;
    }

    private static PaymentAttempt approved(Payment payment) {
        PaymentAttempt attempt = requested(payment, MockMode.DEFER);
        assertTrue(payment.recordApproval(attempt.id(), attempt.pgTransactionId(), NOW));
        return attempt;
    }

    @Test
    void Y1_첫_openAttempt는_Payment를_만들고_1번_REQUESTED_시도를_더한다() {
        Payment payment = payment();

        PaymentAttempt attempt = requested(payment, MockMode.APPROVE);

        assertEquals(1, attempt.attemptNumber());
        assertEquals(PaymentAttemptStatus.REQUESTED, attempt.status());
        assertEquals(AttemptKind.NORMAL, attempt.kind());
        assertEquals(CHARGE, attempt.amount());
        assertEquals(MockMode.APPROVE, attempt.mockMode());
        assertEquals(NOW, attempt.requestedAt());
        assertNull(attempt.completedAt());
        assertEquals(1, payment.attemptCount());
        assertEquals(BOOKING, payment.bookingId());
        assertEquals(CHARGE, payment.amount());
    }

    @Test
    void Y1_REQUESTED가_있는_동안_둘째_openAttempt는_AttemptInProgress다() {
        // I9. 06-4 1-2 openAttempt Pre의 첫 검사. T15와 T16의 결제 몫
        Payment payment = payment();
        requested(payment, MockMode.DEFER);

        assertThrows(AttemptInProgressException.class,
                () -> payment.openAttempt(CHARGE, MockMode.DEFER, LATER));
        assertEquals(1, payment.attemptCount());
    }

    @Test
    void Y1_첫_시도가_FAILED가_된_뒤에는_2번이_열린다() {
        // 06-2 7절. FAILED 후 재시도는 새 PaymentAttempt 생성이다. 역행이 아니다
        Payment payment = payment();
        PaymentAttempt first = failed(payment);

        PaymentAttempt second = payment.openAttempt(CHARGE, MockMode.DEFER, LATER);

        assertEquals(2, second.attemptNumber());
        assertEquals(PaymentAttemptStatus.FAILED, first.status());
        assertEquals(PaymentAttemptStatus.REQUESTED, second.status());
        assertEquals(2, payment.attemptCount());
    }

    @Test
    void Y2_셋째_시도는_열리고_넷째는_AttemptLimitExceeded다() {
        // I6. 경계 양쪽. T17의 결제 몫은 넷째 거절이다
        Payment payment = payment();
        failed(payment);
        failed(payment);

        PaymentAttempt third = requested(payment, MockMode.DEFER);
        assertEquals(3, third.attemptNumber());
        assertTrue(payment.recordFailure(third.id(), third.pgTransactionId(),
                PaymentAttempt.MOCK_DECLINED, NOW));

        assertThrows(AttemptLimitExceededException.class,
                () -> payment.openAttempt(CHARGE, MockMode.DEFER, LATER));
        assertEquals(3, payment.attemptCount());
    }

    @Test
    void Y3_APPROVED_시도가_있으면_openAttempt는_AlreadyApproved다() {
        // I7. 06-4 1-2 openAttempt Pre의 승인 이력 없음
        Payment payment = payment();
        PaymentAttempt attempt = approved(payment);

        AlreadyApprovedException e = assertThrows(AlreadyApprovedException.class,
                () -> payment.openAttempt(CHARGE, MockMode.DEFER, LATER));
        assertEquals(attempt.id(), e.approvedAttemptId());
    }

    @Test
    void Y3_REFUNDED_뒤에도_openAttempt는_AlreadyApproved다() {
        // I7의 승인 이력은 APPROVED 또는 그로부터 전이된 REFUNDED다(06-2 3-1 I7)
        Payment payment = payment();
        PaymentAttempt attempt = approved(payment);
        assertTrue(payment.refund(attempt.id(), RefundReason.BOOKING_CANCELED, LATER));

        assertThrows(AlreadyApprovedException.class,
                () -> payment.openAttempt(CHARGE, MockMode.DEFER, LATER));
        assertEquals(1, payment.attemptCount());
    }

    @Test
    void Y4_둘째_시도의_amount가_청구액과_다르면_AmountMismatch이고_같으면_열린다() {
        // 06-4 1-2 openAttempt Pre의 전달 총액과 청구액 일치. 청구액은 첫 요청이 정한다(R5 결제 몫)
        Payment payment = payment();
        failed(payment);

        assertThrows(AmountMismatchException.class,
                () -> payment.openAttempt(Money.krw(170_000), MockMode.DEFER, LATER));
        assertEquals(1, payment.attemptCount());

        PaymentAttempt second = payment.openAttempt(CHARGE, MockMode.DEFER, LATER);
        assertEquals(2, second.attemptNumber());
        assertEquals(CHARGE, payment.amount());
    }

    @Test
    void Y4_청구액이_0_이하면_Payment를_만들지_않는다() {
        assertThrows(IllegalArgumentException.class, () -> Payment.open(BOOKING, Money.krw(0), NOW));
        assertEquals(CHARGE, Payment.open(BOOKING, CHARGE, NOW).amount());
    }

    @Test
    void Y5_recordApproval은_REQUESTED를_APPROVED로_바꾸고_completedAt을_적는다() {
        // 06-4 1-3 REQUESTED에서 APPROVED. failureCode는 null이다
        Payment payment = payment();
        PaymentAttempt attempt = requested(payment, MockMode.DEFER);

        assertTrue(payment.recordApproval(attempt.id(), attempt.pgTransactionId(), LATER));

        assertEquals(PaymentAttemptStatus.APPROVED, attempt.status());
        assertEquals(LATER, attempt.completedAt());
        assertNull(attempt.failureCode());
        assertEquals(attempt.id(), payment.approvedAttempt().orElseThrow().id());
    }

    @Test
    void Y5_recordFailure는_REQUESTED를_FAILED로_바꾸고_MOCK_DECLINED를_적는다() {
        Payment payment = payment();
        PaymentAttempt attempt = requested(payment, MockMode.DEFER);

        assertTrue(payment.recordFailure(attempt.id(), attempt.pgTransactionId(),
                PaymentAttempt.MOCK_DECLINED, LATER));

        assertEquals(PaymentAttemptStatus.FAILED, attempt.status());
        assertEquals(LATER, attempt.completedAt());
        assertEquals(PaymentAttempt.MOCK_DECLINED, attempt.failureCode());
        assertTrue(payment.approvedAttempt().isEmpty());
    }

    @Test
    void Y5_FAILED에_승인이_오면_역행이라_예외다() {
        // 11 INTERNAL-01 규칙 4. 06-4 1-3 금지 전이. 같은 거래의 반대 결과는 상태를 바꾸지 않는다
        Payment payment = payment();
        PaymentAttempt attempt = failed(payment);

        assertThrows(InvalidAttemptTransitionException.class,
                () -> payment.recordApproval(attempt.id(), attempt.pgTransactionId(), LATER));
        assertEquals(PaymentAttemptStatus.FAILED, attempt.status());
    }

    @Test
    void Y5_APPROVED에_실패가_오면_역행이라_예외다() {
        Payment payment = payment();
        PaymentAttempt attempt = approved(payment);

        assertThrows(InvalidAttemptTransitionException.class,
                () -> payment.recordFailure(attempt.id(), attempt.pgTransactionId(),
                        PaymentAttempt.MOCK_DECLINED, LATER));
        assertEquals(PaymentAttemptStatus.APPROVED, attempt.status());
        assertEquals(NOW, attempt.completedAt());
    }

    @Test
    void Y5_REFUNDED에_실패가_오면_역행이라_예외다() {
        Payment payment = payment();
        PaymentAttempt attempt = approved(payment);
        assertTrue(payment.refund(attempt.id(), RefundReason.LATE_APPROVAL, LATER));

        assertThrows(InvalidAttemptTransitionException.class,
                () -> payment.recordFailure(attempt.id(), attempt.pgTransactionId(),
                        PaymentAttempt.MOCK_DECLINED, LATER));
        assertEquals(PaymentAttemptStatus.REFUNDED, attempt.status());
    }

    @Test
    void Y6_같은_거래_같은_결과의_두_번째_기록은_전이_없음이고_시도가_그대로다() {
        // U4. 06-4 0절 종착 재호출 무해의 같은 거래번호 콜백 자리. 승인과 실패 둘 다
        Payment payment = payment();
        PaymentAttempt attempt = approved(payment);

        assertFalse(payment.recordApproval(attempt.id(), attempt.pgTransactionId(), LATER));
        assertEquals(PaymentAttemptStatus.APPROVED, attempt.status());
        assertEquals(NOW, attempt.completedAt());

        PaymentAttempt failedAttempt = failed(Payment.open("bk_test_0002", CHARGE, NOW));
        Payment other = Payment.open("bk_test_0003", CHARGE, NOW);
        PaymentAttempt otherFailed = failed(other);
        assertFalse(other.recordFailure(otherFailed.id(), otherFailed.pgTransactionId(),
                PaymentAttempt.MOCK_DECLINED, LATER));
        assertEquals(NOW, otherFailed.completedAt());
        assertEquals(PaymentAttemptStatus.FAILED, failedAttempt.status());
    }

    @Test
    void Y6_REFUNDED_뒤_같은_승인이_다시_와도_전이_없음이다() {
        // 11 INTERNAL-01 규칙 8. 환불된 시도에 같은 승인 이벤트가 다시 와도 추가 환불하지 않는다
        Payment payment = payment();
        PaymentAttempt attempt = approved(payment);
        assertTrue(payment.refund(attempt.id(), RefundReason.BOOKING_CANCELED, LATER));

        assertFalse(payment.recordApproval(attempt.id(), attempt.pgTransactionId(), LATER));
        assertEquals(PaymentAttemptStatus.REFUNDED, attempt.status());
        assertEquals(LATER, attempt.refundedAt());
    }

    @Test
    void Y6_다른_pgTransactionId는_PgTransactionMismatch다() {
        // 11 INTERNAL-01 규칙 1. Mock에서 다른 거래 번호는 있을 수 없는 콜백이라 거절한다(계약 7절 D-4)
        Payment payment = payment();
        PaymentAttempt attempt = requested(payment, MockMode.DEFER);

        assertThrows(PgTransactionMismatchException.class,
                () -> payment.assertCallbackMatches(attempt.id(), "mock_tx_other", 180_000, "KRW"));
        assertThrows(PgTransactionMismatchException.class,
                () -> payment.recordApproval(attempt.id(), "mock_tx_other", LATER));
        assertEquals(PaymentAttemptStatus.REQUESTED, attempt.status());

        payment.assertCallbackMatches(attempt.id(), attempt.pgTransactionId(), 180_000, "KRW");
    }

    @Test
    void Y6_금액이나_통화가_다르면_AmountMismatch이고_없는_시도는_UnknownAttempt다() {
        // 11 INTERNAL-01 규칙 1. 금액은 Money가 아니라 long으로 대조한다(계약 2절 표 6행)
        Payment payment = payment();
        PaymentAttempt attempt = requested(payment, MockMode.DEFER);
        String tx = attempt.pgTransactionId();

        assertThrows(AmountMismatchException.class,
                () -> payment.assertCallbackMatches(attempt.id(), tx, 170_000, "KRW"));
        assertThrows(AmountMismatchException.class,
                () -> payment.assertCallbackMatches(attempt.id(), tx, 180_000, "USD"));
        assertThrows(UnknownAttemptException.class,
                () -> payment.assertCallbackMatches(PaymentAttemptId.of("attempt_none"), tx, 180_000, "KRW"));
        assertThrows(UnknownAttemptException.class,
                () -> payment.recordApproval(PaymentAttemptId.of("attempt_none"), tx, LATER));
    }

    @Test
    void Y7_refund는_APPROVED를_REFUNDED로_바꾸고_reason과_refundedAt을_적는다() {
        // 06-4 1-2 refund Post. 환불은 별도 객체가 아니라 시도의 상태다(계약 7절 D-5)
        Payment payment = payment();
        PaymentAttempt attempt = approved(payment);

        assertTrue(payment.refund(attempt.id(), RefundReason.BOOKING_CANCELED, LATER));

        assertEquals(PaymentAttemptStatus.REFUNDED, attempt.status());
        assertEquals(RefundReason.BOOKING_CANCELED, attempt.refundReason());
        assertEquals(LATER, attempt.refundedAt());
        // approvedAttemptId는 환불 뒤에도 그 시도다(11 응답 모델 PaymentSummary)
        assertEquals(attempt.id(), payment.approvedAttempt().orElseThrow().id());
        assertEquals("refund_" + attempt.id().value().substring("attempt_".length()),
                attempt.id().refundId());
    }

    @Test
    void Y7_두_번째_refund는_상태와_refundedAt을_바꾸지_않는다() {
        // 06-4 0절 종착 재호출 무해의 환불 자리. 같은 승인 시도에 환불은 하나다
        Payment payment = payment();
        PaymentAttempt attempt = approved(payment);
        assertTrue(payment.refund(attempt.id(), RefundReason.BOOKING_CANCELED, LATER));

        assertFalse(payment.refund(attempt.id(), RefundReason.LATE_APPROVAL, LATER.plusSeconds(60)));

        assertEquals(RefundReason.BOOKING_CANCELED, attempt.refundReason());
        assertEquals(LATER, attempt.refundedAt());
    }

    @Test
    void Y7_REQUESTED와_FAILED에_refund는_NoApprovedAttempt다() {
        Payment payment = payment();
        PaymentAttempt first = failed(payment);
        PaymentAttempt second = requested(payment, MockMode.DEFER);

        assertThrows(NoApprovedAttemptException.class,
                () -> payment.refund(first.id(), RefundReason.BOOKING_CANCELED, LATER));
        assertThrows(NoApprovedAttemptException.class,
                () -> payment.refund(second.id(), RefundReason.BOOKING_CANCELED, LATER));
        assertEquals(PaymentAttemptStatus.FAILED, first.status());
        assertEquals(PaymentAttemptStatus.REQUESTED, second.status());
    }

    @Test
    void 시도_목록은_attemptNumber_오름차순이고_거래_번호는_한_번만_붙는다() {
        Payment payment = payment();
        PaymentAttempt first = failed(payment);
        PaymentAttempt second = requested(payment, MockMode.DEFER);

        List<PaymentAttempt> attempts = payment.normalAttempts();
        assertEquals(List.of(first.id(), second.id()), attempts.stream().map(PaymentAttempt::id).toList());
        assertThrows(IllegalStateException.class,
                () -> payment.attachPgTransaction(second.id(), "mock_tx_again"));
        assertEquals("mock_tx_2", second.pgTransactionId());
    }
}
