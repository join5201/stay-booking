package com.o2o.payment.domain;

/**
 * I7 위반. 설계 근거: 06-4 1-2 openAttempt와 recordApproval의 위반 예외 AlreadyApproved,
 * 06-2 3-1 I7(예약당 승인 이력 하나).
 *
 * 두 자리에서 난다. 승인 이력이 있는 결제에 새 시도를 열 때(Y3)와, 다른 시도에 승인 이력이
 * 있는데 둘째 승인이 올 때다. 둘째는 I9와 I7이 앞을 막아 Mock에서는 닿기 어려운 방어선이다.
 */
public class AlreadyApprovedException extends RuntimeException {

    private final PaymentAttemptId approvedAttemptId;

    public AlreadyApprovedException(String bookingId, PaymentAttemptId approvedAttemptId) {
        super("이미 승인된 결제가 있다: " + bookingId + " 승인 시도 " + approvedAttemptId.value());
        this.approvedAttemptId = approvedAttemptId;
    }

    public PaymentAttemptId approvedAttemptId() {
        return approvedAttemptId;
    }
}
