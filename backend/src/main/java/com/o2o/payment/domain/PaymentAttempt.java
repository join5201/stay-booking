package com.o2o.payment.domain;

import java.time.Instant;
import java.util.Objects;

import com.o2o.shared.Money;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * 결제 시도 엔티티. 설계 근거: 06-2 1절 Payment 행의 내부 요소(PaymentAttempt 엔티티 목록),
 * 06-2 6절 결제 CRC의 PaymentAttempt 두 행.
 *
 * CRC 첫 행. 순번, 상태, PG 거래 번호, 요청 시각을 알고 루트를 통해서만 접근된다. 그래서
 * 상태를 바꾸는 메서드가 전부 패키지 안에서만 보이고 Payment만 부른다.
 * CRC 둘째 행. REQUESTED에서 APPROVED 또는 FAILED로, APPROVED에서 REFUNDED로만 전이한다.
 * 역행하지 않으며 재시도는 새 시도 생성이다(06-4 1-3, 06-2 7절).
 *
 * 환불이 이 엔티티의 상태인 근거는 계약 7절 D-5다. 11 응답 모델 Refund는 REFUNDED 시도에서
 * 투영한다. pg_transaction_id의 단일 컬럼 유니크는 U4의 강제 수단이다(06-4 v5 1-1 U4,
 * 08-3 결정 2). kind 열은 08-3 결정 2의 표 모양이고 v1은 NORMAL만 만든다(계약 7절 D-4).
 *
 * 저장하지 않는 것. 시도 수는 Payment가 attempts에서 센다(06-2 7절 attemptCount).
 */
@Entity
@Table(name = "payment_attempt",
        uniqueConstraints = @UniqueConstraint(name = "uk_payment_attempt_pg_transaction",
                columnNames = "pg_transaction_id"))
public class PaymentAttempt {

    /** 11 INTERNAL-01 요청 표의 failureCode. FAILED에서 유일한 값이다 */
    public static final String MOCK_DECLINED = "MOCK_DECLINED";

    @Id
    @Column(name = "id", nullable = false, length = 64)
    private String id;

    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 16)
    private AttemptKind kind;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private PaymentAttemptStatus status;

    // 11 응답 모델 PaymentAttempt의 amount와 currency. 청구액과 같은 값이다(2-1절 금액 일치)
    @Embedded
    private Money amount;

    // 접수 때 Mock PG가 정한다. 앱 서비스가 같은 트랜잭션에서 요청한 뒤 attachPgTransaction으로
    // 붙이고 그 뒤에 저장한다(08-3 결정 9, 계약 2절 openAttempt 8행). 그래서 DB에서는 null이 없다
    @Column(name = "pg_transaction_id", nullable = false, length = 64)
    private String pgTransactionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "mock_mode", nullable = false, length = 16)
    private MockMode mockMode;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "failure_code", length = 32)
    private String failureCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "refund_reason", length = 32)
    private RefundReason refundReason;

    @Column(name = "refunded_at")
    private Instant refundedAt;

    protected PaymentAttempt() {
    }

    private PaymentAttempt(int attemptNumber, Money amount, MockMode mockMode, Instant now) {
        this.id = PaymentAttemptId.newId().value();
        this.attemptNumber = attemptNumber;
        this.kind = AttemptKind.NORMAL;
        this.status = PaymentAttemptStatus.REQUESTED;
        this.amount = amount;
        this.mockMode = mockMode;
        this.requestedAt = now;
    }

    /**
     * 06-4 1-3 (생성) 행. 결제 요청이 REQUESTED NORMAL 시도를 만든다. 가드(진행 유일, 시도 상한,
     * 승인 유일)는 Payment.openAttempt가 이 메서드를 부르기 전에 검사한다.
     */
    static PaymentAttempt request(int attemptNumber, Money amount, MockMode mockMode, Instant now) {
        Objects.requireNonNull(amount, "amount는 null일 수 없다");
        Objects.requireNonNull(mockMode, "mockMode는 null일 수 없다");
        return new PaymentAttempt(attemptNumber, amount, mockMode, now);
    }

    /** 06-4 v5 1-2 openAttempt Post. Mock PG가 돌려준 거래 번호를 붙인다. 한 번만 붙는다 */
    void attachPgTransaction(String pgTransactionId) {
        Objects.requireNonNull(pgTransactionId, "pgTransactionId는 null일 수 없다");
        if (this.pgTransactionId != null) {
            throw new IllegalStateException("거래 번호는 한 번만 붙는다: " + id);
        }
        this.pgTransactionId = pgTransactionId;
    }

    /** 06-4 1-3 REQUESTED에서 APPROVED. completedAt을 적고 failureCode는 null이다 */
    void approve(Instant now) {
        requireStatus(PaymentAttemptStatus.REQUESTED, PaymentAttemptStatus.APPROVED);
        this.status = PaymentAttemptStatus.APPROVED;
        this.completedAt = now;
        this.failureCode = null;
    }

    /** 06-4 1-3 REQUESTED에서 FAILED. completedAt과 failureCode(MOCK_DECLINED)를 적는다 */
    void fail(String failureCode, Instant now) {
        Objects.requireNonNull(failureCode, "failureCode는 null일 수 없다");
        requireStatus(PaymentAttemptStatus.REQUESTED, PaymentAttemptStatus.FAILED);
        this.status = PaymentAttemptStatus.FAILED;
        this.completedAt = now;
        this.failureCode = failureCode;
    }

    /** 06-4 1-3 APPROVED에서 REFUNDED. reason과 refundedAt을 적는다(계약 7절 D-5) */
    void refund(RefundReason reason, Instant now) {
        Objects.requireNonNull(reason, "reason은 null일 수 없다");
        requireStatus(PaymentAttemptStatus.APPROVED, PaymentAttemptStatus.REFUNDED);
        this.status = PaymentAttemptStatus.REFUNDED;
        this.refundReason = reason;
        this.refundedAt = now;
    }

    /** 전이 폐쇄(06-4 1-3 금지 전이: 모든 역행). 허용된 출발 상태가 아니면 예외다 */
    private void requireStatus(PaymentAttemptStatus from, PaymentAttemptStatus to) {
        if (this.status != from) {
            throw new InvalidAttemptTransitionException(id(), this.status, to);
        }
    }

    /** I7의 셈 대상. APPROVED 또는 REFUNDED다 */
    public boolean hasApprovalHistory() {
        return status.isApprovalHistory();
    }

    public boolean isNormal() {
        return kind == AttemptKind.NORMAL;
    }

    public boolean isRequested() {
        return status == PaymentAttemptStatus.REQUESTED;
    }

    public boolean hasPgTransaction(String candidate) {
        return pgTransactionId != null && pgTransactionId.equals(candidate);
    }

    public PaymentAttemptId id() {
        return PaymentAttemptId.of(id);
    }

    public int attemptNumber() {
        return attemptNumber;
    }

    public AttemptKind kind() {
        return kind;
    }

    public PaymentAttemptStatus status() {
        return status;
    }

    public Money amount() {
        return amount;
    }

    public String pgTransactionId() {
        return pgTransactionId;
    }

    public MockMode mockMode() {
        return mockMode;
    }

    public Instant requestedAt() {
        return requestedAt;
    }

    public Instant completedAt() {
        return completedAt;
    }

    public String failureCode() {
        return failureCode;
    }

    public RefundReason refundReason() {
        return refundReason;
    }

    public Instant refundedAt() {
        return refundedAt;
    }
}
