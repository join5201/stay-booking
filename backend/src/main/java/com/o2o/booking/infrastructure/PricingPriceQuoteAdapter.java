package com.o2o.booking.infrastructure;

import java.util.List;

import org.springframework.stereotype.Component;

import com.o2o.booking.application.PriceQuotePort;
import com.o2o.booking.domain.AppliedPromotion;
import com.o2o.booking.domain.DailyPrice;
import com.o2o.booking.domain.PriceSnapshot;
import com.o2o.booking.domain.RateNotConfiguredException;
import com.o2o.booking.domain.StayPeriod;
import com.o2o.promotion.domain.PricingService;
import com.o2o.shared.RoomTypeId;

/**
 * 가격 포트의 2차 어댑터. 프로모션 컨텍스트의 PricingService.quote를 감싸고 그 결과를 예약
 * 컨텍스트의 PriceSnapshot으로 옮긴다. 설계 근거: 2차 계약 2절 가격 포트 어댑터 행과 7절 D-4 가,
 * 1차 계약 7절 D-4(요금만 합산하던 어댑터를 2차에서 교체), 06-1 R5(프로모션이 상류, 예약이
 * 하류. ACL로 스냅샷을 받는다), 프로모션 계약 7절 D-3 가(PricingService는 예약 묶음이 재사용).
 *
 * 옮기는 쪽과 검증하는 쪽이 다르다(06-2 6절). 프로모션의 스냅샷은 계산 결과이고 예약의
 * PriceSnapshot.of가 I10, I11, I12, I15를 다시 본다. 두 컨텍스트가 같은 이름의 타입을 갖는
 * 이유이고, 이 클래스가 프로모션 타입을 예약 타입으로 바꾸는 유일한 자리다.
 *
 * 요금이 없는 날짜는 프로모션의 RateNotConfigured가 알리고 예약의 같은 이름 예외로 바꾼다(A6.
 * 1차 K8의 409 RATE_NOT_CONFIGURED 응답이 그대로다). 객실 타입 존재는 호출자가 먼저 본다.
 */
@Component
public class PricingPriceQuoteAdapter implements PriceQuotePort {

    private final PricingService pricingService;

    public PricingPriceQuoteAdapter(PricingService pricingService) {
        this.pricingService = pricingService;
    }

    @Override
    public PriceSnapshot quote(RoomTypeId roomTypeId, StayPeriod period) {
        com.o2o.promotion.domain.PriceSnapshot quoted;
        try {
            quoted = pricingService.quote(roomTypeId, period.checkIn(), period.checkOut());
        } catch (com.o2o.promotion.domain.RateNotConfiguredException e) {
            throw new RateNotConfiguredException(e.missingDates());
        }
        List<DailyPrice> days = quoted.days().stream()
                .map((day) -> new DailyPrice(day.date(), day.baseAmount(), day.discountAmount()))
                .toList();
        return PriceSnapshot.of(period, quoted.currency(), days, promotionOf(quoted),
                quoted.baseTotalAmount(), quoted.discountTotalAmount(), quoted.totalAmount());
    }

    /** 프로모션 ID 값 객체를 예약 스냅샷의 문자열 ID로. 할인이 없으면 null이다(I12의 반대쪽) */
    private static AppliedPromotion promotionOf(com.o2o.promotion.domain.PriceSnapshot quoted) {
        com.o2o.promotion.domain.AppliedPromotion applied = quoted.appliedPromotion();
        if (applied == null) {
            return null;
        }
        return new AppliedPromotion(applied.id().value(), applied.name(), applied.discountRate());
    }
}
