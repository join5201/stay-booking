package com.o2o.inventory.domain;

import java.time.LocalDate;

import com.o2o.shared.RoomTypeId;

/**
 * U2 위반. 설계 근거: 06-4 1-2 openInventory의 위반 시 예외 DuplicateInventory와 전체 롤백.
 *
 * 11 INV-01과 INV-02의 오류 코드는 RESOURCE_ALREADY_EXISTS이고 상태는 409다.
 * INV-02는 한 날짜라도 겹치면 전부 실패한다(11 명세 138행, T03). 그 전부 아니면 전무는
 * 이 예외를 트랜잭션 밖으로 던져서 얻는다. 부분 저장을 코드로 막는 것이 아니라
 * 롤백이 막는다.
 */
public class DuplicateInventoryException extends RuntimeException {

    public DuplicateInventoryException(RoomTypeId roomTypeId, LocalDate stayDate) {
        super("이미 등록된 재고다: " + roomTypeId.value() + " " + stayDate);
    }
}
