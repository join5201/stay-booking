package com.o2o.inventory.api;

import com.o2o.inventory.domain.DailyRate;

/**
 * 설계 근거: 11 응답 모델 DailyRate. 다섯 필드가 그 표 순서 그대로다.
 *
 * amount와 currency가 따로 나가는 것이 값 객체 Money를 그대로 펼친 모양이다. 응답 모델이
 * 두 필드로 적혀 있어 중첩하지 않는다. 안에서 하나로 묶는 것과 밖으로 둘로 내는 것은
 * 서로 어긋나지 않는다.
 */
public record DailyRateResponse(
        String roomTypeId,
        String date,
        long amount,
        String currency,
        long version) {

    public static DailyRateResponse from(DailyRate dailyRate) {
        return new DailyRateResponse(
                dailyRate.roomTypeId().value(),
                ApiDate.format(dailyRate.stayDate()),
                dailyRate.rate().amount(),
                dailyRate.rate().currency(),
                dailyRate.version());
    }
}
