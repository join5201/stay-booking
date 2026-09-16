package com.o2o.payment.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

import com.o2o.payment.domain.MockOutcome;
import com.o2o.payment.domain.PaymentAttempt;

/**
 * INTERNAL-01의 입력. 설계 근거: 11 INTERNAL-01 요청 표의 일곱 필드(eventId, paymentAttemptId,
 * pgTransactionId, outcome, amount, currency, failureCode).
 *
 * amount가 Money가 아니라 long인 이유는 계약 2절 INTERNAL-01 표 6행이다. Money의 상한이 형식
 * 검사의 상한 30,000,000,000보다 낮아 이벤트 금액을 Money로 만들면 형식이 맞는 요청이 여기서
 * 죽는다. 대조는 domain이 long으로 한다. 형식 검사(길이, 허용 값, failureCode 조건)는 api 몫이다.
 *
 * 자동 결과도 같은 명령이다(계약 7절 D-1). eventId가 auto_ 뒤에 attemptId로 결정적이라 재시작
 * 뒤 같은 시도를 다시 재개해도 규칙 2와 3이 DUPLICATE로 막는다(T26).
 */
public record MockEventCommand(String eventId, String paymentAttemptId, String pgTransactionId,
                               MockOutcome outcome, long amount, String currency, String failureCode) {

    private static final String AUTO_PREFIX = "auto_";

    public MockEventCommand {
        Objects.requireNonNull(eventId, "eventId는 null일 수 없다");
        Objects.requireNonNull(paymentAttemptId, "paymentAttemptId는 null일 수 없다");
        Objects.requireNonNull(pgTransactionId, "pgTransactionId는 null일 수 없다");
        Objects.requireNonNull(outcome, "outcome은 null일 수 없다");
        Objects.requireNonNull(currency, "currency는 null일 수 없다");
    }

    /** 자동 결과(APPROVE, DECLINE). 시도에 저장된 거래 번호와 금액을 그대로 싣는다 */
    public static MockEventCommand autoResult(PaymentAttempt attempt) {
        MockOutcome outcome = attempt.mockMode().autoOutcome();
        String failureCode = outcome == MockOutcome.FAILED ? PaymentAttempt.MOCK_DECLINED : null;
        return new MockEventCommand(AUTO_PREFIX + attempt.id().value(), attempt.id().value(),
                attempt.pgTransactionId(), outcome, attempt.amount().amount(),
                attempt.amount().currency(), failureCode);
    }

    /**
     * 11 INTERNAL-01 규칙 2의 같은 body 판정에 쓰는 해시. 일곱 필드를 줄바꿈으로 이어 sha256을
     * 낸다. 필드 순서가 고정이라 같은 내용은 같은 해시다. failureCode가 없으면 빈 문자열이다.
     */
    public String bodyHash() {
        String canonical = String.join("\n", eventId, paymentAttemptId, pgTransactionId,
                outcome.name(), Long.toString(amount), currency, failureCode == null ? "" : failureCode);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256을 쓸 수 없다", e);
        }
    }
}
