package com.o2o.payment.api;

import com.o2o.payment.application.MockEventCommand;
import com.o2o.payment.domain.MockOutcome;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * INTERNAL-01 요청 본문. 설계 근거: 11 INTERNAL-01 요청 표, 계약 2절 검사 순서 1행. 여섯은 필수이고
 * failureCode는 조건부다. 미정의 필드는 전역 설정(fail-on-unknown-properties)이 400으로 막는다.
 *
 * currency는 형식으로 KRW를 강제하지 않는다. 계약 2절 6행이 통화 차이를 시도와의 대조로 두어
 * 409 PAYMENT_AMOUNT_MISMATCH를 내게 했고 11 에러 표도 그 코드에 통화 불일치를 적는다. amount의
 * 상한 30,000,000,000은 명세의 형식 상한이고 Money의 상한보다 높다(계약 6절 P03). 그래서 Money로
 * 만들지 않고 long으로 넘긴다.
 */
@FailureCodeMatchesOutcome
public record MockPaymentEventRequest(
        @NotBlank @Size(max = 128) String eventId,
        @NotBlank @Size(max = 64) String paymentAttemptId,
        @NotBlank @Size(max = 64) String pgTransactionId,
        @NotBlank @Pattern(regexp = "APPROVED|FAILED") String outcome,
        @NotNull @Min(1) @Max(30_000_000_000L) Long amount,
        @NotBlank String currency,
        @Pattern(regexp = "MOCK_DECLINED") String failureCode) {

    public MockEventCommand toCommand() {
        return new MockEventCommand(eventId, paymentAttemptId, pgTransactionId,
                MockOutcome.valueOf(outcome), amount, currency, failureCode);
    }
}
