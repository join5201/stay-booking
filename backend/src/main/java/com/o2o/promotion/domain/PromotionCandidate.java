package com.o2o.promotion.domain;

import java.util.Objects;

import com.o2o.shared.PromotionId;

/**
 * 적용 가능 후보 한 건. 설계 근거: 11 응답 모델 ApplicablePromotion(id, name, discountRate,
 * discountAmount, selected). discountAmount는 전체 숙박의 할인액이다.
 */
public record PromotionCandidate(PromotionId id, String name, int discountRate,
                                 long discountAmount, boolean selected) {

    public PromotionCandidate {
        Objects.requireNonNull(id, "id는 null일 수 없다");
        Objects.requireNonNull(name, "name은 null일 수 없다");
    }
}
