package com.o2o.promotion.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

import com.o2o.shared.PromotionId;
import com.o2o.shared.VersionConflictException;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 프로모션 애그리거트 루트. 설계 근거: 06-2 1절 Promotion 행(내부 요소 discountRate, Condition,
 * status), 06-2 6절 프로모션 CRC, 06-4 1-2 프로모션 계약표 create와 update와 isApplicable.
 *
 * CRC 책임 세 줄에 대응한다. 할인율과 적용 조건을 알고, 생성 시 I8을 검사하고(Condition에
 * 위임), isApplicable로 조건과 상태를 함께 판정한다.
 *
 * 06-2 1절의 status와 06-4 1-3의 CLOSED 전이는 이번 묶음에 없다. 계약 7절 D-2가 다를 골랐다.
 * 11 명세대로 enabled 토글 하나를 두고 상태 머신과 close와 I13은 예약 묶음 뒤로 이월한다.
 * 그래서 이 클래스에 close가 없고 enabled=false가 11 PROMO-02 처리 규칙의 수동 종료다.
 *
 * discountRate 1부터 99 검사는 11 PROMO-01 필드표(계약 8-1절 V2)다. 06-4 1-2 create 행의
 * Invariant 열은 캠페인 순서만 적어서 할인율 범위는 불변식이 아니라 명세 제약이다. 그래도
 * 여기서 보는 이유는 update 경로도 같은 값을 받고 검사 자리가 둘이면 어긋나기 때문이다.
 *
 * 06-4 1-2 update 행의 Post가 기존 예약 스냅샷 무영향을 적는다. DailyRate.adjust와 같은
 * 원리로 이 클래스가 Booking을 모르는 것이 그 보장이다. 검증은 예약 묶음의 V18로 이월했다.
 */
@Entity
@Table(name = "promotion")
public class Promotion {

    static final int MIN_DISCOUNT_RATE = 1;
    static final int MAX_DISCOUNT_RATE = 99;

    @Id
    @Column(name = "id", nullable = false, length = 64)
    private String id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "discount_rate", nullable = false)
    private int discountRate;

    @Embedded
    private Condition condition;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Promotion() {
    }

    private Promotion(String name, int discountRate, Condition condition, boolean enabled,
                      Instant now) {
        this.id = PromotionId.newId().value();
        this.name = name;
        this.discountRate = discountRate;
        this.condition = condition;
        this.enabled = enabled;
        this.version = 0L;
        this.createdAt = now;
        this.updatedAt = now;
    }

    /**
     * PROMO-01. 설계 근거: 06-4 1-2 create 행. Pre가 조건 유효, Invariant가 캠페인 순서(I8),
     * Post가 새 프로모션과 PromotionCreated 발행이다. 발행은 앱 서비스가 한다(06-4 0절
     * 커밋 후 발행 규칙).
     *
     * 11 명세의 끝 날짜는 시작보다 뒤 제약을 여기서 한 번 더 본다. Condition.of의 I8은 같은
     * 날짜를 통과시키므로(V1) 그보다 엄격한 명세 제약은 루트가 본다.
     */
    public static Promotion create(String name, int discountRate, Condition condition,
                                   boolean enabled, Instant now) {
        Objects.requireNonNull(name, "name은 null일 수 없다");
        Objects.requireNonNull(condition, "condition은 null일 수 없다");
        Objects.requireNonNull(now, "now는 null일 수 없다");
        validateDiscountRate(discountRate);
        condition.requireNonEmptyPeriods();
        return new Promotion(name, discountRate, condition, enabled, now);
    }

    /**
     * PROMO-02. 설계 근거: 06-4 1-2 update 행. Pre가 잠금과 변경 유효, Post가 변경 반영과
     * PromotionUpdated 발행과 기존 예약 스냅샷 무영향. 잠금은 계약 7절 D-2(앞 묶음) 대로
     * 낙관적 잠금이라 version 대조가 첫 검사다(11 공통 절, V9).
     *
     * 11 PROMO-02 처리 규칙 두 번째 줄대로 생략한 필드를 유지한 채 합친 최종 상태에서 날짜
     * 순서와 제약을 다시 본다(V10). 합치는 일은 Condition.merge가 한다.
     */
    public void update(long expectedVersion, PromotionChanges changes, Instant now) {
        Objects.requireNonNull(changes, "changes는 null일 수 없다");
        Objects.requireNonNull(now, "now는 null일 수 없다");
        if (this.version != expectedVersion) {
            throw new VersionConflictException(expectedVersion, this.version);
        }
        Condition merged = condition.merge(changes);
        merged.requireNonEmptyPeriods();
        if (changes.discountRate() != null) {
            validateDiscountRate(changes.discountRate());
            this.discountRate = changes.discountRate();
        }
        if (changes.name() != null) {
            this.name = changes.name();
        }
        if (changes.enabled() != null) {
            this.enabled = changes.enabled();
        }
        this.condition = merged;
        this.version = this.version + 1;
        this.updatedAt = now;
    }

    /**
     * 06-2 6절 CRC의 isApplicable. 06-4 1-2 isApplicable 행의 Pre가 상태 ACTIVE인데 D-2 다에
     * 따라 상태 대신 enabled를 본다. 11 내부 처리 가격과 프로모션 절 첫 줄이 enabled=true를
     * 조건으로 적는다. 나머지 조건은 Condition.matches다(V5).
     */
    public boolean isApplicable(String regionCode, StayRange stay, LocalDate at) {
        return enabled && condition.matches(regionCode, stay, at);
    }

    /** 11 PROMO-01 필드표 discountRate 행. 1부터 99. 0과 100이 거부된다(V2) */
    private static void validateDiscountRate(int discountRate) {
        if (discountRate < MIN_DISCOUNT_RATE || discountRate > MAX_DISCOUNT_RATE) {
            throw new InvalidDiscountRateException(discountRate);
        }
    }

    public PromotionId id() {
        return PromotionId.of(id);
    }

    public String name() {
        return name;
    }

    public int discountRate() {
        return discountRate;
    }

    public Condition condition() {
        return condition;
    }

    public boolean enabled() {
        return enabled;
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
