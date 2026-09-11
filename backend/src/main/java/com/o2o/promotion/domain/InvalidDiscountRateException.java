package com.o2o.promotion.domain;

/**
 * 할인율 범위 위반. 설계 근거: 11 PROMO-01 필드표 discountRate 행의 1~99. 계약 8-1절 V2.
 *
 * 06-4 1-1 불변식 표에 할인율 행은 없다. 그래도 도메인이 막는 이유는 0퍼센트는 할인이 아니고
 * 100퍼센트는 총액을 0으로 만들기 때문이다. 이 규칙이 API 층에만 있으면 다른 입구가 생길 때
 * 뚫린다. 컨트롤러의 Min과 Max는 요청 형식을 보고 여기는 규칙을 본다(06-4 1-4).
 */
public class InvalidDiscountRateException extends RuntimeException {

    public InvalidDiscountRateException(int discountRate) {
        super("할인율은 1 이상 99 이하여야 한다: " + discountRate);
    }
}
