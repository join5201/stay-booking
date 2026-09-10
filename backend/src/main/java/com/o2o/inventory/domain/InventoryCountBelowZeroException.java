package com.o2o.inventory.domain;

/**
 * I1a 위반. 설계 근거: 06-4 1-1 수량 하한. soldCount와 heldCount는 음수가 될 수 없다.
 *
 * 06-4 1-2 계약표는 I1a에 별도 예외 이름을 주지 않는다. hold와 commit과 release의 위반 예외는
 * 전부 가용 수량이나 선점 수량 부족을 가리키고, 음수 자체를 가리키는 이름은 표에 없다.
 * 그래서 이름을 여기서 붙이고 그 사실을 적어 둔다.
 *
 * 이번 묶음에서는 이 예외에 닿는 경로가 없다. 수량을 내리는 행동이 hold와 commit과
 * release뿐인데 셋 다 계약 2-2절로 범위 밖이다. 그럼에도 검사를 지금 넣는 이유는 06-2 6절
 * CRC가 모든 수량 변경에서 I1과 I1a를 함께 검사하라고 적기 때문이다. 예약 묶음이 그 세
 * 행동을 붙일 때 검사 자리를 다시 찾지 않아도 된다.
 */
public class InventoryCountBelowZeroException extends RuntimeException {

    public InventoryCountBelowZeroException(int soldCount, int heldCount) {
        super("수량은 음수가 될 수 없다. 판매 " + soldCount + ", 선점 " + heldCount);
    }
}
