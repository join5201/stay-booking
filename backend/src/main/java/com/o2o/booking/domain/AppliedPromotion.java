package com.o2o.booking.domain;

import java.util.Objects;

/**
 * 스냅샷에 고정한 프로모션. 설계 근거: 06-2 1절 PriceSnapshot VO의 promotionId와 promotionName,
 * 11 응답 모델 AppliedPromotion(id, name, discountRate). 이름까지 고정하는 이유는 나중에
 * 프로모션이 바뀌어도 예약이 보여 주는 값이 그대로여야 해서다(I4, T13).
 * 1차의 가격 어댑터는 프로모션을 모르므로 이 값을 만들지 않는다(계약 7절 D-4). 2차가 채운다.
 */
public record AppliedPromotion(String id, String name, int discountRate) {

    public AppliedPromotion {
        Objects.requireNonNull(id, "promotionId는 null일 수 없다");
        Objects.requireNonNull(name, "promotionName은 null일 수 없다");
        if (id.isBlank() || id.length() > 64) {
            throw new InvalidPriceSnapshotException("promotionId는 1자 이상 64자 이하여야 한다: " + id);
        }
        if (discountRate < 1 || discountRate > 100) {
            throw new InvalidPriceSnapshotException("discountRate는 1 이상 100 이하여야 한다: "
                    + discountRate);
        }
    }
}
