package com.o2o.payment.application;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.annotation.Transactional;

import com.o2o.payment.domain.AttemptInProgressException;
import com.o2o.payment.domain.AttemptKind;
import com.o2o.payment.domain.MockEventConflictException;
import com.o2o.payment.domain.MockEventResult;
import com.o2o.payment.domain.MockMode;
import com.o2o.payment.domain.MockOutcome;
import com.o2o.payment.domain.PaymentApproved;
import com.o2o.payment.domain.PaymentAttempt;
import com.o2o.payment.domain.PaymentAttemptId;
import com.o2o.payment.domain.PaymentFailed;
import com.o2o.payment.domain.PaymentRefunded;
import com.o2o.payment.domain.PaymentRequested;
import com.o2o.payment.domain.RefundReason;
import com.o2o.shared.Money;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Y8. 설계 근거: 계약 6절 이벤트 페이로드 행, 06-4 1-2 계약표의 Post 열(openAttempt는
 * PaymentRequested, recordApproval은 PaymentApproved, recordFailure는 PaymentFailed, refund는
 * PaymentRefunded), 06-1 R6(attemptCount는 이벤트에 실려 온다), T17의 결제 몫.
 *
 * 앞 묶음 V12와 같은 도구다. 발행 자체를 스프링 테스트의 ApplicationEvents로 센다. 테스트
 * 트랜잭션 안이라 커밋이 없고, 그래서 자동 결과 어댑터는 오지 않는다. 결과는 DEFER 시도에
 * handleMockEvent로 직접 넣는다. 커밋 뒤 구독은 PaymentAutoResultTest가 본다.
 *
 * 실패 쪽 짝. 거절된 요청과 중복 이벤트와 두 번째 환불은 이벤트를 내지 않는다. 사실이 아닌
 * 것을 알리면 예약 2차가 그 이벤트로 상태를 바꾼다.
 */
@SpringBootTest
@Transactional
@RecordApplicationEvents
class PaymentEventTest {

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
    private ApplicationEvents events;

    private static String newBookingId() {
        return "bk_test_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    private static MockEventCommand event(String eventId, PaymentAttemptView attempt, MockOutcome outcome) {
        String failureCode = outcome == MockOutcome.FAILED ? PaymentAttempt.MOCK_DECLINED : null;
        return new MockEventCommand(eventId, attempt.id(), attempt.pgTransactionId(), outcome,
                attempt.amount(), attempt.currency(), failureCode);
    }

    @Test
    void Y8_openAttempt는_PaymentRequested를_내고_거절된_요청은_내지_않는다() {
        String bookingId = newBookingId();

        PaymentAttemptView attempt = paymentService.openAttempt(bookingId, CHARGE, MockMode.DEFER);
        assertThrows(AttemptInProgressException.class,
                () -> paymentService.openAttempt(bookingId, CHARGE, MockMode.DEFER));

        List<PaymentRequested> requested = events.stream(PaymentRequested.class).toList();
        assertEquals(1, requested.size());
        PaymentRequested payload = requested.get(0);
        assertNotNull(payload.paymentId());
        assertEquals(bookingId, payload.bookingId());
        assertEquals(attempt.id(), payload.paymentAttemptId().value());
        assertEquals(CHARGE, payload.amount());
        assertEquals(MockMode.DEFER, payload.mockMode());
        assertEquals(FIXED_NOW, payload.occurredAt());
    }

    /** 06-1 R6. 셋째 실패의 attemptCount가 3이다. 예약 2차는 이 수로 T17의 넷째 거절을 판단한다 */
    @Test
    void Y8_PaymentFailed의_attemptCount는_그_시점의_NORMAL_시도_수이고_셋째_실패는_3이다() {
        String bookingId = newBookingId();
        for (int i = 1; i <= 3; i++) {
            PaymentAttemptView attempt = paymentService.openAttempt(bookingId, CHARGE, MockMode.DEFER);
            MockEventResult result = paymentService.handleMockEvent(
                    event("ev_" + i + "_" + bookingId, attempt, MockOutcome.FAILED));
            assertEquals(MockEventResult.Result.PROCESSED, result.result());
        }

        List<PaymentFailed> failed = events.stream(PaymentFailed.class).toList();
        assertEquals(List.of(1, 2, 3), failed.stream().map(PaymentFailed::attemptCount).toList());
        PaymentFailed third = failed.get(2);
        assertEquals(bookingId, third.bookingId());
        assertEquals(PaymentAttempt.MOCK_DECLINED, third.failureCode());
        assertEquals(CHARGE, third.amount());
        assertTrue(third.pgTransactionId().startsWith("mock_tx_"), third.pgTransactionId());
        assertEquals(FIXED_NOW, third.occurredAt());
        assertEquals(3, events.stream(PaymentRequested.class).count());
        assertEquals(0, events.stream(PaymentApproved.class).count());
    }

    @Test
    void Y8_PaymentApproved는_거래_번호와_금액과_그_시점의_attemptCount를_싣고_중복_이벤트는_다시_내지_않는다() {
        String bookingId = newBookingId();
        PaymentAttemptView first = paymentService.openAttempt(bookingId, CHARGE, MockMode.DEFER);
        paymentService.handleMockEvent(event("ev_1_" + bookingId, first, MockOutcome.FAILED));
        PaymentAttemptView second = paymentService.openAttempt(bookingId, CHARGE, MockMode.DEFER);
        MockEventCommand approval = event("ev_2_" + bookingId, second, MockOutcome.APPROVED);

        MockEventResult processed = paymentService.handleMockEvent(approval);
        // 규칙 2. 같은 eventId 같은 body는 DUPLICATE. 규칙 3. 다른 eventId 같은 거래 같은 결과도 DUPLICATE
        MockEventResult sameEvent = paymentService.handleMockEvent(approval);
        MockEventResult sameTransaction = paymentService.handleMockEvent(
                event("ev_3_" + bookingId, second, MockOutcome.APPROVED));
        // 규칙 2. 같은 eventId 다른 body는 충돌
        assertThrows(MockEventConflictException.class, () -> paymentService.handleMockEvent(
                new MockEventCommand(approval.eventId(), second.id(), second.pgTransactionId(),
                        MockOutcome.FAILED, second.amount(), second.currency(), PaymentAttempt.MOCK_DECLINED)));

        assertEquals(MockEventResult.Result.PROCESSED, processed.result());
        assertEquals(MockEventResult.Result.DUPLICATE, sameEvent.result());
        assertEquals(processed.processedAt(), sameEvent.processedAt());
        assertEquals(MockEventResult.Result.DUPLICATE, sameTransaction.result());
        assertEquals(second.id(), sameTransaction.paymentAttemptId().value());

        List<PaymentApproved> approved = events.stream(PaymentApproved.class).toList();
        assertEquals(1, approved.size());
        PaymentApproved payload = approved.get(0);
        assertEquals(bookingId, payload.bookingId());
        assertEquals(second.id(), payload.paymentAttemptId().value());
        assertEquals(second.pgTransactionId(), payload.pgTransactionId());
        assertEquals(CHARGE, payload.amount());
        assertEquals(2, payload.attemptCount());
        assertEquals(FIXED_NOW, payload.occurredAt());
        assertEquals(1, events.stream(PaymentFailed.class).count());
    }

    @Test
    void Y8_PaymentRefunded는_kind와_reason을_싣고_두_번째_환불은_내지_않는다() {
        String bookingId = newBookingId();
        PaymentAttemptView attempt = paymentService.openAttempt(bookingId, CHARGE, MockMode.DEFER);
        paymentService.handleMockEvent(event("ev_" + bookingId, attempt, MockOutcome.APPROVED));
        PaymentAttemptId attemptId = PaymentAttemptId.of(attempt.id());

        paymentService.refund(attemptId, RefundReason.LATE_APPROVAL);
        paymentService.refund(attemptId, RefundReason.BOOKING_CANCELED);

        List<PaymentRefunded> refunded = events.stream(PaymentRefunded.class).toList();
        assertEquals(1, refunded.size());
        PaymentRefunded payload = refunded.get(0);
        assertEquals(bookingId, payload.bookingId());
        assertEquals(attempt.id(), payload.paymentAttemptId().value());
        assertEquals(AttemptKind.NORMAL, payload.kind());
        assertEquals(CHARGE, payload.amount());
        assertEquals(RefundReason.LATE_APPROVAL, payload.reason());
        assertEquals(FIXED_NOW, payload.occurredAt());
        assertNull(paymentService.attemptsOf(bookingId).attempts().get(0).failureCode());
    }
}
