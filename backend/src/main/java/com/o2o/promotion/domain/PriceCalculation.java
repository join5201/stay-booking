package com.o2o.promotion.domain;

import java.math.BigInteger;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 가격 계산 규칙. 설계 근거: 06-4 118행 PricingService 행(단수는 날짜별 비례 내림 후 마지막
 * 날짜 행 가산), 11 내부 처리 가격과 프로모션 절, 계약 6절 할인 단수 처리와 동률 후보 선택 행.
 *
 * PricingService에서 순수 계산만 떼어 낸 자리다. 리포지토리 없이 단가 목록과 후보 목록만
 * 받아서 계약 8-1절 V6과 V7과 V8을 DB 없이 검사할 수 있다(backend/CLAUDE.md 테스트 규칙의
 * 단위 테스트 행).
 *
 * 할인 총액은 floor(baseTotal * rate / 100)이고 날짜별 배분은 floor(할인 총액 * base_i /
 * baseTotal), 남는 단수는 마지막 날짜에 더한다. 11 명세 넷째 줄은 날짜별 floor를 적는데
 * 계약 6절이 06-4 118행을 따르기로 확정했다. 두 식은 총액이 다를 수 있고 이 클래스는 계약을
 * 따른다. BigInteger를 쓰는 이유는 할인 총액(최대 30박 곱하기 상한 10억)에 단가를 곱하면
 * long을 넘기 때문이다.
 */
public final class PriceCalculation {

    private static final int PERCENT = 100;

    private PriceCalculation() {
    }

    /**
     * dates와 baseAmounts는 날짜 오름차순 같은 길이이고 비어 있지 않다(A6은 호출자가 본다).
     * applicable은 isApplicable이 참인 프로모션이고 순서는 상관없다.
     */
    public static PricingResult calculate(List<LocalDate> dates, List<Long> baseAmounts,
                                          List<Promotion> applicable) {
        if (dates.size() != baseAmounts.size() || dates.isEmpty()) {
            throw new IllegalArgumentException("날짜와 단가의 수가 다르거나 비어 있다");
        }
        long baseTotal = 0L;
        for (long base : baseAmounts) {
            baseTotal += base;
        }

        // 11 PROMO-05 처리 규칙 둘째 줄. 할인액 내림차순, 동률이면 ID 오름차순. 첫 행이 선택이다
        List<Ranked> ranked = new ArrayList<>();
        for (Promotion promotion : applicable) {
            ranked.add(new Ranked(promotion, discountTotal(baseTotal, promotion.discountRate())));
        }
        ranked.sort(Comparator.comparingLong(Ranked::discountAmount).reversed()
                .thenComparing(r -> r.promotion().id().value()));

        Ranked selected = ranked.isEmpty() ? null : ranked.get(0);
        long discountTotal = selected == null ? 0L : selected.discountAmount();
        List<PriceDay> days = allocate(dates, baseAmounts, baseTotal, discountTotal);
        AppliedPromotion applied = selected == null ? null : AppliedPromotion.of(selected.promotion());

        List<PromotionCandidate> candidates = new ArrayList<>();
        for (Ranked r : ranked) {
            candidates.add(new PromotionCandidate(r.promotion().id(), r.promotion().name(),
                    r.promotion().discountRate(), r.discountAmount(), r == selected));
        }
        return new PricingResult(PriceSnapshot.of(days, applied), candidates);
    }

    /** 06-4 118행. 전체 숙박 기준 할인 총액을 먼저 내림한다 */
    static long discountTotal(long baseTotal, int discountRate) {
        return baseTotal * discountRate / PERCENT;
    }

    /** 06-4 118행. 날짜별 비례 내림 후 마지막 날짜 행 가산. 합이 할인 총액과 같다(I10, V6) */
    static List<PriceDay> allocate(List<LocalDate> dates, List<Long> baseAmounts,
                                   long baseTotal, long discountTotal) {
        int n = dates.size();
        long[] allocated = new long[n];
        long allocatedSum = 0L;
        BigInteger total = BigInteger.valueOf(discountTotal);
        BigInteger denominator = BigInteger.valueOf(baseTotal);
        for (int i = 0; i < n; i++) {
            allocated[i] = total.multiply(BigInteger.valueOf(baseAmounts.get(i)))
                    .divide(denominator).longValueExact();
            allocatedSum += allocated[i];
        }
        allocated[n - 1] += discountTotal - allocatedSum;

        List<PriceDay> days = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            days.add(PriceDay.of(dates.get(i), baseAmounts.get(i), allocated[i]));
        }
        return days;
    }

    private record Ranked(Promotion promotion, long discountAmount) {
    }
}
