package com.o2o.booking.domain;

/**
 * 결제 실패 만료의 선행조건 위반. 06-4 1-2 expire(reason)의 Pre가 원인이 결제 실패면 이벤트에
 * 실린 시도 수가 3 이상인지 재확인한다고 적는다(11 규칙 6, 상태 전이 표의 3회 실패 행).
 * 1회나 2회 실패는 HELD를 유지하고 재시도가 가능하다(T16).
 *
 * 앱 서비스가 시도 수로 먼저 분기하므로 정상 경로에서는 여기 오지 않는다.
 */
public class FailedAttemptsBelowLimitException extends RuntimeException {

    public FailedAttemptsBelowLimitException(BookingId bookingId, int attemptCount, int limit) {
        super("결제 실패 만료는 시도 " + limit + "회부터다: " + bookingId.value() + " attemptCount="
                + attemptCount);
    }
}
