package com.o2o.promotion.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * PROMO-02 수정 내용. 설계 근거: 11 PROMO-02 필드표. version 외 아홉 필드가 전부 선택이고
 * 생략하면 기존 값을 유지한다.
 *
 * null이 유지다. 숙박 기간만 StayWindowChange로 세 갈래를 가른다. 그 이유는 그 타입에 있다.
 * version은 여기 없다. 대조는 Promotion.update의 인자로 따로 받는다. 카탈로그의 update와
 * 같은 모양이다.
 *
 * version 외 변경 필드가 한 개 이상이어야 한다는 규칙은 요청 형식이라 api 층이 본다(06-4 1-4).
 */
public record PromotionChanges(
        String name,
        Integer discountRate,
        LocalDate campaignStartDate,
        LocalDate campaignEndDate,
        StayWindowChange stayWindow,
        Integer minNights,
        List<String> regionCodes,
        Boolean enabled) {

    public PromotionChanges {
        Objects.requireNonNull(stayWindow, "stayWindow는 null일 수 없다. 유지는 Keep이다");
        regionCodes = regionCodes == null ? null : List.copyOf(regionCodes);
    }
}
