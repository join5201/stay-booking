package com.o2o.inventory.domain;

import java.time.Instant;
import java.time.LocalDate;

import com.o2o.shared.RoomTypeId;

/**
 * 재고가 선점됨. 설계 근거: 06-4 1-2 hold(n)의 Post 열, 03 재고와 요금 이벤트 행, 06-2 4절
 * 생성 경계의 커밋 후 이벤트(InventoryHeld ×N).
 *
 * 행 단위 사실이라 숙박 날짜마다 하나씩 낸다. 발행은 예약 앱 서비스가 트랜잭션 안에서 하고
 * 커밋 뒤 전달은 구독자가 지킨다(backend/.claude/rules/layers.md 3-3). 구독자는 아직 없다.
 * 검증 항목은 계약 8-1절 K12다.
 */
public record InventoryHeld(RoomTypeId roomTypeId, LocalDate stayDate, int heldCount,
                            Instant occurredAt) {

    public static InventoryHeld of(DailyInventory inventory) {
        return new InventoryHeld(inventory.roomTypeId(), inventory.stayDate(),
                inventory.heldCount(), inventory.updatedAt());
    }
}
