package com.o2o.promotion.api;

import java.util.List;

import com.o2o.promotion.domain.Promotion;
import com.o2o.promotion.domain.StayWindow;
import com.o2o.shared.ApiDate;
import com.o2o.shared.ApiTime;

/**
 * 응답 모델 Promotion. 설계 근거: 11 응답 모델 Promotion 열세 칸.
 *
 * 도메인 객체를 그대로 직렬화하지 않고 따로 두는 이유는 PropertyResponse와 같다. 11이 응답
 * 모양을 정하고 도메인은 규칙을 정한다. 숙박 기간 제한이 없으면 두 날짜가 null이다.
 */
public record PromotionResponse(String id, String name, int discountRate,
                                String campaignStartDate, String campaignEndDate,
                                String stayStartDate, String stayEndDate, int minNights,
                                List<String> regionCodes, boolean enabled, long version,
                                String createdAt, String updatedAt) {

    public static PromotionResponse from(Promotion promotion) {
        StayWindow window = promotion.condition().stayWindow();
        return new PromotionResponse(
                promotion.id().value(),
                promotion.name(),
                promotion.discountRate(),
                ApiDate.format(promotion.condition().campaignStartDate()),
                ApiDate.format(promotion.condition().campaignEndDate()),
                window == null ? null : ApiDate.format(window.start()),
                window == null ? null : ApiDate.format(window.end()),
                promotion.condition().minNights(),
                promotion.condition().regionCodes(),
                promotion.enabled(),
                promotion.version(),
                ApiTime.format(promotion.createdAt()),
                ApiTime.format(promotion.updatedAt()));
    }
}
