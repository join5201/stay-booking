package com.o2o.inventory.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * INV-03 요청 body. 설계 근거: 11 재고 INV-03의 필드표.
 *
 * 카탈로그의 수정 요청과 다른 점 하나. 그쪽은 바꿀 수 있는 필드가 여럿이라 version 외에
 * 하나 이상이라는 검사를 따로 두었다. 여기는 바꿀 수 있는 것이 totalCount 하나뿐이라
 * 그 검사가 곧 NotNull이다. 같은 규칙의 다른 모양이다.
 *
 * heldCount와 soldCount가 없는 것이 의도다. 같은 절의 처리 규칙이 둘을 직접 수정하지
 * 않는다고 적는다. 필드를 두면 규칙을 코드가 아니라 사람이 지켜야 한다.
 */
public record AdjustInventoryRequest(
        @NotNull @Min(0) Long version,
        @NotNull @Min(0) @Max(100_000) Integer totalCount) {
}
