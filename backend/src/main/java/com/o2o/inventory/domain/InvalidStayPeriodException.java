package com.o2o.inventory.domain;

import java.time.LocalDate;

/**
 * 설계 근거: 06-4 1-2 openInventory의 Pre 열 기간 상한 이내 [가설 366일], 11 INV-02(명세 137행).
 *
 * 계약표는 366일을 가설로 적고 11 명세가 최대 366일로 확정한다. 계약 6절이 명세를 따르기로
 * 확정했다. from은 포함하고 to는 제외하는 반열림 구간이며 근거는 11 INV-02 처리 규칙과
 * 05-3의 반열림 규칙이다.
 */
public class InvalidStayPeriodException extends RuntimeException {

    public static final int MAX_DAYS = 366;

    public InvalidStayPeriodException(String reason, LocalDate from, LocalDate to) {
        super(reason + ". from " + from + ", to " + to);
    }
}
