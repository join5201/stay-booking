package com.o2o.promotion.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.o2o.shared.VersionConflictException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * V2와 V1의 명세 몫과 V5의 상태 몫과 V10의 도메인 몫. 설계 근거: 11 PROMO-01 필드표(1141행
 * discountRate, 캠페인 끝 날짜는 시작보다 뒤), 06-4 1-2 프로모션 계약표 create와 update와
 * isApplicable, 11 PROMO-02 처리 규칙, 계약 7절 D-2 다.
 *
 * 스프링을 띄우지 않는다. 할인율 범위와 버전 대조와 합친 상태 검사는 애그리거트가 스스로
 * 하는 일이다(06-4 1-4). 시각은 인자로 받으므로 상수다.
 */
class PromotionTest {

    private static final Instant NOW = Instant.parse("2026-10-01T00:00:00Z");
    private static final LocalDate START = LocalDate.parse("2026-10-01");
    private static final LocalDate END = LocalDate.parse("2026-11-01");
    private static final Condition CONDITION = Condition.of(START, END, null, null, 1, List.of());

    @Test
    void 할인율_0과_100은_거절한다() {
        // V2
        assertThrows(InvalidDiscountRateException.class,
                () -> Promotion.create("가을 할인", 0, CONDITION, true, NOW));
        assertThrows(InvalidDiscountRateException.class,
                () -> Promotion.create("가을 할인", 100, CONDITION, true, NOW));
    }

    @Test
    void 할인율_1과_99는_통과한다() {
        // V2의 짝. 경계 양쪽
        assertEquals(1, Promotion.create("가을 할인", 1, CONDITION, true, NOW).discountRate());
        assertEquals(99, Promotion.create("가을 할인", 99, CONDITION, true, NOW).discountRate());
    }

    @Test
    void 캠페인_시작일과_종료일이_같으면_등록을_거절한다() {
        // V1의 명세 몫. Condition은 같은 날짜를 통과시키고(I8) 루트가 11 명세의 하루 이상을 본다.
        // 시작 포함 끝 제외라 같은 날짜는 하루도 없는 기간이다
        Condition 같은날 = Condition.of(START, START, null, null, 1, List.of());

        assertThrows(InvalidPeriodException.class,
                () -> Promotion.create("가을 할인", 10, 같은날, true, NOW));
    }

    @Test
    void 숙박_기간_시작일과_종료일이_같으면_등록을_거절한다() {
        // 11 PROMO-01 필드표 stayEndDate 행. 시작보다 뒤
        Condition 같은날 = Condition.of(START, END, START, START, 1, List.of());

        assertThrows(InvalidPeriodException.class,
                () -> Promotion.create("가을 할인", 10, 같은날, true, NOW));
    }

    @Test
    void 등록하면_버전_0과_생성_시각을_갖는다() {
        Promotion promotion = Promotion.create("가을 할인", 10, CONDITION, true, NOW);

        assertEquals(0L, promotion.version());
        assertEquals(NOW, promotion.createdAt());
        assertEquals(NOW, promotion.updatedAt());
        assertTrue(promotion.id().value().startsWith("promo_"));
    }

    @Test
    void 지난_버전으로_수정하면_거절하고_값을_유지한다() {
        // V9의 도메인 몫. 11 공통 절 version 규칙
        Promotion promotion = Promotion.create("가을 할인", 10, CONDITION, true, NOW);
        promotion.update(0L, 변경(null, false), NOW.plusSeconds(60));

        assertThrows(VersionConflictException.class,
                () -> promotion.update(0L, 변경(20, null), NOW.plusSeconds(120)));
        assertEquals(10, promotion.discountRate());
        assertFalse(promotion.enabled());
        assertEquals(1L, promotion.version());
    }

    @Test
    void 맞는_버전으로_수정하면_버전이_오르고_생략한_필드는_유지된다() {
        // 11 PROMO-02 처리 규칙. 생략한 필드는 기존 값 유지
        Promotion promotion = Promotion.create("가을 할인", 10, CONDITION, true, NOW);

        promotion.update(0L, 변경(20, null), NOW.plusSeconds(60));

        assertEquals(20, promotion.discountRate());
        assertTrue(promotion.enabled());
        assertEquals("가을 할인", promotion.name());
        assertEquals(1L, promotion.version());
        assertEquals(NOW.plusSeconds(60), promotion.updatedAt());
        assertEquals(NOW, promotion.createdAt());
    }

    @Test
    void 합친_최종_상태에서_캠페인_순서가_어긋나면_거절하고_값을_유지한다() {
        // V10의 도메인 몫. 종료일만 시작일보다 앞으로 보내면 합친 상태가 I8을 어긴다
        Promotion promotion = Promotion.create("가을 할인", 10, CONDITION, true, NOW);
        PromotionChanges 종료일만 = new PromotionChanges(null, null, null, START.minusDays(1),
                StayWindowChange.keep(), null, null, null);

        assertThrows(InvalidPeriodException.class,
                () -> promotion.update(0L, 종료일만, NOW.plusSeconds(60)));
        assertEquals(END, promotion.condition().campaignEndDate());
        assertEquals(0L, promotion.version());
    }

    @Test
    void 합친_최종_상태에서_할인율이_범위_밖이면_거절한다() {
        // V2의 수정 몫. 등록에서만 막고 수정에서 뚫리면 안 된다
        Promotion promotion = Promotion.create("가을 할인", 10, CONDITION, true, NOW);

        assertThrows(InvalidDiscountRateException.class,
                () -> promotion.update(0L, 변경(100, null), NOW.plusSeconds(60)));
        assertEquals(10, promotion.discountRate());
    }

    @Test
    void 숙박_기간을_바꾸고_해제할_수_있다() {
        // 11 PROMO-02 처리 규칙 첫 줄. 두 날짜를 함께 보낸다
        Promotion promotion = Promotion.create("가을 할인", 10, CONDITION, true, NOW);

        promotion.update(0L, new PromotionChanges(null, null, null, null,
                StayWindowChange.replace(StayWindow.of(START, END)), null, null, null),
                NOW.plusSeconds(60));
        assertEquals(START, promotion.condition().stayWindow().start());

        promotion.update(1L, new PromotionChanges(null, null, null, null,
                StayWindowChange.clear(), null, null, null), NOW.plusSeconds(120));
        assertNull(promotion.condition().stayWindow());
        assertEquals(2L, promotion.version());
    }

    @Test
    void enabled가_거짓이면_조건이_맞아도_적용_불가다() {
        // V5의 상태 몫. 06-2 6절 CRC isApplicable 행의 상태 조건을 D-2 다에 따라 enabled로 본다
        Promotion 사용 = Promotion.create("가을 할인", 10, CONDITION, true, NOW);
        Promotion 미사용 = Promotion.create("가을 할인", 10, CONDITION, false, NOW);
        StayRange stay = StayRange.of(START.plusDays(5), START.plusDays(7));

        assertTrue(사용.isApplicable("SEOUL", stay, START));
        assertFalse(미사용.isApplicable("SEOUL", stay, START));
    }

    private static PromotionChanges 변경(Integer discountRate, Boolean enabled) {
        return new PromotionChanges(null, discountRate, null, null, StayWindowChange.keep(),
                null, null, enabled);
    }
}
