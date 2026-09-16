package com.o2o.inventory.api;

import java.time.LocalDate;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * INV-02 요청 body. 설계 근거: 11 재고 INV-02의 필드표.
 *
 * from은 포함하고 to는 제외한다. 같은 절의 제약 열이 끝 날짜 제외라고 적는다.
 * 순서와 366일 상한은 여기서 보지 않는다. 앱 서비스의 requirePeriod가 본다. 두 값의
 * 관계는 한 필드의 형식이 아니라서 필드 검증이 볼 수 있는 것이 아니다.
 */
public record BulkInventoryRequest(
        @NotBlank String from,
        @NotBlank String to,
        @NotNull @Min(0) @Max(100_000) Integer totalCount) {

    public LocalDate fromDate() {
        return ApiDate.parse("from", from);
    }

    public LocalDate toDate() {
        return ApiDate.parse("to", to);
    }
}
