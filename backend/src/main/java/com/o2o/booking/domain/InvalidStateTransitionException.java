package com.o2o.booking.domain;

/**
 * 전이 폐쇄(I5) 위반. 허용 전이는 HELD에서 CONFIRMED, HELD에서 EXPIRED, CONFIRMED에서 CANCELED
 * 셋뿐이다(06-4 1-3 예약 상태 표). 종착 상태에 같은 전이를 다시 부르는 것은 위반이 아니라
 * 무해 경로라 여기 오지 않는다(06-4 0절 종착 재호출 무해).
 *
 * 사람 경로(BOOK-04)에서는 409 BOOKING_STATE_CONFLICT가 되고 정책 경로(P1, P3, T1)에서는 앱
 * 서비스가 부르기 전에 상태로 분기해 여기 오지 않는다(08-3 결정 5).
 */
public class InvalidStateTransitionException extends RuntimeException {

    private final BookingStatus from;
    private final BookingStatus to;

    public InvalidStateTransitionException(BookingId bookingId, BookingStatus from, BookingStatus to) {
        super("허용되지 않는 전이다: " + bookingId.value() + " " + from + " -> " + to);
        this.from = from;
        this.to = to;
    }

    public BookingStatus from() {
        return from;
    }

    public BookingStatus to() {
        return to;
    }
}
