package com.o2o.inventory.domain;

import java.time.LocalDate;

import com.o2o.shared.RoomTypeId;

/**
 * commit과 releaseHeld의 위반. 설계 근거: 06-4 1-2 commit(n)과 releaseHeld(n)의 위반 시 예외
 * InsufficientHold. 선점된 수보다 많이 판매로 옮기거나 반환하려는 것이다.
 *
 * 1차에는 이 예외에 닿는 호출 경로가 없다. 호출자는 확정과 만료(2차)다. 검사 자체는
 * I1a의 자리라 지금 넣는다(06-2 6절 CRC). 검증 항목은 계약 8-1절 K5다.
 */
public class InsufficientHoldException extends RuntimeException {

    public InsufficientHoldException(RoomTypeId roomTypeId, LocalDate stayDate, int held,
                                     int requested) {
        super("선점 수량이 부족하다. " + roomTypeId.value() + " " + stayDate
                + " 선점 " + held + ", 요청 " + requested);
    }
}
