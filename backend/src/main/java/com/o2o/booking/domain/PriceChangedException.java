package com.o2o.booking.domain;

/**
 * 예상 총액 불일치. 설계 근거: 11 BOOK-01 처리 규칙(예상 총액이 다르면 PRICE_CHANGED이며 예약과
 * Hold는 만들지 않는다), P06, 11 내부 처리 가격과 프로모션 절. 409 PRICE_CHANGED. 검증 항목은
 * T12다. 1차는 요금 절반이고 프로모션 절반은 2차다.
 */
public class PriceChangedException extends RuntimeException {

    public PriceChangedException(long expectedTotalAmount, long actualTotalAmount) {
        super("예상 총액 " + expectedTotalAmount + "이 서버 금액 " + actualTotalAmount + "과 다르다");
    }
}
