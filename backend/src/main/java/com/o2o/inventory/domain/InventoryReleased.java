package com.o2o.inventory.domain;

import java.time.Instant;
import java.time.LocalDate;

import com.o2o.shared.RoomTypeId;

/**
 * 재고가 반환됨. 설계 근거: 06-4 1-2 releaseHeld(n)과 releaseSold(n)의 Post 열, 06-4 v5 2-4 이벤트
 * 페이로드 행(source가 HELD 또는 SOLD), 03 재고와 요금 이벤트 행, 예약 2차 계약 개정 2.
 *
 * 만료는 선점 반환(A2)이라 HELD이고 취소는 판매분 반환이라 SOLD다. 행 단위 사실이라 숙박
 * 날짜마다 하나씩 낸다. 발행과 전달 규칙은 InventoryCommitted와 같다.
 */
public record InventoryReleased(RoomTypeId roomTypeId, LocalDate stayDate, Source source,
                                int availableCount, Instant occurredAt) {

    public enum Source {
        HELD,
        SOLD
    }

    public static InventoryReleased of(DailyInventory inventory, Source source) {
        return new InventoryReleased(inventory.roomTypeId(), inventory.stayDate(), source,
                inventory.availableCount(), inventory.updatedAt());
    }
}
