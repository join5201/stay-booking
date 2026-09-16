package com.o2o.promotion.domain;

import java.util.List;
import java.util.Objects;

import com.o2o.shared.PromotionId;

/**
 * PricingService의 반환값. 설계 근거: 06-2 6절 PricingService 행(DailyPrice 행 N개와 할인
 * 배분), 11 응답 모델 ApplicablePromotions의 items와 selectedPromotionId.
 *
 * 스냅샷 하나와 후보 목록을 함께 돌려주는 이유는 PROMO-05가 후보 전부를 보여 주고 SEARCH-03과
 * 예약이 선택된 하나만 쓰기 때문이다. 한 번의 계산에서 둘 다 나온다. 계산을 두 번 하면
 * 11 내부 처리 여섯째 줄(검색, 예상 금액, 예약이 같은 계산 규칙)을 두 코드로 지켜야 한다.
 *
 * candidates는 할인액 내림차순, 동률이면 ID 오름차순이고 첫 행이 선택된 행이다(11 명세
 * PROMO-05 처리 규칙, 계약 6절 동률 후보 선택 행).
 */
public record PricingResult(PriceSnapshot snapshot, List<PromotionCandidate> candidates) {

    public PricingResult {
        Objects.requireNonNull(snapshot, "snapshot은 null일 수 없다");
        Objects.requireNonNull(candidates, "candidates는 null일 수 없다");
        candidates = List.copyOf(candidates);
    }

    /** 후보가 없으면 null이다. 11 PROMO-05 처리 규칙 넷째 줄 */
    public PromotionId selectedPromotionId() {
        AppliedPromotion applied = snapshot.appliedPromotion();
        return applied == null ? null : applied.id();
    }
}
