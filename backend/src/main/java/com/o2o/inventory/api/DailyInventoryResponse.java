package com.o2o.inventory.api;

import com.o2o.inventory.domain.DailyInventory;

/**
 * 설계 근거: 11 응답 모델 DailyInventory. 일곱 필드가 그 표 순서 그대로다.
 *
 * createdAt과 updatedAt이 없는 것이 의도다. 그 표에 없다. 애그리거트는 두 값을 갖지만
 * 응답 모델이 안 쓰면 안 내보낸다. 명세에 없는 필드를 더하면 API 계약 준수 축에 걸린다.
 *
 * availableCount를 저장 값이 아니라 애그리거트의 계산으로 채운다. 06-2 6절 CRC가 가용성을
 * 계산해 답하되 저장하지 않는다고 적는다.
 */
public record DailyInventoryResponse(
        String roomTypeId,
        String date,
        int totalCount,
        int heldCount,
        int soldCount,
        int availableCount,
        long version) {

    public static DailyInventoryResponse from(DailyInventory inventory) {
        return new DailyInventoryResponse(
                inventory.roomTypeId().value(),
                ApiDate.format(inventory.stayDate()),
                inventory.totalCount(),
                inventory.heldCount(),
                inventory.soldCount(),
                inventory.availableCount(),
                inventory.version());
    }
}
