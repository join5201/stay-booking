package com.o2o.promotion.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.o2o.shared.Money;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * V6과 V7과 V8. 설계 근거: I10과 I11과 I15(06-4 1-1), 06-4 118행 PricingService 계약 행,
 * 11 PROMO-05 처리 규칙 둘째 줄, 계약 6절 할인 단수 처리와 동률 후보 선택 행, T28.
 *
 * 스프링을 띄우지 않는다. 계산은 순수 함수라 단가 목록과 후보 목록만 있으면 된다. 계약 7절
 * D-3이 그은 경계대로 만들어 내는 쪽이 I10과 I11과 I15를 만족하는지를 여기서 본다. 검증하는
 * 쪽(Booking 생성자)은 예약 묶음이다.
 */
class PriceCalculationTest {

    private static final Instant NOW = Instant.parse("2026-10-01T00:00:00Z");
    private static final LocalDate CHECK_IN = LocalDate.parse("2026-10-10");
    private static final Condition ALL = Condition.of(LocalDate.parse("2026-10-01"),
            LocalDate.parse("2026-11-01"), null, null, 1, List.of());

    @Test
    void 날짜별_비례_내림_뒤_마지막_날짜에_가산해_배분_합계가_할인_총액과_같다() {
        // V6. 06-4 118행. 단가 33333, 33333, 33334에 10%면 할인 총액은 10000이다.
        // 날짜별 내림은 3333, 3333, 3333으로 9999라 단수 1이 남고 마지막 날짜가 3334가 된다.
        // 11 명세의 날짜별 floor 식이면 9999다. 계약 6절이 06-4를 골랐고 이 값이 그 결정이다
        Promotion 십퍼센트 = Promotion.create("10%", 10, ALL, true, NOW);

        PricingResult result = PriceCalculation.calculate(dates(3),
                List.of(33_333L, 33_333L, 33_334L), List.of(십퍼센트));
        PriceSnapshot snapshot = result.snapshot();

        assertEquals(10_000L, snapshot.discountTotalAmount());
        assertEquals(3_333L, snapshot.days().get(0).discountAmount());
        assertEquals(3_333L, snapshot.days().get(1).discountAmount());
        assertEquals(3_334L, snapshot.days().get(2).discountAmount());
        assertEquals(snapshot.discountTotalAmount(),
                snapshot.days().stream().mapToLong(PriceDay::discountAmount).sum());
    }

    @Test
    void 단수가_없으면_마지막_날짜에_더하는_것이_없다() {
        // V6의 짝. 가산 규칙이 항상 무언가를 더하는 코드면 여기서 잡힌다
        Promotion 십퍼센트 = Promotion.create("10%", 10, ALL, true, NOW);

        PriceSnapshot snapshot = PriceCalculation.calculate(dates(2),
                List.of(100_000L, 100_000L), List.of(십퍼센트)).snapshot();

        assertEquals(10_000L, snapshot.days().get(0).discountAmount());
        assertEquals(10_000L, snapshot.days().get(1).discountAmount());
        assertEquals(20_000L, snapshot.discountTotalAmount());
    }

    @Test
    void 상한_단가_30박에_99퍼센트도_넘치지_않고_배분_합계가_맞는다() {
        // V6의 상한 경계. 할인 총액 곱하기 단가가 long을 넘는 자리라 BigInteger를 쓴다.
        // 단가 상한은 Money의 10억이고 박수 상한은 StayRange의 30이다
        Promotion 최대 = Promotion.create("99%", 99, ALL, true, NOW);
        List<Long> 상한 = new ArrayList<>();
        for (int i = 0; i < StayRange.MAX_NIGHTS; i++) {
            상한.add(1_000_000_000L);
        }

        PriceSnapshot snapshot = PriceCalculation.calculate(dates(30), 상한, List.of(최대)).snapshot();

        assertEquals(30_000_000_000L, snapshot.baseTotalAmount());
        assertEquals(29_700_000_000L, snapshot.discountTotalAmount());
        assertEquals(snapshot.discountTotalAmount(),
                snapshot.days().stream().mapToLong(PriceDay::discountAmount).sum());
        assertEquals(300_000_000L, snapshot.totalAmount());
    }

    @Test
    void 스냅샷_행_수가_박수와_같고_총액이_날짜별_합과_같다() {
        // V7. I11 행수 일치, I15 총액 합치, 11 PriceSnapshot 표의 세 합계
        Promotion 십오퍼센트 = Promotion.create("15%", 15, ALL, true, NOW);
        List<Long> bases = List.of(80_000L, 90_000L, 100_000L, 110_000L);

        PriceSnapshot snapshot = PriceCalculation.calculate(dates(4), bases, List.of(십오퍼센트))
                .snapshot();

        assertEquals(4, snapshot.days().size());
        assertEquals(Money.KRW, snapshot.currency());
        assertEquals(380_000L, snapshot.baseTotalAmount());
        assertEquals(57_000L, snapshot.discountTotalAmount());
        assertEquals(323_000L, snapshot.totalAmount());
        assertEquals(snapshot.totalAmount(),
                snapshot.days().stream().mapToLong(PriceDay::finalAmount).sum());
        for (int i = 0; i < 4; i++) {
            PriceDay day = snapshot.days().get(i);
            assertEquals(CHECK_IN.plusDays(i), day.date());
            assertEquals(day.baseAmount() - day.discountAmount(), day.finalAmount());
        }
    }

    @Test
    void 후보가_없으면_할인이_0이고_적용_프로모션이_null이다() {
        // V7의 짝. 11 내부 처리 다섯째 줄
        PricingResult result = PriceCalculation.calculate(dates(2), List.of(50_000L, 50_000L),
                List.of());
        PriceSnapshot snapshot = result.snapshot();

        assertNull(snapshot.appliedPromotion());
        assertNull(result.selectedPromotionId());
        assertTrue(result.candidates().isEmpty());
        assertEquals(0L, snapshot.discountTotalAmount());
        assertEquals(100_000L, snapshot.totalAmount());
        assertEquals(0L, snapshot.days().get(1).discountAmount());
    }

    @Test
    void 할인액이_큰_하나를_적용하고_후보는_할인액_내림차순이다() {
        // 11 PROMO-05 처리 규칙 둘째 줄. 실제 할인액이 가장 큰 하나
        Promotion 십 = Promotion.create("10%", 10, ALL, true, NOW);
        Promotion 이십 = Promotion.create("20%", 20, ALL, true, NOW);

        PricingResult result = PriceCalculation.calculate(dates(2), List.of(50_000L, 50_000L),
                List.of(십, 이십));

        assertEquals(이십.id(), result.selectedPromotionId());
        assertEquals("20%", result.snapshot().appliedPromotion().name());
        assertEquals(20, result.snapshot().appliedPromotion().discountRate());
        assertEquals(2, result.candidates().size());
        assertEquals(이십.id(), result.candidates().get(0).id());
        assertTrue(result.candidates().get(0).selected());
        assertEquals(20_000L, result.candidates().get(0).discountAmount());
        assertFalse(result.candidates().get(1).selected());
        assertEquals(10_000L, result.candidates().get(1).discountAmount());
    }

    @Test
    void 동률이면_ID_오름차순으로_하나가_뽑히고_결과가_재현된다() {
        // V8. 계약 6절 동률 후보 선택 행. ID는 무작위라 둘 중 작은 쪽을 테스트가 고른다
        Promotion 갑 = Promotion.create("갑", 10, ALL, true, NOW);
        Promotion 을 = Promotion.create("을", 10, ALL, true, NOW);
        Promotion 작은ID = 갑.id().value().compareTo(을.id().value()) < 0 ? 갑 : 을;
        Promotion 큰ID = 작은ID == 갑 ? 을 : 갑;

        PricingResult 정순 = PriceCalculation.calculate(dates(2), List.of(50_000L, 50_000L),
                List.of(갑, 을));
        PricingResult 역순 = PriceCalculation.calculate(dates(2), List.of(50_000L, 50_000L),
                List.of(을, 갑));

        assertEquals(작은ID.id(), 정순.selectedPromotionId());
        assertEquals(작은ID.id(), 역순.selectedPromotionId());
        assertEquals(작은ID.id(), 정순.candidates().get(0).id());
        assertEquals(큰ID.id(), 정순.candidates().get(1).id());
        assertEquals(정순.snapshot(), 역순.snapshot());
        assertEquals(정순.candidates(), 역순.candidates());
    }

    private static List<LocalDate> dates(int nights) {
        return StayRange.of(CHECK_IN, CHECK_IN.plusDays(nights)).dates();
    }
}
