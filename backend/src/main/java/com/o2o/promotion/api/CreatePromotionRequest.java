package com.o2o.promotion.api;

import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * PROMO-01 요청 body. 설계 근거: 11 PROMO-01 필드표 아홉 칸.
 *
 * 날짜를 문자열로 받는 이유는 shared의 ApiDate 주석에 있다. 형식 오류가 400 INVALID_DATE_RANGE로
 * 나가야 한다. 숙박 기간 두 날짜의 함께 오거나 둘 다 없음은 도메인(Condition)이 본다. 그 규칙은
 * 형식이 아니라 값의 관계라 한 자리에 둔다. 지역 코드가 등록된 것인지는 보지 않는다. 카탈로그의
 * Region과 같은 이유다(지역 fixture 없음).
 */
public record CreatePromotionRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull @Min(1) @Max(99) Integer discountRate,
        @NotNull String campaignStartDate,
        @NotNull String campaignEndDate,
        String stayStartDate,
        String stayEndDate,
        @NotNull @Min(1) @Max(30) Integer minNights,
        @NotNull @Size(max = 100) List<@NotBlank @Size(max = 32) String> regionCodes,
        Boolean enabled) {

    /** 11 PROMO-01 필드표 enabled 행. 생략하면 true */
    public boolean enabledOrTrue() {
        return enabled == null || enabled;
    }
}
