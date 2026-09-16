package com.o2o.inventory.domain;

import java.time.LocalDate;

import com.o2o.shared.RoomTypeId;

/**
 * 설계 근거: 06-4 1-2 adjustRate의 대상 존재 선행조건. 11 RATE-04가 레코드 없으면 404라고 적는다(명세 500행).
 */
public class RateNotFoundException extends RuntimeException {

    public RateNotFoundException(RoomTypeId roomTypeId, LocalDate stayDate) {
        super("요금을 찾을 수 없다: " + roomTypeId.value() + " " + stayDate);
    }
}
