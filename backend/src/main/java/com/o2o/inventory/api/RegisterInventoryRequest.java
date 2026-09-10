package com.o2o.inventory.api;

import java.time.LocalDate;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * INV-01 요청 body. 설계 근거: 11 재고 INV-01의 필드표.
 *
 * totalCount가 Integer인 이유는 RegisterRoomTypeRequest와 같다. int면 누락이 0으로 들어와
 * 필수 누락이 값 0으로 둔갑한다. 이번 API는 0이 실제 허용 값이라 그 구분이 더 중요하다.
 * 0 허용의 근거는 같은 절의 제약 0~100,000이다.
 *
 * date가 문자열인 이유는 ApiDate의 주석에 있다. 형식 오류의 코드가 달라진다.
 */
public record RegisterInventoryRequest(
        @NotBlank String date,
        @NotNull @Min(0) @Max(100_000) Integer totalCount) {

    public LocalDate stayDate() {
        return ApiDate.parse("date", date);
    }
}
