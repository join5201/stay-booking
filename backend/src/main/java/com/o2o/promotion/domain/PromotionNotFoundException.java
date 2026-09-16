package com.o2o.promotion.domain;

import com.o2o.shared.PromotionId;

/**
 * 설계 근거: 06-4 1-2 프로모션 계약표 update의 대상 존재 선행조건. 11 PROMO-03 조회의 404.
 *
 * 프로모션은 운영자의 자원이라 남의 자원이라는 개념이 없다(계약 2절). 그래서 이 예외는
 * 없는 자원 하나만 뜻한다.
 */
public class PromotionNotFoundException extends RuntimeException {

    public PromotionNotFoundException(PromotionId promotionId) {
        super("프로모션을 찾을 수 없다: " + promotionId.value());
    }
}
