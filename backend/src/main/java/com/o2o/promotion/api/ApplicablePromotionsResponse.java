package com.o2o.promotion.api;

import java.util.List;

import com.o2o.promotion.application.ApplicablePromotionsResult;
import com.o2o.promotion.domain.PromotionCandidate;
import com.o2o.shared.ApiDate;
import com.o2o.shared.ApiTime;

/**
 * 응답 모델 ApplicablePromotions. 설계 근거: 11 응답 모델 ApplicablePromotions 일곱 칸과
 * ApplicablePromotion 다섯 칸. items는 할인액 내림차순, 동률이면 ID 오름차순이고 없으면 빈
 * 배열, selectedPromotionId는 없으면 null이다.
 */
public record ApplicablePromotionsResponse(String roomTypeId, String checkIn, String checkOut,
                                           int guestCount, String evaluatedAt,
                                           List<Item> items, String selectedPromotionId) {

    public record Item(String id, String name, int discountRate, long discountAmount,
                       boolean selected) {

        static Item from(PromotionCandidate candidate) {
            return new Item(candidate.id().value(), candidate.name(), candidate.discountRate(),
                    candidate.discountAmount(), candidate.selected());
        }
    }

    public static ApplicablePromotionsResponse from(ApplicablePromotionsResult result) {
        return new ApplicablePromotionsResponse(
                result.roomTypeId().value(),
                ApiDate.format(result.stay().checkIn()),
                ApiDate.format(result.stay().checkOut()),
                result.guestCount().value(),
                ApiTime.format(result.evaluatedAt()),
                result.items().stream().map(Item::from).toList(),
                result.selectedPromotionId() == null ? null : result.selectedPromotionId().value());
    }
}
