package com.o2o.payment.domain;

import java.time.Instant;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Mock 이벤트 처리 기록. 설계 근거: 11 INTERNAL-01 규칙 2(eventId를 유일하게 저장한다. 같은
 * eventId와 같은 body의 재전달은 DUPLICATE다. 다른 body면 MOCK_EVENT_CONFLICT다), 규칙 7
 * (이벤트 처리 기록과 시도 결과가 함께 저장된 뒤 200).
 *
 * eventId가 기본키라 같은 eventId는 한 번만 저장된다(계약 2-1절 이벤트 유일). 저장 범위는
 * PROCESSED와 DUPLICATE 둘 다이고 4xx로 거절된 이벤트는 저장하지 않는다(계약 6절 이벤트
 * 기록의 저장 범위 행). 시도 결과와 같은 트랜잭션에서 저장된다(계약 7절 D-2).
 *
 * Payment 애그리거트 밖의 별도 기록인 이유는 규칙 2의 판정이 시도가 아니라 이벤트 단위이고,
 * 예약 묶음의 멱등 기록처럼 요청의 재전달을 가리는 장치이기 때문이다. 06-4 v5 0-3의 수신
 * 원장은 아니다. 그 원장은 처리가 롤백돼도 남는 재처리 입력이고 이 기록은 처리와 같이 남는다.
 */
@Entity
@Table(name = "mock_payment_event")
public class MockPaymentEvent {

    @Id
    @Column(name = "event_id", nullable = false, length = 128)
    private String eventId;

    @Column(name = "payment_attempt_id", nullable = false, length = 64)
    private String paymentAttemptId;

    // 요청 body의 sha256 16진수 64자. 같은 eventId의 재전달이 같은 내용인지 가른다(규칙 2)
    @Column(name = "body_hash", nullable = false, length = 64)
    private String bodyHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "result", nullable = false, length = 16)
    private MockEventResult.Result result;

    // 최초 업무 처리 완료 시각. DUPLICATE 기록도 최초 처리의 시각을 갖는다(11 응답 모델 MockEventResult)
    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    protected MockPaymentEvent() {
    }

    private MockPaymentEvent(String eventId, PaymentAttemptId attemptId, String bodyHash,
                             MockEventResult.Result result, Instant processedAt) {
        this.eventId = Objects.requireNonNull(eventId, "eventId는 null일 수 없다");
        this.paymentAttemptId = attemptId.value();
        this.bodyHash = Objects.requireNonNull(bodyHash, "bodyHash는 null일 수 없다");
        this.result = result;
        this.processedAt = Objects.requireNonNull(processedAt, "processedAt은 null일 수 없다");
    }

    /** 이번 처리로 시도가 전이했다. processedAt은 지금이다 */
    public static MockPaymentEvent processed(String eventId, PaymentAttemptId attemptId,
                                             String bodyHash, Instant now) {
        return new MockPaymentEvent(eventId, attemptId, bodyHash, MockEventResult.Result.PROCESSED, now);
    }

    /** 같은 거래의 같은 결과가 이미 반영돼 있었다(규칙 3). processedAt은 최초 처리의 시각이다 */
    public static MockPaymentEvent duplicate(String eventId, PaymentAttemptId attemptId,
                                             String bodyHash, Instant firstProcessedAt) {
        return new MockPaymentEvent(eventId, attemptId, bodyHash, MockEventResult.Result.DUPLICATE,
                firstProcessedAt);
    }

    /** 규칙 2. 같은 body인지 */
    public boolean hasSameBody(String candidateHash) {
        return bodyHash.equals(candidateHash);
    }

    /** 규칙 2의 재전달 응답. 기록의 시각을 그대로 낸다 */
    public MockEventResult asDuplicate() {
        return MockEventResult.duplicate(eventId, PaymentAttemptId.of(paymentAttemptId), processedAt);
    }

    public String eventId() {
        return eventId;
    }

    public PaymentAttemptId paymentAttemptId() {
        return PaymentAttemptId.of(paymentAttemptId);
    }

    public String bodyHash() {
        return bodyHash;
    }

    public MockEventResult.Result result() {
        return result;
    }

    public Instant processedAt() {
        return processedAt;
    }
}
