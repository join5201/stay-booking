package com.o2o.search.application;

import java.time.LocalDate;
import java.util.List;

import com.o2o.promotion.domain.StayRange;
import com.o2o.shared.GuestCount;
import com.o2o.shared.RoomTypeId;

/**
 * 설계 근거: 11 응답 모델 Availability 열 칸과 AvailabilityDay 두 칸. reasons의 값 넷은 그 표의
 * OCCUPANCY_EXCEEDED, INVENTORY_NOT_CONFIGURED, INVENTORY_UNAVAILABLE, RATE_NOT_CONFIGURED이고
 * 해당 항목만 중복 없이 담는다.
 */
public record AvailabilityResult(RoomTypeId roomTypeId, StayRange stay, GuestCount guestCount,
                                 boolean available, int availableCount, List<Day> days,
                                 List<LocalDate> missingInventoryDates,
                                 List<LocalDate> missingRateDates, List<String> reasons) {

    /** availableCount는 재고 레코드가 없으면 null이다(11 AvailabilityDay 표) */
    public record Day(LocalDate date, Integer availableCount) {
    }
}
