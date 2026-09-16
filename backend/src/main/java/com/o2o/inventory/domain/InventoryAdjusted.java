package com.o2o.inventory.domain;

import java.time.Instant;
import java.time.LocalDate;

import com.o2o.shared.RoomTypeId;

/**
 * 재고가 조정됨. 설계 근거: 06-4 1-2 adjust의 Post 열, 03 재고와 요금 이벤트 행.
 *
 * 바뀐 뒤의 총 수량을 싣는다. 증감량이 아니라 총 수량인 근거는 11 INV-03 요청 표다.
 */
public record InventoryAdjusted(RoomTypeId roomTypeId, LocalDate stayDate, int totalCount,
                                Instant occurredAt) {

    public static InventoryAdjusted of(DailyInventory inventory) {
        return new InventoryAdjusted(inventory.roomTypeId(), inventory.stayDate(),
                inventory.totalCount(), inventory.updatedAt());
    }
}
