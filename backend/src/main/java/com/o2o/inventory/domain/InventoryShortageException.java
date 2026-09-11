package com.o2o.inventory.domain;

import java.time.LocalDate;

import com.o2o.shared.RoomTypeId;

/**
 * hold의 위반. 설계 근거: 06-4 1-2 hold(n)의 위반 시 예외 InventoryShortage(가용 부족).
 *
 * 11 BOOK-01의 오류 코드는 INVENTORY_UNAVAILABLE이고 상태는 409다. 같은 표가 어느 쪽이든
 * 연박 원자성(A1)에 따라 전체 롤백이라 적으므로 이 예외는 트랜잭션을 깨고 나간다.
 * 검증 항목은 T08과 T09다.
 */
public class InventoryShortageException extends RuntimeException {

    private final LocalDate stayDate;

    public InventoryShortageException(RoomTypeId roomTypeId, LocalDate stayDate, int available,
                                      int requested) {
        super("가용 재고가 부족하다. " + roomTypeId.value() + " " + stayDate
                + " 가용 " + available + ", 요청 " + requested);
        this.stayDate = stayDate;
    }

    public LocalDate stayDate() {
        return stayDate;
    }
}
