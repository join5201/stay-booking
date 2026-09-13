package com.o2o.payment.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.o2o.shared.Money;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * 결제 애그리거트 루트. 설계 근거: 06-2 1절 Payment 행, 06-2 6절 결제 CRC의 Payment 네 행.
 *
 * CRC 첫 행. 대상 예약, 청구 총액, 시도 목록을 안다. 예약은 문자열 bookingId다. 결제는
 * 예약을 모른다(06-1 R6)는 전제 때문에 예약 컨텍스트의 타입을 참조하지 않는다.
 * CRC 둘째 행. 새 시도를 연다. 루트를 잠근 뒤 REQUESTED 없음(I9), 3회 미만(I6), 승인 이력
 * 없음(I7)을 검사하고 통과 시에만 시도를 추가한다. openAttempt다.
 * CRC 셋째 행. 승인과 실패를 기록한다. 같은 pgTransactionId 콜백은 무시하고 이벤트를
 * 재발행하지 않는다(U4). 승인은 승인 이력이 이미 있으면 거부한다(I7). recordApproval과
 * recordFailure다.
 * CRC 넷째 행. 환불을 처리한다. APPROVED 시도가 있을 때만 받고 그 시도를 REFUNDED로
 * 전이시킨다. refund다.
 *
 * 지키는 불변식은 셋이고 전부 NORMAL 시도만 센다(06-2 3-1, 06-4 v5 1-1).
 * I6 시도 상한. NORMAL 시도는 3개를 넘지 않는다.
 * I7 승인 유일. 승인 이력(APPROVED 또는 REFUNDED)인 NORMAL 시도는 하나를 넘지 않는다.
 * I9 진행 유일. REQUESTED인 NORMAL 시도는 동시에 하나를 넘지 않는다.
 *
 * 잠금은 여기가 아니라 앱 서비스가 리포지토리로 건다. 06-2 5절 Payment 행이 시도 추가와
 * 콜백 기록은 루트를 잠근 뒤 수행한다고 적고 06-4 0절이 비관적 락으로 확정한다. 이 클래스의
 * 메서드는 전부 잠긴 뒤에 불린다. 유니크(booking_id)는 U3이고 DB가 맡는다(06-4 1-4).
 * attemptCount는 저장하지 않고 센다(06-2 7절). PG 호출은 하지 않는다(06-4 v5 1-2 결제 머리).
 *
 * 무해 반환 규약. recordApproval과 recordFailure와 refund는 전이 발생이면 true, 종착 재호출
 * 무해면 false를 돌려준다(06-4 0절 종착 재호출 무해, 06-4 v5 1-2 반환 규약). 호출자인 앱
 * 서비스가 false에서 PG 호출과 이벤트 발행을 생략한다.
 */
@Entity
@Table(name = "payment",
        uniqueConstraints = @UniqueConstraint(name = "uk_payment_booking",
                columnNames = "booking_id"))
public class Payment {

    /** I6. 06-2 3-1 결제 시도 수 <= 3 */
    public static final int MAX_ATTEMPTS = 3;

    @Id
    @Column(name = "id", nullable = false, length = 64)
    private String id;

    @Column(name = "booking_id", nullable = false, length = 64)
    private String bookingId;

    // 청구 총액. 첫 요청이 정하고 이후 시도는 이 값과 같아야 한다(06-4 1-2 openAttempt Pre)
    @Embedded
    private Money amount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // 06-2 1절 내부 요소. 루트가 생명주기를 갖고 시도는 루트를 통해서만 접근된다(06-2 6절 CRC).
    // 즉시 로딩인 이유는 이 루트의 모든 행동이 시도 전체를 세기 때문이다(I6, I7, I9)
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "payment_id", nullable = false)
    @OrderBy("attemptNumber asc")
    private List<PaymentAttempt> attempts = new ArrayList<>();

    protected Payment() {
    }

    private Payment(String bookingId, Money amount, Instant now) {
        this.id = PaymentId.newId().value();
        this.bookingId = bookingId;
        this.amount = amount;
        this.createdAt = now;
        this.updatedAt = now;
    }

    /**
     * 첫 요청의 Payment 생성. 설계 근거: 06-4 1-2 openAttempt Pre(첫 요청이면 Payment 생성(U3)).
     * 청구액은 이 순간 고정된다. 같은 bookingId의 둘째 Payment는 DB 유니크가 막는다.
     */
    public static Payment open(String bookingId, Money amount, Instant now) {
        Objects.requireNonNull(bookingId, "bookingId는 null일 수 없다");
        Objects.requireNonNull(amount, "amount는 null일 수 없다");
        if (bookingId.isBlank() || bookingId.length() > 64) {
            throw new IllegalArgumentException("bookingId는 1자 이상 64자 이하여야 한다: " + bookingId);
        }
        if (amount.amount() <= 0) {
            throw new IllegalArgumentException("청구액은 양수여야 한다: " + amount.amount());
        }
        return new Payment(bookingId, amount, now);
    }

    /**
     * RequestPayment의 결제 몫. 설계 근거: 06-4 1-2 openAttempt. Pre가 잠금, 진행 중 시도 없음(I9),
     * 시도 3 미만(I6), 승인 이력 없음(I7), 전달 총액과 청구액 일치를 적고 Post가 REQUESTED 시도
     * 추가를 적는다. 검사 순서는 그 Pre 열의 순서이고 계약 2절 openAttempt 표의 3부터 7행이다.
     *
     * 돌려준 시도는 아직 거래 번호가 없다. 앱 서비스가 같은 트랜잭션에서 Mock PG에 요청하고
     * attachPgTransaction으로 붙인다(08-3 결정 9). PaymentRequested 발행도 앱 서비스 몫이다.
     */
    public PaymentAttempt openAttempt(Money requested, MockMode mockMode, Instant now) {
        Objects.requireNonNull(requested, "requested는 null일 수 없다");
        List<PaymentAttempt> normal = normalAttempts();
        if (normal.stream().anyMatch(PaymentAttempt::isRequested)) {
            throw new AttemptInProgressException(bookingId);
        }
        if (normal.size() >= MAX_ATTEMPTS) {
            throw new AttemptLimitExceededException(bookingId, normal.size());
        }
        Optional<PaymentAttempt> approved = approvedAttempt();
        if (approved.isPresent()) {
            throw new AlreadyApprovedException(bookingId, approved.get().id());
        }
        if (!this.amount.equals(requested)) {
            throw new AmountMismatchException(this.amount, requested);
        }
        PaymentAttempt attempt = PaymentAttempt.request(normal.size() + 1, this.amount, mockMode, now);
        attempts.add(attempt);
        this.updatedAt = now;
        return attempt;
    }

    /** 06-4 v5 1-2 openAttempt Post의 뒷부분. Mock PG가 돌려준 거래 번호를 그 시도에 붙인다 */
    public void attachPgTransaction(PaymentAttemptId attemptId, String pgTransactionId) {
        normalAttempt(attemptId).attachPgTransaction(pgTransactionId);
    }

    /**
     * INTERNAL-01 규칙 1의 결제 몫. 설계 근거: 계약 2절 INTERNAL-01 표 5행과 6행.
     * 거래 번호가 그 시도의 것이 아니면 PgTransactionMismatch, 금액이나 통화가 다르면
     * AmountMismatch다. 금액은 Money로 만들지 않고 long으로 대조한다. Money의 상한이
     * 형식 검사의 상한보다 낮아서다(계약 6절 P03 행).
     *
     * 규칙 2(eventId 기록)가 이 검사와 전이 사이에 있어 전이 메서드와 갈라져 있다.
     */
    public void assertCallbackMatches(PaymentAttemptId attemptId, String pgTransactionId,
                                      long amount, String currency) {
        PaymentAttempt attempt = normalAttempt(attemptId);
        if (!attempt.hasPgTransaction(pgTransactionId)) {
            throw new PgTransactionMismatchException(attemptId, attempt.pgTransactionId(),
                    pgTransactionId);
        }
        Money own = attempt.amount();
        if (own.amount() != amount || !own.currency().equals(currency)) {
            throw new AmountMismatchException(own, amount, currency);
        }
    }

    /**
     * RecordPaymentApproval. 설계 근거: 06-4 1-2 recordApproval. Pre가 잠금과 같은 거래번호는
     * 무해 무시(U4)와 REQUESTED 존재와 승인 이력 없음을 적고, Post가 REQUESTED에서 APPROVED를
     * 적는다. 계약 2절 INTERNAL-01 표의 8행부터 10행이다.
     *
     * 반환. 전이 발생이면 true다. 같은 거래의 승인이 이미 반영돼 있으면(APPROVED 또는 그 뒤의
     * REFUNDED) 상태를 바꾸지 않고 false를 돌려준다. 그것이 U4의 무시이고 11 규칙 3과 8의
     * DUPLICATE다. FAILED 시도에 승인이 오면 역행이라 InvalidAttemptTransition이다(11 규칙 4).
     * 다른 NORMAL 시도에 승인 이력이 있으면 AlreadyApproved다(I7의 둘째 승인 거부).
     * PaymentApproved 발행은 앱 서비스가 true를 받은 뒤 한다.
     */
    public boolean recordApproval(PaymentAttemptId attemptId, String pgTransactionId, Instant now) {
        PaymentAttempt attempt = normalAttempt(attemptId);
        requirePgTransaction(attempt, pgTransactionId);
        if (attempt.hasApprovalHistory()) {
            return false;
        }
        if (!attempt.isRequested()) {
            throw new InvalidAttemptTransitionException(attemptId, attempt.status(),
                    PaymentAttemptStatus.APPROVED);
        }
        Optional<PaymentAttempt> approved = approvedAttempt();
        if (approved.isPresent()) {
            throw new AlreadyApprovedException(bookingId, approved.get().id());
        }
        attempt.approve(now);
        this.updatedAt = now;
        return true;
    }

    /**
     * RecordPaymentFailure. 설계 근거: 06-4 1-2 recordFailure. Pre가 잠금과 같은 거래번호는
     * 무해 무시와 REQUESTED 존재를 적고, Post가 REQUESTED에서 FAILED를 적는다.
     *
     * 반환. 전이 발생이면 true, 같은 거래의 실패가 이미 반영돼 있으면 false(U4, 11 규칙 3).
     * APPROVED나 REFUNDED 시도에 실패가 오면 역행이라 InvalidAttemptTransition이다(11 규칙 4).
     * PaymentFailed는 실패마다 attemptCount를 실어 발행한다(계약 6절 PaymentFailed 발행 시점 행).
     * 발행은 앱 서비스 몫이다.
     */
    public boolean recordFailure(PaymentAttemptId attemptId, String pgTransactionId,
                                 String failureCode, Instant now) {
        PaymentAttempt attempt = normalAttempt(attemptId);
        requirePgTransaction(attempt, pgTransactionId);
        if (attempt.status() == PaymentAttemptStatus.FAILED) {
            return false;
        }
        if (!attempt.isRequested()) {
            throw new InvalidAttemptTransitionException(attemptId, attempt.status(),
                    PaymentAttemptStatus.FAILED);
        }
        attempt.fail(failureCode, now);
        this.updatedAt = now;
        return true;
    }

    /**
     * RefundPayment. 설계 근거: 06-4 1-2 refund. Pre가 잠금과 APPROVED 존재와 REFUNDED 재호출
     * 무해를 적고, Post가 APPROVED에서 REFUNDED를 적는다. 위반 예외는 NoApprovedAttempt다.
     *
     * 반환. 전이 발생이면 true다. 이미 REFUNDED면 상태와 refundedAt을 바꾸지 않고 false다
     * (06-4 0절 종착 재호출 무해의 환불 자리). 앱 서비스가 true에서만 Mock 환불을 부르고
     * PaymentRefunded를 발행한다. 같은 승인 시도에 환불이 하나인 것은 상태가 종착이라서다.
     */
    public boolean refund(PaymentAttemptId attemptId, RefundReason reason, Instant now) {
        PaymentAttempt attempt = normalAttempt(attemptId);
        if (attempt.status() == PaymentAttemptStatus.REFUNDED) {
            return false;
        }
        if (attempt.status() != PaymentAttemptStatus.APPROVED) {
            throw new NoApprovedAttemptException(attemptId, attempt.status());
        }
        attempt.refund(reason, now);
        this.updatedAt = now;
        return true;
    }

    /** U4의 거래 번호 대조. assertCallbackMatches를 먼저 지나지만 전이 앞에서 한 번 더 본다 */
    private void requirePgTransaction(PaymentAttempt attempt, String pgTransactionId) {
        if (!attempt.hasPgTransaction(pgTransactionId)) {
            throw new PgTransactionMismatchException(attempt.id(), attempt.pgTransactionId(),
                    pgTransactionId);
        }
    }

    /** 계약 2절 INTERNAL-01 표 3행. NORMAL 시도가 아니면 UnknownAttempt(11 규칙 1의 404) */
    private PaymentAttempt normalAttempt(PaymentAttemptId attemptId) {
        return attempt(attemptId).filter(PaymentAttempt::isNormal)
                .orElseThrow(() -> new UnknownAttemptException(attemptId));
    }

    /**
     * 06-2 7절. attemptCount는 저장하지 않고 센다. NORMAL 시도 수이고, 이벤트 시점에는 I9
     * 때문에 다른 REQUESTED가 없어 완료된 시도 수와 같다(계약 6절 attemptCount의 뜻 행).
     */
    public int attemptCount() {
        return normalAttempts().size();
    }

    /** NORMAL 시도만 attemptNumber 오름차순으로. 11 PAY-02 처리 규칙과 응답 모델 PaymentSummary */
    public List<PaymentAttempt> normalAttempts() {
        return attempts.stream()
                .filter(PaymentAttempt::isNormal)
                .sorted(Comparator.comparingInt(PaymentAttempt::attemptNumber))
                .toList();
    }

    /** I7의 승인 이력 시도. 환불 뒤에도 그 시도를 가리킨다(11 응답 모델 PaymentSummary approvedAttemptId) */
    public Optional<PaymentAttempt> approvedAttempt() {
        return normalAttempts().stream().filter(PaymentAttempt::hasApprovalHistory).findFirst();
    }

    public Optional<PaymentAttempt> attempt(PaymentAttemptId attemptId) {
        Objects.requireNonNull(attemptId, "attemptId는 null일 수 없다");
        return attempts.stream().filter(a -> a.id().equals(attemptId)).findFirst();
    }

    public PaymentId id() {
        return PaymentId.of(id);
    }

    public String bookingId() {
        return bookingId;
    }

    public Money amount() {
        return amount;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
