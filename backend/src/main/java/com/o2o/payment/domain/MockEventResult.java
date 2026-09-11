package com.o2o.payment.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * Mock 이벤트 처리 결과. 설계 근거: 11 응답 모델 MockEventResult의 4개 필드(eventId,
 * paymentAttemptId, result, processedAt).
 *
 * PROCESSED는 이번 처리로 시도가 전이했고, DUPLICATE는 같은 이벤트나 같은 거래의 같은 결과가
 * 이미 반영돼 있어 아무것도 바꾸지 않았다는 뜻이다(11 INTERNAL-01 규칙 2와 3). processedAt은
 * 어느 쪽이든 최초 업무 처리 완료 시각이다(같은 모델 표). 필드를 더하지 않는다(BN1).
 */
public record MockEventResult(String eventId, PaymentAttemptId paymentAttemptId, Result result,
                              Instant processedAt) {

    public enum Result {
        PROCESSED,
        DUPLICATE
    }

    public MockEventResult {
        Objects.requireNonNull(eventId, "eventId는 null일 수 없다");
        Objects.requireNonNull(paymentAttemptId, "paymentAttemptId는 null일 수 없다");
        Objects.requireNonNull(result, "result는 null일 수 없다");
        Objects.requireNonNull(processedAt, "processedAt은 null일 수 없다");
    }

    public static MockEventResult processed(String eventId, PaymentAttemptId attemptId,
                                            Instant processedAt) {
        return new MockEventResult(eventId, attemptId, Result.PROCESSED, processedAt);
    }

    public static MockEventResult duplicate(String eventId, PaymentAttemptId attemptId,
                                            Instant firstProcessedAt) {
        return new MockEventResult(eventId, attemptId, Result.DUPLICATE, firstProcessedAt);
    }
}
