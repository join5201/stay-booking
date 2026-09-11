package com.o2o.inventory.domain;

import java.time.LocalDate;
import java.util.List;

import com.o2o.shared.RoomTypeId;

/**
 * hold의 위반. 설계 근거: 06-4 1-2 hold(n)의 위반 시 예외 InventoryNotOpened(미개설).
 *
 * 11 BOOK-01의 오류 코드는 INVENTORY_NOT_CONFIGURED이고 상태는 409다. 재고 0과 재고 미개설을
 * 갈라 알려야 한다(T07의 오류 코드 구분). 어느 날짜가 없는지를 실어 나른다.
 */
public class InventoryNotOpenedException extends RuntimeException {

    private final List<LocalDate> missingDates;

    public InventoryNotOpenedException(RoomTypeId roomTypeId, List<LocalDate> missingDates) {
        super("재고가 개설되지 않은 날짜가 있다. " + roomTypeId.value() + " " + missingDates);
        this.missingDates = List.copyOf(missingDates);
    }

    public List<LocalDate> missingDates() {
        return missingDates;
    }
}
