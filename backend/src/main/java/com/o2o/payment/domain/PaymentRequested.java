package com.o2o.payment.domain;

import java.time.Instant;

import com.o2o.shared.Money;

/**
 * 결제 요청됨. 설계 근거: 06-4 1-2 openAttempt의 Post 열, 03 결제 이벤트 행, 06-4 v5 2-4
 * (paymentId, bookingId, attemptId, 청구액)에 mockMode를 더한 계약 6절 이벤트 페이로드 행.
 *
 * mockMode가 실리는 이유는 자동 결과 어댑터의 분기 입력이라서다(계약 7절 D-1). 발행은 앱
 * 서비스가 트랜잭션 안에서 하고 커밋 뒤 전달은 구독자가 지킨다(layers.md 3-3). 첫 구독자가
 * payment/infrastructure의 자동 결과 어댑터다. 검증 항목은 Y8, Y11, Y12다.
 * amount는 페이로드의 amount와 currency 둘을 한 값으로 묶은 것이다.
 */
public record PaymentRequested(PaymentId paymentId, String bookingId, PaymentAttemptId paymentAttemptId,
                               Money amount, MockMode mockMode, Instant occurredAt) {

    public static PaymentRequested of(Payment payment, PaymentAttempt attempt) {
        return new PaymentRequested(payment.id(), payment.bookingId(), attempt.id(), attempt.amount(),
                attempt.mockMode(), attempt.requestedAt());
    }
}
