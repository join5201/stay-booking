package com.o2o.inventory.domain;

import java.time.LocalDate;

import com.o2o.shared.RoomTypeId;

/**
 * 설계 근거: 06-4 1-2 adjust의 잠금 대상 존재 선행조건. 11 INV-05가 레코드 없으면 404라고 적는다(명세 295행).
 */
public class InventoryNotFoundException extends RuntimeException {

    public InventoryNotFoundException(RoomTypeId roomTypeId, LocalDate stayDate) {
        super("재고를 찾을 수 없다: " + roomTypeId.value() + " " + stayDate);
    }
}
