package com.o2o.shared;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * V3의 상한 몫. 설계 근거: 11 응답 모델 DailyRate의 amount 1부터 1,000,000,000과 currency KRW 고정,
 * 계약 6절 P03.
 *
 * 하한 위반은 I2라 DailyRate가 막고 상한과 통화는 값 객체가 막는다. 둘을 가르는 근거는
 * 06-4 1-4다. 불변식은 애그리거트와 VO가 지키고, 그중 값 자체의 형태는 VO가 맡는다.
 */
class MoneyTest {

    @Test
    void 통화가_KRW가_아니면_거절한다() {
        assertThrows(IllegalArgumentException.class, () -> new Money(1000L, "USD"));
    }

    @Test
    void 통화가_KRW면_만든다() {
        Money money = Money.krw(1000L);

        assertEquals(1000L, money.amount());
        assertEquals("KRW", money.currency());
    }

    @Test
    void 상한을_넘으면_거절한다() {
        assertThrows(IllegalArgumentException.class, () -> Money.krw(1_000_000_001L));
    }

    @Test
    void 상한값은_만든다() {
        // 경계의 통과 케이스. 상한 자체는 허용 값이다
        assertEquals(1_000_000_000L, Money.krw(1_000_000_000L).amount());
    }
}
