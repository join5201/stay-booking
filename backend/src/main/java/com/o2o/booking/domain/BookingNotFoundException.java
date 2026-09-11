package com.o2o.booking.domain;

/**
 * 예약 없음. 없는 예약과 남의 예약이 같은 예외를 쓴다. 11 인증과 접근 제어가 남의 자원을
 * 자원 정보 없이 404로 주라고 적고 T02가 그것을 확인한다. 404 RESOURCE_NOT_FOUND.
 */
public class BookingNotFoundException extends RuntimeException {

    public BookingNotFoundException(BookingId bookingId) {
        super("예약을 찾을 수 없다: " + bookingId.value());
    }
}
