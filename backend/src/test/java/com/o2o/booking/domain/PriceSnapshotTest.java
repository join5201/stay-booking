package com.o2o.booking.domain;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * K2. 설계 근거: task-S9-booking 8-1절, 06-2 3-1 I10과 I11과 I12와 I15, 06-4 1-2 Booking 생성자의
 * 불변식 열(배분 합치, 행수 일치, 할인 근거, 총액 합치), 11 응답 모델 PriceSnapshot과 PriceDay.
 * 넷 각각 실패와 통과가 짝이다(testing.md T1).
 */
class PriceSnapshotTest {

    private static final LocalDate CHECK_IN = LocalDate.parse("2026-10-10");
    private static final StayPeriod TWO_NIGHTS = new StayPeriod(CHECK_IN, CHECK_IN.plusDays(2));
    private static final AppliedPromotion PROMOTION = new AppliedPromotion("promo_001", "가을 할인", 10);

    private static List<DailyPrice> days(long discount) {
        return List.of(new DailyPrice(CHECK_IN, 100_000, discount),
                new DailyPrice(CHECK_IN.plusDays(1), 100_000, discount));
    }

    @Test
    void K2_맞는_값은_통과하고_읽은_값이_그대로다() {
        PriceSnapshot snapshot = PriceSnapshot.of(TWO_NIGHTS, "KRW", days(10_000), PROMOTION,
                200_000, 20_000, 180_000);

        assertEquals(200_000L, snapshot.baseTotalAmount());
        assertEquals(20_000L, snapshot.discountTotalAmount());
        assertEquals(180_000L, snapshot.totalAmount());
        assertEquals(PROMOTION, snapshot.appliedPromotion().orElseThrow());
        assertEquals(2, snapshot.days().size());
        assertEquals(90_000L, snapshot.days().get(0).finalAmount());
    }

    @Test
    void K2_할인_없는_스냅샷은_프로모션_없이_통과한다() {
        PriceSnapshot snapshot = PriceSnapshot.of(TWO_NIGHTS, "KRW", days(0), null,
                200_000, 0, 200_000);

        assertTrue(snapshot.appliedPromotion().isEmpty());
        assertEquals(200_000L, snapshot.totalAmount());
    }

    @Test
    void K2_I10_할인_배분액_합이_할인_총액과_다르면_거절한다() {
        assertThrows(InvalidPriceSnapshotException.class, () -> PriceSnapshot.of(
                TWO_NIGHTS, "KRW", days(10_000), PROMOTION, 200_000, 15_000, 180_000));
    }

    @Test
    void K2_I11_행_수가_박수와_다르면_거절한다() {
        // 1박 스냅샷을 2박 기간에 붙이면 안 된다. 날짜가 어긋나도 마찬가지다
        List<DailyPrice> oneNight = List.of(new DailyPrice(CHECK_IN, 100_000, 0));
        assertThrows(InvalidPriceSnapshotException.class, () -> PriceSnapshot.of(
                TWO_NIGHTS, "KRW", oneNight, null, 100_000, 0, 100_000));

        List<DailyPrice> shifted = List.of(new DailyPrice(CHECK_IN.plusDays(1), 100_000, 0),
                new DailyPrice(CHECK_IN.plusDays(2), 100_000, 0));
        assertThrows(InvalidPriceSnapshotException.class, () -> PriceSnapshot.of(
                TWO_NIGHTS, "KRW", shifted, null, 200_000, 0, 200_000));
    }

    @Test
    void K2_I12_할인이_있는데_프로모션이_없으면_거절한다() {
        assertThrows(InvalidPriceSnapshotException.class, () -> PriceSnapshot.of(
                TWO_NIGHTS, "KRW", days(10_000), null, 200_000, 20_000, 180_000));
    }

    @Test
    void K2_I15_총액이_날짜별_최종액_합과_다르면_거절한다() {
        assertThrows(InvalidPriceSnapshotException.class, () -> PriceSnapshot.of(
                TWO_NIGHTS, "KRW", days(10_000), PROMOTION, 200_000, 20_000, 170_000));
        // 단가 합도 같은 자리에서 본다
        assertThrows(InvalidPriceSnapshotException.class, () -> PriceSnapshot.of(
                TWO_NIGHTS, "KRW", days(10_000), PROMOTION, 210_000, 20_000, 180_000));
    }

    @Test
    void K2_통화는_KRW만_쓴다() {
        // P03. 스냅샷도 요금과 같은 통화 규칙이다
        assertThrows(InvalidPriceSnapshotException.class, () -> PriceSnapshot.of(
                TWO_NIGHTS, "USD", days(0), null, 200_000, 0, 200_000));
    }

    @Test
    void K2_날짜별_가격_행은_단가_0과_단가를_넘는_할인을_거절한다() {
        assertThrows(InvalidPriceSnapshotException.class,
                () -> new DailyPrice(CHECK_IN, 0, 0));
        assertThrows(InvalidPriceSnapshotException.class,
                () -> new DailyPrice(CHECK_IN, 100_000, 100_001));
        assertEquals(0L, new DailyPrice(CHECK_IN, 100_000, 100_000).finalAmount());
    }

    @Test
    void K2_days는_바깥에서_바꿀_수_없다() {
        // I4의 절반. 나머지 절반(Booking이 스냅샷을 바꾸는 길이 없다)은 K3이다
        PriceSnapshot snapshot = PriceSnapshot.of(TWO_NIGHTS, "KRW", days(0), null,
                200_000, 0, 200_000);

        assertThrows(UnsupportedOperationException.class,
                () -> snapshot.days().add(new DailyPrice(CHECK_IN.plusDays(2), 1, 0)));
        assertEquals(2, snapshot.days().size());
    }
}
