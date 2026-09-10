package com.o2o.inventory.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.o2o.shared.Money;
import com.o2o.shared.RoomTypeId;
import com.o2o.shared.VersionConflictException;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * 날짜별 요금 애그리거트 루트. 설계 근거: 06-2 1절 DailyRate 행, 06-2 6절 재고와 요금 CRC.
 *
 * CRC의 책임 두 줄에 대응한다. 객실 타입과 날짜의 1박 단가를 알고, 등록하고 조정하며
 * I2를 검사한다.
 *
 * 지키는 불변식은 I2 하나다(06-2 3-1, 06-4 1-1). 요금은 0보다 크다. 재고와 다른 점이
 * 여기 있다. 재고의 totalCount는 0을 허용하고 요금의 amount는 0을 허용하지 않는다.
 * 근거가 다르다. 재고 0은 11 명세 139행이 허용하고, 요금 양수는 불변식이다.
 *
 * 식별자와 유니크는 DailyInventory와 같다. 06-2 1절 식별자 열이 둘을 같게 적는다.
 */
@Entity
@Table(name = "daily_rate",
        uniqueConstraints = @UniqueConstraint(name = "uk_daily_rate_room_date",
                columnNames = {"room_type_id", "stay_date"}))
public class DailyRate {

    private static final String PREFIX = "rate_";

    @Id
    @Column(name = "id", nullable = false, length = 64)
    private String id;

    @Column(name = "room_type_id", nullable = false, length = 64)
    private String roomTypeId;

    @Column(name = "stay_date", nullable = false)
    private LocalDate stayDate;

    // 06-2 1절이 DailyRate의 내부 요소를 rate(Money)로 적는다. 금액과 통화를 한 값으로 묶는다
    @Embedded
    private Money rate;

    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected DailyRate() {
    }

    private DailyRate(RoomTypeId roomTypeId, LocalDate stayDate, Money rate, Instant now) {
        this.id = PREFIX + UUID.randomUUID().toString().replace("-", "");
        this.roomTypeId = roomTypeId.value();
        this.stayDate = stayDate;
        this.rate = rate;
        this.version = 0L;
        this.createdAt = now;
        this.updatedAt = now;
    }

    /**
     * RATE-01. 설계 근거: 06-4 1-2 registerRate. Invariant가 요금 양수(I2)이고 위반 시 예외가
     * InvalidRate다. roomType 존재와 중복 없음은 앱 서비스와 DB가 맡는다.
     */
    public static DailyRate register(RoomTypeId roomTypeId, LocalDate stayDate, Money rate,
                                     Instant now) {
        validateRate(rate);
        return new DailyRate(roomTypeId, stayDate, rate, now);
    }

    /**
     * RATE-02. 설계 근거: 06-4 1-2 adjustRate. Post가 기존 스냅샷 무영향을 적는다(I4 스냅샷 동결).
     *
     * 그 무영향은 이 클래스가 하는 일이 아니라 안 하는 일로 지켜진다. 예약이 생성될 때
     * 그 시점의 단가를 자기 스냅샷에 복사해 두므로, 여기서 값을 바꿔도 복사본은 그대로다.
     * 이 애그리거트가 Booking을 모르는 것이 그 보장의 근거다.
     *
     * 통화를 바꾸지 않는 근거는 11 명세 410행이다. 그래서 이 메서드는 금액만 받는다.
     */
    public void adjust(long expectedVersion, long newAmount, Instant now) {
        if (this.version != expectedVersion) {
            throw new VersionConflictException(expectedVersion, this.version);
        }
        Money newRate = Money.krw(newAmount);
        validateRate(newRate);
        this.rate = newRate;
        this.version = this.version + 1;
        this.updatedAt = now;
    }

    /** I2. 06-2 6절 CRC가 등록과 조정 둘 다에서 검사하라고 적어서 한자리에 둔다 */
    private static void validateRate(Money rate) {
        if (rate.amount() <= 0) {
            throw new InvalidRateException(rate.amount());
        }
    }

    public String id() {
        return id;
    }

    public RoomTypeId roomTypeId() {
        return RoomTypeId.of(roomTypeId);
    }

    public LocalDate stayDate() {
        return stayDate;
    }

    public Money rate() {
        return rate;
    }

    public long version() {
        return version;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
