package com.o2o.promotion.application;

import java.time.Instant;
import java.util.List;

import com.o2o.promotion.domain.PromotionCandidate;
import com.o2o.promotion.domain.StayRange;
import com.o2o.shared.GuestCount;
import com.o2o.shared.PromotionId;
import com.o2o.shared.RoomTypeId;

/**
 * PROMO-05의 결과. 설계 근거: 11 응답 모델 ApplicablePromotions 일곱 칸.
 *
 * evaluatedAt이 여기 있는 이유는 판정 시각이 응용 층의 Clock에서 나오기 때문이다. 도메인의
 * PricingResult는 시각을 모른다.
 */
public record ApplicablePromotionsResult(RoomTypeId roomTypeId, StayRange stay,
                                         GuestCount guestCount, Instant evaluatedAt,
                                         List<PromotionCandidate> items,
                                         PromotionId selectedPromotionId) {
}
