package com.o2o.promotion.domain;

import java.util.Objects;

import com.o2o.shared.PromotionId;

/**
 * 적용한 프로모션의 복사본. 설계 근거: 11 응답 모델 AppliedPromotion(id, name, discountRate),
 * 06-2 6절 PriceSnapshot VO 행의 적용 프로모션의 식별자와 이름.
 *
 * 이름까지 복사하는 이유는 06-1 R5다. 프로모션에 인스턴스가 없어 적용 사실이 스냅샷에만
 * 남는다. 예약 생성 뒤 프로모션 이름이 바뀌어도 이 값은 그대로다(11 내부 처리 일곱째 줄).
 */
public record AppliedPromotion(PromotionId id, String name, int discountRate) {

    public AppliedPromotion {
        Objects.requireNonNull(id, "id는 null일 수 없다");
        Objects.requireNonNull(name, "name은 null일 수 없다");
    }

    public static AppliedPromotion of(Promotion promotion) {
        return new AppliedPromotion(promotion.id(), promotion.name(), promotion.discountRate());
    }
}
