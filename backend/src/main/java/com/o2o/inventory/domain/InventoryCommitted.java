package com.o2o.inventory.domain;

import java.time.Instant;
import java.time.LocalDate;

import com.o2o.shared.RoomTypeId;

/**
 * 선점이 판매로 이동함(A3). 설계 근거: 06-4 1-2 commit(n)의 Post 열, 06-4 v5 2-4 이벤트 페이로드 행,
 * 03 재고와 요금 이벤트 행, 예약 2차 계약 개정 2.
 *
 * InventoryHeld처럼 행 단위 사실이라 숙박 날짜마다 하나씩 낸다. 발행은 예약 앱 서비스가 확정
 * 트랜잭션 안에서 하고 커밋 뒤 전달은 구독자가 지킨다(layers.md 3-3). 프로덕션 구독자는 없다.
 * 검증 항목은 예약 2차 계약 8-1절 L12다.
 */
public record InventoryCommitted(RoomTypeId roomTypeId, LocalDate stayDate, int soldCount,
                                 Instant occurredAt) {

    public static InventoryCommitted of(DailyInventory inventory) {
        return new InventoryCommitted(inventory.roomTypeId(), inventory.stayDate(),
                inventory.soldCount(), inventory.updatedAt());
    }
}
