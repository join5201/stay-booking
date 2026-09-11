package com.o2o.promotion.domain;

import java.time.Instant;

import com.o2o.shared.PromotionId;

/**
 * 프로모션이 등록됨. 설계 근거: 06-4 1-2 create 행의 Post 열 PromotionCreated 발행, 03 이벤트 행.
 *
 * 구독자는 없다. 계약 7절 D-1 나가 검색 프로젝션을 예약 묶음 뒤로 미뤘고 그 프로젝션이
 * 이 이벤트의 첫 구독자가 된다. 발행만 남기는 이유는 06-4 0절의 커밋 후 발행 규칙이
 * 애그리거트 행동의 Post에 적혀 있기 때문이다.
 */
public record PromotionCreated(PromotionId promotionId, boolean enabled, Instant occurredAt) {

    public static PromotionCreated of(Promotion promotion) {
        return new PromotionCreated(promotion.id(), promotion.enabled(), promotion.createdAt());
    }
}
