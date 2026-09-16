package com.o2o.inventory.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * RATE-02 요청 body. 설계 근거: 11 요금 RATE-02의 필드표.
 *
 * currency가 없는 것이 의도다. 같은 절의 처리 규칙이 통화는 바꾸지 않는다고 적는다.
 * 등록 요청에는 있고 수정 요청에는 없는 차이가 그 규칙을 코드로 만든 것이다.
 */
public record AdjustRateRequest(
        @NotNull @Min(0) Long version,
        @NotNull @Min(1) @Max(1_000_000_000L) Long amount) {
}
