package com.o2o.promotion.domain;

import java.util.List;
import java.util.Objects;

import com.o2o.shared.Money;

/**
 * 가격 계산 결과. 설계 근거: 11 응답 모델 PriceSnapshot(currency, baseTotalAmount,
 * discountTotalAmount, totalAmount, appliedPromotion, days), 06-2 6절 PriceSnapshot VO 행.
 *
 * 계약 7절 D-3이 그은 경계가 이 타입의 모양이다. PricingService가 만들어 내는 쪽이고 I10과
 * I11과 I15를 검증하는 쪽은 예약 묶음의 Booking 생성자다. 그래서 이 레코드는 합계를 다시
 * 검산하지 않는다. 만들어 내는 쪽이 셋을 만족하는지는 계약 8-1절 V6과 V7이 본다.
 *
 * 다른 세션과의 접점 표가 이 타입을 quote의 반환 타입으로 적는다. 예약 묶음은 이 값을
 * 자기 소유 PriceSnapshot VO로 번역해 동결한다(06-1 R5의 ACL 성격).
 *
 * currency가 KRW 고정인 근거는 11 PriceSnapshot 표와 계약 6절 P03이다.
 */
public record PriceSnapshot(String currency, long baseTotalAmount, long discountTotalAmount,
                            long totalAmount, AppliedPromotion appliedPromotion,
                            List<PriceDay> days) {

    public PriceSnapshot {
        Objects.requireNonNull(currency, "currency는 null일 수 없다");
        Objects.requireNonNull(days, "days는 null일 수 없다");
        days = List.copyOf(days);
    }

    /** days 오름차순 N행에서 세 합계를 더해 만든다. 11 PriceSnapshot 표의 세 합계 행 */
    public static PriceSnapshot of(List<PriceDay> days, AppliedPromotion appliedPromotion) {
        long baseTotal = 0L;
        long discountTotal = 0L;
        long total = 0L;
        for (PriceDay day : days) {
            baseTotal += day.baseAmount();
            discountTotal += day.discountAmount();
            total += day.finalAmount();
        }
        return new PriceSnapshot(Money.KRW, baseTotal, discountTotal, total, appliedPromotion, days);
    }
}
