package com.o2o.shared;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * 금액 값 객체. 설계 근거: 06-1 4절 공유 커널 후보의 Money 행, 06-2 1절 DailyRate 내부 요소.
 *
 * 06-1 4절이 Money를 적합[가설]로 적고 조건을 할인 배분의 반올림 규칙 미정으로 달았다.
 * 그 조건은 05-3 60행 할인 배분 행이 v10에서 비례 내림 후 마지막 날짜 행에 가산으로
 * 확정하면서 풀렸다. 다만 배분 자체는 예약 컨텍스트의 PriceSnapshot이 하는 일이라
 * 이 타입에 배분 메서드를 두지 않는다. 재고와 요금 묶음에 필요한 것은 단가 한 값이다.
 *
 * 통화를 KRW로 고정하는 근거는 11 응답 모델 DailyRate의 currency 행과
 * task-S9-inventory-rate 6절 P03이다. 원 단위 정수라 소수점을 쓰지 않는다.
 */
@Embeddable
public record Money(@Column(name = "amount", nullable = false) long amount,
                    @Column(name = "currency", nullable = false, length = 3) String currency) {

    public static final String KRW = "KRW";

    private static final long MAX_AMOUNT = 1_000_000_000L;

    public Money {
        Objects.requireNonNull(currency, "currency는 null일 수 없다");
        if (!KRW.equals(currency)) {
            throw new IllegalArgumentException("통화는 KRW만 쓴다: " + currency);
        }
        if (amount > MAX_AMOUNT) {
            throw new IllegalArgumentException("금액 상한을 넘었다: " + amount);
        }
    }

    public static Money krw(long amount) {
        return new Money(amount, KRW);
    }
}
