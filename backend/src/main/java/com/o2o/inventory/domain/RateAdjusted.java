package com.o2o.inventory.domain;

import java.time.Instant;
import java.time.LocalDate;

import com.o2o.shared.Money;
import com.o2o.shared.RoomTypeId;

/**
 * 요금이 조정됨. 설계 근거: 06-4 1-2 adjustRate의 Post 열, 03 재고와 요금 이벤트 행.
 *
 * 같은 Post 열이 기존 스냅샷 무영향(I4)도 적는다. 그 보장은 이 이벤트가 하는 것이 아니라
 * 예약이 생성 시점의 단가를 복사해 두는 것으로 이미 서 있다. 구독자가 없는 이유도 같다.
 */
public record RateAdjusted(RoomTypeId roomTypeId, LocalDate stayDate, Money rate,
                           Instant occurredAt) {

    public static RateAdjusted of(DailyRate dailyRate) {
        return new RateAdjusted(dailyRate.roomTypeId(), dailyRate.stayDate(),
                dailyRate.rate(), dailyRate.updatedAt());
    }
}
