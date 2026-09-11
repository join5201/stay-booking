package com.o2o.inventory.domain;

import java.time.LocalDate;

import com.o2o.shared.RoomTypeId;

/**
 * releaseSold의 위반. 설계 근거: 06-4 1-2 releaseSold(n)의 위반 시 예외 InsufficientSold.
 * 판매된 수보다 많이 반환하려는 것이다. 호출자는 취소(2차)다. K5.
 */
public class InsufficientSoldException extends RuntimeException {

    public InsufficientSoldException(RoomTypeId roomTypeId, LocalDate stayDate, int sold,
                                     int requested) {
        super("판매 수량이 부족하다. " + roomTypeId.value() + " " + stayDate
                + " 판매 " + sold + ", 요청 " + requested);
    }
}
