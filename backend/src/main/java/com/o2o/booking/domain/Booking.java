package com.o2o.booking.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

import com.o2o.shared.PropertyId;
import com.o2o.shared.RoomTypeId;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * 예약 애그리거트 루트. 설계 근거: 06-2 1절 예약 행, 06-2 6절 예약 CRC의 Booking 행,
 * 06-4 1-2 Booking 생성자 행, 11 응답 모델 Booking.
 *
 * CRC의 책임 두 줄에 대응한다. 이용자, 대상 객실 타입, 숙박 기간, 인원, 금액 스냅샷, 상태,
 * 만료 시각, 멱등키를 알고, 생성 시 기간 순서(I3)를 확인하고 스냅샷을 검증(I10, I11, I12, I15)한
 * 뒤 동결한다(I4). 1차가 HELD 생성을 만들었고 2차가 전이 셋을 더했다. 상태를 바꾸는 공개
 * 메서드는 confirm, expireByTtl, expireByPaymentFailure, cancel뿐이고 허용 전이는 셋이다(I5.
 * 06-4 1-3 예약 상태 표). 종착 상태에 같은 전이를 다시 부르면 아무것도 바꾸지 않고 전이
 * 없음(false)을 돌려준다(06-4 0절 종착 재호출 무해). 호출자는 그 반환값으로 재고 연산과
 * 이벤트 발행을 정확히 한 번만 한다(R4, 2차 계약 2-1절).
 *
 * 잠금은 여기 없다. 잠금 순서 Booking, Payment, 재고 N행(08-3 결정 3)은 앱 서비스가 리포지토리의
 * 잠금 조회로 지킨다. 전이 메서드는 잠긴 뒤 불리는 것을 전제한다(06-4 1-2 Pre의 잠금).
 *
 * 06-2 1절 필드 목록에 없는 것 하나가 propertyId다. 11 응답 모델 Booking이 요구하고 RoomType의
 * propertyId는 등록 뒤 바뀌지 않아 생성 시점에 같이 저장한다(계약 6절 propertyId 행).
 *
 * 식별자는 BookingId이고 유니크는 (userId, idempotencyKey)다. 06-2 3-4절 U1을 11 명세 멱등
 * 규칙 1의 범위로 읽은 것이다(계약 6절 U1 행). 멱등 기록의 유니크와 겹치는 것은 의도다.
 * 기록 논리에 구멍이 나도 Booking이 둘이 되지 않는다.
 *
 * Hold는 별도 자원이 아니라 이 애그리거트의 HELD 상태다(11 명세 2151행).
 */
@Entity
@Table(name = "booking",
        uniqueConstraints = @UniqueConstraint(name = "uk_booking_user_idempotency_key",
                columnNames = {"user_id", "idempotency_key"}))
public class Booking {

    /** 11 BOOK-04 요청 표. 취소 사유는 최대 300자다 */
    public static final int CANCELLATION_REASON_MAX_LENGTH = 300;

    /** 11 규칙 6과 상태 전이 표. 세 번째 실패가 HELD를 끝낸다 */
    public static final int PAYMENT_FAILURE_LIMIT = 3;

    @Id
    @Column(name = "id", nullable = false, length = 64)
    private String id;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Column(name = "property_id", nullable = false, length = 64)
    private String propertyId;

    @Column(name = "room_type_id", nullable = false, length = 64)
    private String roomTypeId;

    @Embedded
    private StayPeriod period;

    @Column(name = "user_count", nullable = false)
    private int userCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private BookingStatus status;

    // 생성 시 저장한 Hold 만료 시각. 이후 불변이다(06-4 1-2 RequestBooking Post, I4)
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Embedded
    private PriceSnapshot priceSnapshot;

    @Column(name = "idempotency_key", nullable = false, length = 128)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // 11 응답 모델 Booking의 version. 예약 상태 변경 버전이라 전이가 1씩 올린다. 생성은 0이다
    @Column(name = "version", nullable = false)
    private long version;

    // 아래 다섯은 전이가 채운다. 11 응답 모델 Booking. 전이 전에는 null이고 확정 시각은 취소 뒤에도 남는다
    @Enumerated(EnumType.STRING)
    @Column(name = "expiration_reason", length = 16)
    private ExpirationReason expirationReason;

    @Column(name = "cancellation_reason", length = CANCELLATION_REASON_MAX_LENGTH)
    private String cancellationReason;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "canceled_at")
    private Instant canceledAt;

    @Column(name = "expired_at")
    private Instant expiredAt;

    protected Booking() {
    }

    private Booking(UserId userId, PropertyId propertyId, RoomTypeId roomTypeId, StayPeriod period,
                    int userCount, PriceSnapshot priceSnapshot, IdempotencyKey idempotencyKey,
                    Instant now, Duration ttl) {
        this.id = BookingId.newId().value();
        this.userId = userId.value();
        this.propertyId = propertyId.value();
        this.roomTypeId = roomTypeId.value();
        this.period = period;
        this.userCount = userCount;
        this.status = BookingStatus.HELD;
        this.expiresAt = now.plus(ttl);
        this.priceSnapshot = priceSnapshot;
        this.idempotencyKey = idempotencyKey.value();
        this.createdAt = now;
        this.updatedAt = now;
        this.version = 0L;
    }

    /**
     * RequestBooking의 생성 몫. 설계 근거: 06-4 1-2 Booking 생성자(Pre 기간 순서와 스냅샷 검증
     * 통과, Post HELD 상태의 유효한 Booking)와 RequestBooking(Post HELD 생성, expiresAt = 생성
     * 시각 + ttl, 스냅샷 동결).
     *
     * 기간 순서(I3)는 StayPeriod가 이미 지켰고, 스냅샷의 I10과 I12와 I15는 PriceSnapshot이
     * 지켰다. 여기서는 스냅샷이 이 기간을 덮는지(I11)를 다시 본다. 다른 기간으로 만든 스냅샷이
     * 끼어드는 것을 막는 검사라 값 객체 둘을 함께 아는 자리에서만 할 수 있다.
     *
     * 인원 상한(A5)은 컨텍스트를 넘는 선행조건이라 앱 서비스가 본다(06-4 1-4). 여기서는
     * 1 이상만 본다.
     */
    public static Booking request(UserId userId, PropertyId propertyId, RoomTypeId roomTypeId,
                                  StayPeriod period, int userCount, PriceSnapshot priceSnapshot,
                                  IdempotencyKey idempotencyKey, Instant now, Duration ttl) {
        Objects.requireNonNull(userId, "userId는 null일 수 없다");
        Objects.requireNonNull(propertyId, "propertyId는 null일 수 없다");
        Objects.requireNonNull(roomTypeId, "roomTypeId는 null일 수 없다");
        Objects.requireNonNull(period, "period는 null일 수 없다");
        Objects.requireNonNull(priceSnapshot, "priceSnapshot은 null일 수 없다");
        Objects.requireNonNull(idempotencyKey, "idempotencyKey는 null일 수 없다");
        Objects.requireNonNull(now, "now는 null일 수 없다");
        if (userCount < 1) {
            throw new IllegalArgumentException("인원은 1 이상이어야 한다: " + userCount);
        }
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("TTL은 0보다 커야 한다: " + ttl);
        }
        if (!priceSnapshot.covers(period)) {
            throw new InvalidPriceSnapshotException("스냅샷이 숙박 기간 " + period + "을 덮지 않는다");
        }
        return new Booking(userId, propertyId, roomTypeId, period, userCount, priceSnapshot,
                idempotencyKey, now, ttl);
    }

    /**
     * ConfirmBooking의 상태 몫. 설계 근거: 06-4 1-2 confirm()(Pre 잠금과 HELD, Post CONFIRMED로
     * 전이하고 전이 발생을 반환), 11 상태 전이 표 둘째 행.
     *
     * 이미 CONFIRMED면 무해 경로다. 확정 이동(A3)과 BookingConfirmed 발행은 호출자가 true일 때만
     * 한다. EXPIRED에서는 거부다. 지연 승인의 환불은 승인 지연 정책의 영역이고 여기 오기 전에
     * 앱 서비스가 상태로 분기한다(06-4 1-2 confirm 행의 예외 열, 11 규칙 5).
     *
     * @return 전이가 일어났으면 true, 이미 CONFIRMED라 아무것도 바꾸지 않았으면 false
     */
    public boolean confirm(Instant now) {
        Objects.requireNonNull(now, "now는 null일 수 없다");
        if (status == BookingStatus.CONFIRMED) {
            return false;
        }
        requireHeld(BookingStatus.CONFIRMED);
        this.status = BookingStatus.CONFIRMED;
        this.confirmedAt = now;
        transitioned(now);
        return true;
    }

    /**
     * ExpireBooking의 TTL 몫. 설계 근거: 06-4 1-2 expire(reason)(Pre 잠금과 HELD와 expiresAt 경과,
     * Post EXPIRED로 전이하고 전이 발생을 반환), 11 상태 전이 표 다섯째 행과 시간 경계(처리
     * 시각이 정확히 expiresAt이면 만료).
     *
     * 이미 EXPIRED면 무해 경로다. 선점 반환(A2)과 BookingExpired 발행은 호출자가 true일 때만 한다.
     * 아직 만료 시각 전이면 ExpirationNotDue다.
     */
    public boolean expireByTtl(Instant now) {
        Objects.requireNonNull(now, "now는 null일 수 없다");
        if (status == BookingStatus.EXPIRED) {
            return false;
        }
        requireHeld(BookingStatus.EXPIRED);
        if (now.isBefore(expiresAt)) {
            throw new ExpirationNotDueException(id(), expiresAt, now);
        }
        expire(ExpirationReason.TTL_EXPIRED, now);
        return true;
    }

    /**
     * ExpireBooking의 결제 실패 몫. 설계 근거: 06-4 1-2 expire(reason)(Pre 잠금과 HELD와 이벤트
     * 탑재 시도 수 3 이상 재확인), 11 규칙 6과 상태 전이 표 넷째 행.
     *
     * 시도 수가 3 미만이면 FailedAttemptsBelowLimit다. 처리 시각에 TTL이 이미 지났으면 호출자가
     * 이 메서드 대신 expireByTtl을 불러 TTL_EXPIRED를 우선한다(11 시간 경계). 여기서는 그 판단을
     * 하지 않는다. 원인의 선택은 앱 서비스의 분기다(08-3 결정 5).
     */
    public boolean expireByPaymentFailure(int attemptCount, Instant now) {
        Objects.requireNonNull(now, "now는 null일 수 없다");
        if (status == BookingStatus.EXPIRED) {
            return false;
        }
        requireHeld(BookingStatus.EXPIRED);
        if (attemptCount < PAYMENT_FAILURE_LIMIT) {
            throw new FailedAttemptsBelowLimitException(id(), attemptCount, PAYMENT_FAILURE_LIMIT);
        }
        expire(ExpirationReason.PAYMENT_FAILED, now);
        return true;
    }

    /**
     * CancelBooking의 상태 몫. 설계 근거: 06-4 1-2 cancel()(Pre 잠금과 CONFIRMED, Post CANCELED로
     * 전이하고 전이 발생을 반환), 11 BOOK-04 처리 규칙, 11 응답 모델의 cancellationReason(이유
     * 없이 취소하면 빈 문자열)과 confirmedAt(취소 후에도 유지).
     *
     * 이미 CANCELED면 무해 경로다. 환불 중계와 판매분 반환과 BookingCanceled 발행은 호출자가
     * true일 때만 한다. HELD와 EXPIRED에서는 InvalidStateTransition이다. HELD 이탈은 TTL이
     * 한다(11 BOOK-04 규칙). 취소 날짜 조건(P05)은 컨텍스트 밖 시계(서울 오늘)를 보는
     * 선행조건이라 앱 서비스가 본다(06-4 1-4).
     */
    public boolean cancel(String reason, Instant now) {
        Objects.requireNonNull(now, "now는 null일 수 없다");
        String normalized = reason == null ? "" : reason;
        if (normalized.length() > CANCELLATION_REASON_MAX_LENGTH) {
            throw new IllegalArgumentException("취소 사유는 " + CANCELLATION_REASON_MAX_LENGTH
                    + "자 이하여야 한다: " + normalized.length());
        }
        if (status == BookingStatus.CANCELED) {
            return false;
        }
        if (status != BookingStatus.CONFIRMED) {
            throw new InvalidStateTransitionException(id(), status, BookingStatus.CANCELED);
        }
        this.status = BookingStatus.CANCELED;
        this.cancellationReason = normalized;
        this.canceledAt = now;
        transitioned(now);
        return true;
    }

    /** HELD에서만 나갈 수 있는 전이의 공통 Pre. 종착 무해는 호출자가 먼저 걸렀다 */
    private void requireHeld(BookingStatus to) {
        if (status != BookingStatus.HELD) {
            throw new InvalidStateTransitionException(id(), status, to);
        }
    }

    private void expire(ExpirationReason reason, Instant now) {
        this.status = BookingStatus.EXPIRED;
        this.expirationReason = reason;
        this.expiredAt = now;
        transitioned(now);
    }

    /** 전이마다 version이 1 오르고 updatedAt이 처리 시각이 된다. expiresAt은 그대로다(I4) */
    private void transitioned(Instant now) {
        this.version = this.version + 1;
        this.updatedAt = now;
    }

    public BookingId id() {
        return BookingId.of(id);
    }

    public UserId userId() {
        return UserId.of(userId);
    }

    public PropertyId propertyId() {
        return PropertyId.of(propertyId);
    }

    public RoomTypeId roomTypeId() {
        return RoomTypeId.of(roomTypeId);
    }

    public StayPeriod period() {
        return period;
    }

    public int userCount() {
        return userCount;
    }

    public BookingStatus status() {
        return status;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public PriceSnapshot priceSnapshot() {
        return priceSnapshot;
    }

    public IdempotencyKey idempotencyKey() {
        return IdempotencyKey.of(idempotencyKey);
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public long version() {
        return version;
    }

    /** EXPIRED이면 원인, 그 외 null(11 응답 모델) */
    public ExpirationReason expirationReason() {
        return expirationReason;
    }

    /** 취소 전 null, 이유 없이 취소하면 빈 문자열(11 응답 모델) */
    public String cancellationReason() {
        return cancellationReason;
    }

    public Instant confirmedAt() {
        return confirmedAt;
    }

    public Instant canceledAt() {
        return canceledAt;
    }

    public Instant expiredAt() {
        return expiredAt;
    }

    /** 처리 시각이 expiresAt 이상이면 만료 대상이다. 같음도 포함한다(11 시간 경계) */
    public boolean isDue(Instant now) {
        return !now.isBefore(expiresAt);
    }
}
