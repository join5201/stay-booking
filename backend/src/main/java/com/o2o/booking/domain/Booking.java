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
 * 뒤 동결한다(I4). 1차는 HELD 생성만 만든다. 전이 셋(I5)은 2차다. 상태를 바꾸는 공개
 * 메서드를 두지 않는 것이 1차의 I5다(task-S9-booking 2-1절).
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

    // 11 응답 모델 Booking의 version. 예약 상태 변경 버전이라 전이가 올린다. 1차는 0이다
    @Column(name = "version", nullable = false)
    private long version;

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
}
