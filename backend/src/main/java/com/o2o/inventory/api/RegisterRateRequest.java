package com.o2o.inventory.api;

import java.time.LocalDate;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * RATE-01 요청 body. 설계 근거: 11 요금 RATE-01의 필드표.
 *
 * currency를 받고 KRW만 통과시킨다. 같은 절이 필수이면서 KRW 고정이라 적는다. 값 객체
 * Money도 같은 검사를 한다. 두 번 하는 것이 중복이 아닌 이유는 06-4 1-4다. 이 층은 요청
 * 형식을 보고 그 층은 불변식을 지킨다. 이 층을 지우면 다른 입구가 열렸을 때 뚫린다.
 *
 * amount의 1과 10억도 같은 자리다. 하한 1은 불변식 I2와 겹치지만 여기 검사는 형식이다.
 */
public record RegisterRateRequest(
        @NotBlank String date,
        @NotNull @Min(1) @Max(1_000_000_000L) Long amount,
        @NotBlank @Pattern(regexp = "KRW", message = "통화는 KRW만 쓴다") String currency) {

    public LocalDate stayDate() {
        return ApiDate.parse("date", date);
    }
}
