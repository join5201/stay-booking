package com.o2o.promotion.api;

import java.util.List;

import com.o2o.promotion.domain.AppliedPromotion;
import com.o2o.promotion.domain.PriceDay;
import com.o2o.promotion.domain.PriceSnapshot;
import com.o2o.shared.ApiDate;

/**
 * 응답 모델 PriceSnapshot. 설계 근거: 11 응답 모델 PriceSnapshot 여섯 칸, PriceDay 네 칸,
 * AppliedPromotion 세 칸. appliedPromotion은 후보가 없으면 null이다.
 *
 * promotion/api에 두는 이유는 PriceSnapshot이 이 컨텍스트의 도메인 값이기 때문이다. 첫 사용처는
 * 검색의 SEARCH-03이고 예약 응답의 priceSnapshot이 두 번째 사용처가 된다. 그때 shared로 올릴지
 * 판단한다(backend/CLAUDE.md 3-2).
 */
public record PriceSnapshotResponse(String currency, long baseTotalAmount, long discountTotalAmount,
                                    long totalAmount, AppliedPromotionResponse appliedPromotion,
                                    List<PriceDayResponse> days) {

    public record PriceDayResponse(String date, long baseAmount, long discountAmount,
                                   long finalAmount) {

        static PriceDayResponse from(PriceDay day) {
            return new PriceDayResponse(ApiDate.format(day.date()), day.baseAmount(),
                    day.discountAmount(), day.finalAmount());
        }
    }

    public record AppliedPromotionResponse(String id, String name, int discountRate) {

        static AppliedPromotionResponse from(AppliedPromotion applied) {
            return applied == null ? null
                    : new AppliedPromotionResponse(applied.id().value(), applied.name(),
                            applied.discountRate());
        }
    }

    public static PriceSnapshotResponse from(PriceSnapshot snapshot) {
        return new PriceSnapshotResponse(snapshot.currency(), snapshot.baseTotalAmount(),
                snapshot.discountTotalAmount(), snapshot.totalAmount(),
                AppliedPromotionResponse.from(snapshot.appliedPromotion()),
                snapshot.days().stream().map(PriceDayResponse::from).toList());
    }
}
