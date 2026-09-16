package com.o2o.inventory.domain;

/**
 * I2 위반. 설계 근거: 06-4 1-2 재고와 요금 계약표 registerRate와 adjustRate의 위반 시 예외 InvalidRate.
 *
 * 11 명세는 amount 제약을 1부터 1,000,000,000으로 적는다. 하한 위반이 이 예외이고
 * 상한 위반은 Money 값 객체가 막는다. 형식 범위와 불변식을 가르는 근거는 06-4 1-4다.
 */
public class InvalidRateException extends RuntimeException {

    public InvalidRateException(long amount) {
        super("요금은 0보다 커야 한다: " + amount);
    }
}
