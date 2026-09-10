package com.o2o.inventory.domain;

/**
 * I1 위반. 설계 근거: 06-4 1-2 재고와 요금 계약표 adjust의 위반 시 예외 InventoryBelowOccupied.
 *
 * 11 INV-03의 오류 코드는 INVENTORY_BELOW_COMMITTED이고 상태는 409다. 검증 항목은 T04다.
 */
public class InventoryBelowOccupiedException extends RuntimeException {

    public InventoryBelowOccupiedException(int totalCount, int soldCount, int heldCount) {
        super("총 수량이 점유 수량보다 작다. 총 " + totalCount
                + ", 판매 " + soldCount + ", 선점 " + heldCount);
    }
}
