package com.o2o.inventory.domain;

import java.time.Instant;
import java.time.LocalDate;

import com.o2o.shared.Money;
import com.o2o.shared.RoomTypeId;

/**
 * 요금이 등록됨. 설계 근거: 06-4 1-2 registerRate의 Post 열, 03 재고와 요금 이벤트 행.
 */
public record RateRegistered(RoomTypeId roomTypeId, LocalDate stayDate, Money rate,
                             Instant occurredAt) {

    public static RateRegistered of(DailyRate dailyRate) {
        return new RateRegistered(dailyRate.roomTypeId(), dailyRate.stayDate(),
                dailyRate.rate(), dailyRate.createdAt());
    }
}
