package com.o2o.payment.api;

import com.o2o.payment.domain.MockEventResult;
import com.o2o.shared.ApiTime;

/**
 * 응답 모델 MockEventResult. 설계 근거: 11 응답 모델 MockEventResult(2594행)의 4개 필드 그대로.
 * 필드를 더하지 않는다(BN1). processedAt은 최초 업무 처리 완료 시각이고 ApiTime 형식이다.
 */
public record MockEventResultResponse(String eventId, String paymentAttemptId, String result,
                                      String processedAt) {

    public static MockEventResultResponse from(MockEventResult result) {
        return new MockEventResultResponse(result.eventId(), result.paymentAttemptId().value(),
                result.result().name(), ApiTime.format(result.processedAt()));
    }
}
