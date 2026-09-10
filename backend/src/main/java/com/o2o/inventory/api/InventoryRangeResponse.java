package com.o2o.inventory.api;

import java.time.LocalDate;
import java.util.List;

import com.o2o.inventory.domain.DailyInventory;

/**
 * 설계 근거: 11 응답 모델 InventoryRange. 다섯 필드가 그 표 순서 그대로다.
 *
 * INV-02가 이 모델로 답하는 이유는 일괄 등록이 만든 것이 행 하나가 아니라 기간이기 때문이다.
 * 등록 성공이면 missingDates는 빈 배열이다. 같은 절의 처리 규칙이 그렇게 적는다. 빈 배열과
 * null을 가르는 것이 중요해서 List.of를 쓴다. null이면 필드가 사라지거나 null로 나간다.
 *
 * missingDates가 채워지는 것은 조회인 INV-04다. 그쪽은 6-2단계다.
 */
public record InventoryRangeResponse(
        String roomTypeId,
        String from,
        String to,
        List<DailyInventoryResponse> items,
        List<String> missingDates) {

    public static InventoryRangeResponse of(String roomTypeId, LocalDate from, LocalDate to,
                                            List<DailyInventory> items,
                                            List<LocalDate> missingDates) {
        return new InventoryRangeResponse(
                roomTypeId,
                ApiDate.format(from),
                ApiDate.format(to),
                items.stream().map(DailyInventoryResponse::from).toList(),
                missingDates.stream().map(ApiDate::format).toList());
    }
}
