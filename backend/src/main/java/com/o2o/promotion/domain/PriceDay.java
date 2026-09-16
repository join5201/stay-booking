package com.o2o.promotion.domain;

import java.time.LocalDate;
import java.util.Objects;

/**
 * 날짜별 가격 행. 설계 근거: 11 응답 모델 PriceDay(date, baseAmount, discountAmount, finalAmount),
 * 06-2 6절 PriceSnapshot VO 행의 DailyPrice VO. 이름은 11 명세를 따른다.
 *
 * finalAmount는 baseAmount - discountAmount다(11 PriceDay 표). 셋을 다 들고 다니는 이유는
 * 예약 묶음이 이 행을 그대로 저장하고 I15를 저장된 값으로 다시 검증하기 때문이다.
 */
public record PriceDay(LocalDate date, long baseAmount, long discountAmount, long finalAmount) {

    public PriceDay {
        Objects.requireNonNull(date, "date는 null일 수 없다");
    }

    /** 최종액을 명세 식으로 계산해 만든다. 06-4 118행의 배분 결과가 여기 들어온다 */
    public static PriceDay of(LocalDate date, long baseAmount, long discountAmount) {
        return new PriceDay(date, baseAmount, discountAmount, baseAmount - discountAmount);
    }
}
