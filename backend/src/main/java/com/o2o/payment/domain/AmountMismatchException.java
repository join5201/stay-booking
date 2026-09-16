package com.o2o.payment.domain;

import com.o2o.shared.Money;

/**
 * 금액 불일치. 두 자리에서 난다.
 *
 * 첫째, openAttempt의 전달 총액이 청구액과 다를 때. 설계 근거: 06-4 1-2 openAttempt Pre의
 * 전달 총액과 청구액 일치와 위반 예외 AmountMismatch. 청구액은 첫 요청이 정한다(R5의 결제 몫, Y4).
 * 둘째, INTERNAL-01 이벤트의 amount나 currency가 그 시도의 것과 다를 때. 설계 근거: 11 INTERNAL-01
 * 규칙 1(금액 차이는 PAYMENT_AMOUNT_MISMATCH). 이벤트 금액은 Money로 만들지 않고 long으로
 * 대조한다(계약 2절 INTERNAL-01 표 6행). HTTP 매핑은 api 층이 한다.
 */
public class AmountMismatchException extends RuntimeException {

    public AmountMismatchException(Money charged, Money requested) {
        super("전달 총액이 청구액과 다르다. 청구액 " + charged.amount() + " " + charged.currency()
                + ", 전달 " + requested.amount() + " " + requested.currency());
    }

    public AmountMismatchException(Money own, long amount, String currency) {
        super("이벤트 금액이 시도 금액과 다르다. 시도 " + own.amount() + " " + own.currency()
                + ", 이벤트 " + amount + " " + currency);
    }
}
