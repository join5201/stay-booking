package com.o2o.promotion.domain;

import java.time.Instant;

import com.o2o.shared.PromotionId;

/**
 * 프로모션이 수정됨. 설계 근거: 06-4 1-2 update 행의 Post 열 PromotionUpdated 발행, 03 이벤트 행.
 *
 * 같은 Post 열이 기존 예약 스냅샷 무영향도 적는다. 그 보장은 이벤트가 아니라 예약이
 * 생성 시점 금액을 복사해 두는 것으로 선다(11 내부 처리 가격과 프로모션 절 일곱째 줄).
 * enabled를 싣는 이유는 11 PROMO-02 처리 규칙이 enabled=false를 수동 종료로 적기 때문이다.
 */
public record PromotionUpdated(PromotionId promotionId, boolean enabled, long version,
                               Instant occurredAt) {

    public static PromotionUpdated of(Promotion promotion) {
        return new PromotionUpdated(promotion.id(), promotion.enabled(), promotion.version(),
                promotion.updatedAt());
    }
}
