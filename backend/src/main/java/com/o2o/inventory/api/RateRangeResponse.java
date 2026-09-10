package com.o2o.inventory.api;

import java.time.LocalDate;
import java.util.List;

import com.o2o.inventory.domain.DailyRate;

/**
 * 설계 근거: 11 응답 모델 RateRange. 다섯 필드가 그 표 순서 그대로다.
 *
 * InventoryRangeResponse와 열이 같고 items의 원소만 다르다. 그래도 하나로 묶지 않는다.
 * 명세가 두 모델을 따로 적고, 응답 레코드는 명세의 표를 그대로 옮긴 것이라 표가 둘이면
 * 레코드도 둘이다. 앱 서비스의 RangeResult가 타입 매개변수를 쓰는 것과 다른 판단인데
 * 이유가 있다. 그쪽은 계산이라 한 벌이면 되고 이쪽은 계약이라 명세를 따라간다.
 */
public record RateRangeResponse(
        String roomTypeId,
        String from,
        String to,
        List<DailyRateResponse> items,
        List<String> missingDates) {

    public static RateRangeResponse of(String roomTypeId, LocalDate from, LocalDate to,
                                       List<DailyRate> items, List<LocalDate> missingDates) {
        return new RateRangeResponse(
                roomTypeId,
                ApiDate.format(from),
                ApiDate.format(to),
                items.stream().map(DailyRateResponse::from).toList(),
                missingDates.stream().map(ApiDate::format).toList());
    }
}
