package com.o2o.payment.application;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.o2o.payment.domain.MockEventConflictException;
import com.o2o.payment.domain.MockEventResult;
import com.o2o.payment.domain.MockMode;
import com.o2o.payment.domain.MockPaymentEvent;
import com.o2o.payment.domain.MockPaymentEventRepository;
import com.o2o.payment.domain.Payment;
import com.o2o.payment.domain.PaymentApproved;
import com.o2o.payment.domain.PaymentAttempt;
import com.o2o.payment.domain.PaymentAttemptId;
import com.o2o.payment.domain.PaymentFailed;
import com.o2o.payment.domain.PaymentRefunded;
import com.o2o.payment.domain.PaymentRepository;
import com.o2o.payment.domain.PaymentRequested;
import com.o2o.payment.domain.RefundReason;
import com.o2o.payment.domain.UnknownAttemptException;
import com.o2o.shared.Money;

/**
 * 설계 근거: 06-2 6절 결제 CRC(앱 서비스 행은 없고 루트의 네 행이 유스케이스다), 06-4 1-2 결제
 * 계약표의 openAttempt, recordApproval, recordFailure, refund, 06-4 v5 2-6 조회.
 *
 * 하는 일. 트랜잭션 경계를 열고 루트를 잠근 뒤 도메인을 부르고, Mock PG 포트를 같은 트랜잭션에서
 * 부르며(08-3 결정 9와 10), 이벤트를 트랜잭션 안에서 발행하고(layers.md 3-3), INTERNAL-01의 이벤트
 * 기록을 시도 결과와 같은 트랜잭션에 저장한다(11 규칙 7, 계약 7절 D-2).
 *
 * 입구가 셋이다. 예약 2차가 부르는 공개 메서드 셋(openAttempt, refund, attemptsOf), api가 부르는
 * handleMockEvent(INTERNAL-01), 인프라의 자동 결과 어댑터와 재개 러너가 부르는 deliverAutoResult다.
 * 자동 결과와 수동 이벤트가 같은 처리 길을 지나므로 중복 판정이 하나다(계약 7절 D-1).
 *
 * 결제는 예약을 모른다(06-1 R6). bookingId는 문자열이고 예약 리포지토리를 읽지 않는다(11 결제
 * 접수와 환불 절 첫 줄). 시각을 Clock에서 받는 이유는 앞 묶음과 같다. 테스트가 고정 Clock을 끼운다.
 *
 * 격리 수준이 READ COMMITTED인 이유(2026-09-13, Y22에서 드러남). 잠금 뒤에 읽은 값이 최신이어야
 * 규칙 2와 3이 선다(계약 7절 D-2. 잠금이 규칙 2보다 앞). MySQL의 기본 REPEATABLE READ에서는
 * 트랜잭션의 첫 일관 읽기가 스냅샷을 고정하는데, 루트만 잠그는 조회(for update of payment)가 조인한
 * 시도 표를 잠금 없이 읽으면서 잠금을 얻기도 전에 스냅샷이 고정된다. 그러면 잠금이 풀린 뒤 읽는
 * 시도와 이벤트 기록이 앞 트랜잭션의 커밋을 못 보고 같은 이벤트 둘이 둘 다 PROCESSED가 된다.
 * READ COMMITTED는 읽기마다 새 스냅샷이라 잠금 뒤 읽기가 커밋된 최신을 본다. 잠금 순서는 그대로
 * 루트 하나다(08-3 결정 3). 시도 표를 먼저 잠그면 루트를 쥔 채 시도를 고치는 쪽과 교착한다.
 */
@Service
@Transactional(isolation = Isolation.READ_COMMITTED)
public class PaymentApplicationService {

    private final PaymentRepository paymentRepository;
    private final MockPaymentEventRepository eventRepository;
    private final MockPaymentGateway gateway;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public PaymentApplicationService(PaymentRepository paymentRepository,
                                     MockPaymentEventRepository eventRepository,
                                     MockPaymentGateway gateway,
                                     ApplicationEventPublisher eventPublisher,
                                     Clock clock) {
        this.paymentRepository = paymentRepository;
        this.eventRepository = eventRepository;
        this.gateway = gateway;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    /**
     * RequestPayment의 결제 몫. 설계 근거: 06-4 1-2 openAttempt, 계약 2절 openAttempt 검사 순서.
     *
     * 호출자는 예약 2차의 RequestPayment 중계다. 예약이 Booking을 먼저 잠그고 스냅샷 총액을 넘긴다
     * (06-4 v5 0-1). 여기서는 Payment 한 행만 잠근다(08-3 결정 3). 첫 요청이면 Payment를 만들고
     * 청구액이 이 값으로 고정된다(U3). 시도를 REQUESTED로 열고 같은 트랜잭션에서 Mock PG에 요청해
     * 거래 번호를 붙인 뒤 저장하고 PaymentRequested를 발행한다. 돌려주는 뷰는 REQUESTED 스냅샷이다.
     * 202는 접수 결과이지 승인 결과가 아니다(11 결제 접수와 환불 절). APPROVE와 DECLINE의 결과는
     * 커밋 뒤 자동 결과 어댑터가 넣는다(계약 7절 D-1). DEFER는 INTERNAL-01을 기다린다.
     */
    public PaymentAttemptView openAttempt(String bookingId, Money amount, MockMode mockMode) {
        requireBookingId(bookingId);
        Objects.requireNonNull(amount, "amount는 null일 수 없다");
        Objects.requireNonNull(mockMode, "mockMode는 null일 수 없다");
        if (amount.amount() <= 0) {
            throw new IllegalArgumentException("amount는 양수여야 한다: " + amount.amount());
        }
        Instant now = Instant.now(clock);

        Payment payment = paymentRepository.findByBookingIdForUpdate(bookingId)
                .orElseGet(() -> Payment.open(bookingId, amount, now));
        PaymentAttempt attempt = payment.openAttempt(amount, mockMode, now);

        // 08-3 결정 9. 요청은 같은 트랜잭션 안. attemptId가 멱등키다(06-4 v5 1-2 openAttempt Post)
        String pgTransactionId = gateway.request(attempt.id(), attempt.amount());
        payment.attachPgTransaction(attempt.id(), pgTransactionId);

        Payment saved = paymentRepository.save(payment);
        PaymentAttempt savedAttempt = saved.attempt(attempt.id())
                .orElseThrow(() -> new IllegalStateException("저장한 시도가 없다: " + attempt.id().value()));
        eventPublisher.publishEvent(PaymentRequested.of(saved, savedAttempt));
        return PaymentAttemptView.of(saved.bookingId(), savedAttempt);
    }

    /**
     * RefundPayment. 설계 근거: 06-4 1-2 refund, 계약 2절 refund 검사 순서, 08-3 결정 4와 10.
     *
     * 호출자는 예약 2차다. reason은 호출자가 정한다. APPROVED면 REFUNDED로 바꾸고 attemptId를
     * 멱등키로 Mock 환불을 같은 트랜잭션에서 부르고 PaymentRefunded를 발행한다. 이미 REFUNDED면
     * 상태 변경과 PG 호출과 발행 없이 기존 환불을 돌려준다(06-4 0절 종착 재호출 무해).
     */
    public RefundView refund(PaymentAttemptId attemptId, RefundReason reason) {
        Objects.requireNonNull(attemptId, "attemptId는 null일 수 없다");
        Objects.requireNonNull(reason, "reason은 null일 수 없다");
        Instant now = Instant.now(clock);

        Payment payment = paymentRepository.findByAttemptIdForUpdate(attemptId)
                .orElseThrow(() -> new UnknownAttemptException(attemptId));
        boolean transitioned = payment.refund(attemptId, reason, now);
        PaymentAttempt attempt = attemptOf(payment, attemptId);
        if (transitioned) {
            gateway.refund(attemptId, attempt.amount());
            paymentRepository.save(payment);
            eventPublisher.publishEvent(PaymentRefunded.of(payment, attempt));
        }
        return RefundView.of(attempt);
    }

    /**
     * 예약의 결제 조회. 설계 근거: 06-4 v5 2-6(조회는 결제가 제공하고 예약이 부른다), 11 PAY-02
     * 처리 규칙과 응답 모델 PaymentSummary. 잠그지 않는다. Payment가 없는 예약은 빈 요약이다.
     */
    @Transactional(readOnly = true)
    public PaymentSummaryView attemptsOf(String bookingId) {
        requireBookingId(bookingId);
        return paymentRepository.findByBookingId(bookingId)
                .map(PaymentSummaryView::of)
                .orElseGet(PaymentSummaryView::empty);
    }

    /**
     * INTERNAL-01. 설계 근거: 11 INTERNAL-01 중복과 결과 처리 규칙 1부터 8, 계약 2절 INTERNAL-01
     * 검사 순서 3행부터 13행. 형식(1행)과 행위자(2행)는 api가 앞에서 본다.
     *
     * 한 트랜잭션이다(계약 7절 D-2). 3부터 9에서 거절되면 아무것도 저장하지 않는다. 예외로 롤백되면
     * 이벤트 기록도 남지 않아 재전달이 PROCESSED가 된다(11 규칙 7). 예약 정책(규칙 5와 6의 예약
     * 부분)은 예약 2차의 AFTER_COMMIT 구독자가 별도 트랜잭션에서 한다(08-3 결정 6).
     */
    public MockEventResult handleMockEvent(MockEventCommand command) {
        Objects.requireNonNull(command, "command는 null일 수 없다");
        return process(command, Instant.now(clock));
    }

    /**
     * 자동 결과 전달. 설계 근거: 계약 7절 D-1, 11 결제 접수와 환불 절(자동 결과는 시도 저장 후
     * 전달하고, 저장된 REQUESTED와 mockMode로 재시작 후에도 재개한다. 중복 전달은 같은 업무를
     * 반복하지 않는다), 08-3 결정 6의 REQUIRES_NEW.
     *
     * 호출자는 커밋 뒤의 자동 결과 어댑터와 재시작 재개 러너다. AFTER_COMMIT 단계에서는 원래
     * 트랜잭션의 자원이 아직 묶여 있어 새 트랜잭션을 열지 않으면 변경이 커밋되지 않으므로
     * REQUIRES_NEW다. 자기 호출은 프록시를 타지 않으므로 호출자가 다른 빈이어야 한다(06-4 v5 0-3).
     * 결과는 INTERNAL-01과 같은 처리 길로 넣는다. eventId는 auto_ 뒤에 attemptId라 두 번째
     * 전달은 규칙 2나 3이 DUPLICATE로 막는다(T26). DEFER 시도는 아무것도 하지 않는다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)
    public Optional<MockEventResult> deliverAutoResult(PaymentAttemptId attemptId) {
        Objects.requireNonNull(attemptId, "attemptId는 null일 수 없다");
        Payment payment = paymentRepository.findByAttemptIdForUpdate(attemptId)
                .orElseThrow(() -> new UnknownAttemptException(attemptId));
        PaymentAttempt attempt = attemptOf(payment, attemptId);
        if (!attempt.mockMode().isAutomatic()) {
            return Optional.empty();
        }
        return Optional.of(process(MockEventCommand.autoResult(attempt), Instant.now(clock)));
    }

    /**
     * 재시작 재개의 후보(T26). REQUESTED이고 mockMode가 자동인 NORMAL 시도. 러너가 건마다
     * deliverAutoResult를 부른다. 잠금은 그 건별 호출이 건다.
     */
    @Transactional(readOnly = true)
    public List<PaymentAttemptId> findAutoAttemptsToResume() {
        return paymentRepository.findAutoAttemptIdsToResume();
    }

    /** 계약 2절 INTERNAL-01 표의 3행부터 13행. 자동 결과와 수동 이벤트가 같이 지난다 */
    private MockEventResult process(MockEventCommand command, Instant now) {
        PaymentAttemptId attemptId = PaymentAttemptId.of(command.paymentAttemptId());

        // 3과 4. NORMAL 시도 존재(규칙 1의 404)와 그 시도를 가진 Payment 잠금(06-2 5절)
        Payment payment = paymentRepository.findByAttemptIdForUpdate(attemptId)
                .orElseThrow(() -> new UnknownAttemptException(attemptId));

        // 5와 6. 거래 번호와 금액과 통화(규칙 1의 409 둘)
        payment.assertCallbackMatches(attemptId, command.pgTransactionId(), command.amount(),
                command.currency());

        // 7. eventId 기록(규칙 2). 같은 body면 재전달, 다른 body면 충돌.
        // 잠금 뒤에 보는 이유는 같은 이벤트 둘이 같은 순간 와도 둘째가 첫째의 커밋을 본 뒤
        // 답하게 하려는 것이다(계약 7절 D-2, Y22)
        String bodyHash = command.bodyHash();
        Optional<MockPaymentEvent> recorded = eventRepository.findByEventId(command.eventId());
        if (recorded.isPresent()) {
            if (recorded.get().hasSameBody(bodyHash)) {
                return recorded.get().asDuplicate();
            }
            throw new MockEventConflictException(command.eventId());
        }

        // 8, 9, 10. 같은 결과면 전이 없음(규칙 3), 반대 결과면 예외(규칙 4), 아니면 전이(규칙 6)
        boolean transitioned = switch (command.outcome()) {
            case APPROVED -> payment.recordApproval(attemptId, command.pgTransactionId(), now);
            case FAILED -> payment.recordFailure(attemptId, command.pgTransactionId(),
                    command.failureCode(), now);
        };
        PaymentAttempt attempt = attemptOf(payment, attemptId);

        if (!transitioned) {
            // 규칙 3과 8. 새 이벤트를 다시 발행하지 않는다. 기록은 남겨 같은 eventId 재전달이
            // 규칙 2로 답하게 한다. processedAt은 최초 처리의 시각이다(11 응답 모델 MockEventResult)
            Instant firstProcessedAt = attempt.completedAt();
            eventRepository.save(MockPaymentEvent.duplicate(command.eventId(), attemptId, bodyHash,
                    firstProcessedAt));
            return MockEventResult.duplicate(command.eventId(), attemptId, firstProcessedAt);
        }

        // 11. 발행은 트랜잭션 안(layers.md 3-3). 커밋 뒤 전달은 구독자가 지킨다
        paymentRepository.save(payment);
        switch (command.outcome()) {
            case APPROVED -> eventPublisher.publishEvent(PaymentApproved.of(payment, attempt));
            case FAILED -> eventPublisher.publishEvent(PaymentFailed.of(payment, attempt));
        }

        // 12와 13. 이벤트 기록을 시도 결과와 같은 트랜잭션에 저장하고 커밋한다(규칙 7의 결제 부분)
        eventRepository.save(MockPaymentEvent.processed(command.eventId(), attemptId, bodyHash, now));
        return MockEventResult.processed(command.eventId(), attemptId, now);
    }

    private static PaymentAttempt attemptOf(Payment payment, PaymentAttemptId attemptId) {
        return payment.attempt(attemptId)
                .orElseThrow(() -> new UnknownAttemptException(attemptId));
    }

    private static void requireBookingId(String bookingId) {
        Objects.requireNonNull(bookingId, "bookingId는 null일 수 없다");
        if (bookingId.isBlank() || bookingId.length() > 64) {
            throw new IllegalArgumentException("bookingId는 1자 이상 64자 이하여야 한다: " + bookingId);
        }
    }
}
