package com.o2o.inventory.domain;

import java.time.LocalDate;

import com.o2o.shared.RoomTypeId;

/**
 * 설계 근거: 06-4 1-2 registerRate의 위반 시 예외 DuplicateRate. 11 RATE-01의 409 RESOURCE_ALREADY_EXISTS.
 */
public class DuplicateRateException extends RuntimeException {

    public DuplicateRateException(RoomTypeId roomTypeId, LocalDate stayDate) {
        super("이미 등록된 요금이다: " + roomTypeId.value() + " " + stayDate);
    }
}
