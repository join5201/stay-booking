package com.o2o.inventory.domain;

import java.time.Instant;
import java.time.LocalDate;

import com.o2o.shared.RoomTypeId;

/**
 * 재고가 개설됨. 설계 근거: 06-4 1-2 openInventory의 Post 열, 03 재고와 요금 이벤트 행, 05-2 2절.
 *
 * 구독자가 없다. 카탈로그의 네 이벤트와 같은 성격이고 같은 이유로 발행한다. 계약표 Post 열에
 * 적혀 있으면 코드에 있어야 한다. 검증 항목은 계약 8-1절 V12다.
 *
 * INV-02 일괄 등록은 날짜 수만큼 이 이벤트를 낸다. 06-4 0절이 재고 개설의 여러 행 생성을
 * 한 트랜잭션 한 애그리거트 규칙의 의도적 예외로 적고, 이벤트는 행 단위 사실이라 행마다 낸다.
 */
public record InventoryOpened(RoomTypeId roomTypeId, LocalDate stayDate, int totalCount,
                              Instant occurredAt) {

    public static InventoryOpened of(DailyInventory inventory) {
        return new InventoryOpened(inventory.roomTypeId(), inventory.stayDate(),
                inventory.totalCount(), inventory.createdAt());
    }
}
