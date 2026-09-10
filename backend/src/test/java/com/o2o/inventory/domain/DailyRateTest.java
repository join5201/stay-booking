package com.o2o.inventory.domain;

import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.o2o.shared.Money;
import com.o2o.shared.RoomTypeId;
import com.o2o.shared.VersionConflictException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * V3. 설계 근거: I2 (06-2 3-1, 06-4 1-1). 요금은 0보다 크다.
 * 06-4 1-2 registerRate와 adjustRate의 Invariant 열이 둘 다 요금 양수를 적는다.
 *
 * 재고와 다른 점을 여기서 확인한다. 재고의 총 수량은 0을 허용하고 요금은 허용하지 않는다.
 * 근거가 다르다. 재고 0은 11 명세 139행이 허용하고 요금 양수는 불변식이다.
 */
class DailyRateTest {

    private static final Instant NOW = Instant.parse("2026-10-01T00:00:00Z");
    private static final RoomTypeId ROOM_TYPE_ID = RoomTypeId.of("room_test");
    private static final LocalDate STAY_DATE = LocalDate.parse("2026-10-10");

    @Test
    void 요금이_0이면_거절한다() {
        assertThrows(InvalidRateException.class,
                () -> DailyRate.register(ROOM_TYPE_ID, STAY_DATE, Money.krw(0), NOW));
    }

    @Test
    void 요금이_음수면_거절한다() {
        assertThrows(InvalidRateException.class,
                () -> DailyRate.register(ROOM_TYPE_ID, STAY_DATE, Money.krw(-1), NOW));
    }

    @Test
    void 요금이_1이면_등록한다() {
        // 하한의 통과 케이스. 11 명세가 amount를 1 이상으로 적는다
        DailyRate rate = DailyRate.register(ROOM_TYPE_ID, STAY_DATE, Money.krw(1), NOW);

        assertEquals(1L, rate.rate().amount());
        assertEquals(Money.KRW, rate.rate().currency());
        assertEquals(0L, rate.version());
    }

    @Test
    void 조정하면_금액과_버전이_바뀐다() {
        DailyRate rate = DailyRate.register(ROOM_TYPE_ID, STAY_DATE, Money.krw(100_000), NOW);

        rate.adjust(0L, 120_000, NOW.plusSeconds(60));

        assertEquals(120_000L, rate.rate().amount());
        assertEquals(1L, rate.version());
    }

    @Test
    void 조정에서도_0을_거절한다() {
        // 06-4 1-2 adjustRate의 Invariant도 요금 양수다. 등록에서만 막고 조정에서 뚫리면 안 된다
        DailyRate rate = DailyRate.register(ROOM_TYPE_ID, STAY_DATE, Money.krw(100_000), NOW);

        assertThrows(InvalidRateException.class, () -> rate.adjust(0L, 0, NOW.plusSeconds(60)));
        assertEquals(100_000L, rate.rate().amount());
        assertEquals(0L, rate.version());
    }

    @Test
    void 지난_버전으로_조정하면_거절한다() {
        // T05의 요금 몫
        DailyRate rate = DailyRate.register(ROOM_TYPE_ID, STAY_DATE, Money.krw(100_000), NOW);
        rate.adjust(0L, 120_000, NOW.plusSeconds(60));

        assertThrows(VersionConflictException.class,
                () -> rate.adjust(0L, 130_000, NOW.plusSeconds(120)));
        assertEquals(120_000L, rate.rate().amount());
    }
}
