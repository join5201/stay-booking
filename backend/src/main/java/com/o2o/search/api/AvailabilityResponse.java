package com.o2o.search.api;

import java.util.List;

import com.o2o.search.application.AvailabilityResult;
import com.o2o.shared.ApiDate;

/**
 * 응답 모델 Availability. 설계 근거: 11 응답 모델 Availability 열 칸과 AvailabilityDay 두 칸.
 * 날짜 배열은 date 오름차순이고 없으면 빈 배열이다.
 */
public record AvailabilityResponse(String roomTypeId, String checkIn, String checkOut,
                                   int guestCount, boolean available, int availableCount,
                                   List<Day> days, List<String> missingInventoryDates,
                                   List<String> missingRateDates, List<String> reasons) {

    /** availableCount는 재고 레코드가 없으면 null이다 */
    public record Day(String date, Integer availableCount) {
    }

    public static AvailabilityResponse from(AvailabilityResult result) {
        return new AvailabilityResponse(result.roomTypeId().value(),
                ApiDate.format(result.stay().checkIn()), ApiDate.format(result.stay().checkOut()),
                result.guestCount().value(), result.available(), result.availableCount(),
                result.days().stream()
                        .map(d -> new Day(ApiDate.format(d.date()), d.availableCount())).toList(),
                result.missingInventoryDates().stream().map(ApiDate::format).toList(),
                result.missingRateDates().stream().map(ApiDate::format).toList(),
                result.reasons());
    }
}
