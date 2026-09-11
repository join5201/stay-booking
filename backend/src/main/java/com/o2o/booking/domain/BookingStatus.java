package com.o2o.booking.domain;

/**
 * 예약 상태. 설계 근거: 06-2 1절 Booking의 BookingStatus, 06-4 1-3 상태전이표, 05-3 HELD와
 * CONFIRMED와 CANCELED와 EXPIRED 행. 1차는 HELD만 만든다. 전이는 HELD → CONFIRMED,
 * HELD → EXPIRED, CONFIRMED → CANCELED 셋뿐이고(I5) 전이 메서드는 2차다.
 */
public enum BookingStatus {
    HELD,
    CONFIRMED,
    CANCELED,
    EXPIRED
}
