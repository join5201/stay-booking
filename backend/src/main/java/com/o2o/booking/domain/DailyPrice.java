package com.o2o.booking.domain;

import java.time.LocalDate;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * 날짜별 가격 행. 설계 근거: 06-2 1절 PriceSnapshot VO의 DailyPrice ×N, 05-3 DailyPrice 행,
 * 11 응답 모델 PriceDay. baseAmount는 할인 전 1박 금액, discountAmount는 날짜별로 버림한
 * 할인액, finalAmount는 그 차다. 할인은 단가를 넘을 수 없고 단가는 0보다 크다. 요금에 I2가
 * 걸리므로 그 값을 옮겨 온 단가도 같다.
 */
@Embeddable
public record DailyPrice(@Column(name = "stay_date", nullable = false) LocalDate date,
                         @Column(name = "base_amount", nullable = false) long baseAmount,
                         @Column(name = "discount_amount", nullable = false) long discountAmount) {

    public DailyPrice {
        Objects.requireNonNull(date, "date는 null일 수 없다");
        if (baseAmount <= 0) {
            throw new InvalidPriceSnapshotException("단가는 0보다 커야 한다: " + date + " " + baseAmount);
        }
        if (discountAmount < 0 || discountAmount > baseAmount) {
            throw new InvalidPriceSnapshotException("할인액은 0 이상 단가 이하여야 한다: " + date
                    + " " + discountAmount);
        }
    }

    public long finalAmount() {
        return baseAmount - discountAmount;
    }
}
