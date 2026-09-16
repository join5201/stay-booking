package com.o2o.payment.infrastructure;

import org.junit.jupiter.api.Test;

import com.o2o.payment.domain.PaymentAttemptId;
import com.o2o.shared.Money;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Y26. S9-R1-B-01. Mock PG 어댑터의 멱등성. 설계 근거: 계약 2절 openAttempt 표 8행(attemptId가
 * 멱등키), 6절 08-3 결정 4 행(환불도 attemptId를 멱등키로 같은 키 재요청은 같은 결과), 06-4 v5 1-2.
 *
 * 어댑터만 본다. DB도 스프링도 없다. 앱 서비스 테스트(Y7)는 둘째 환불에서 어댑터 호출 자체를
 * 건너뛰므로 어댑터의 같은 키 재호출은 여기서만 직접 확인한다.
 */
class InProcessMockPaymentGatewayTest {

    private static final Money CHARGE = Money.krw(180_000);

    @Test
    void Y26_같은_attemptId의_request는_같은_거래_번호이고_다른_attemptId는_다르다() {
        InProcessMockPaymentGateway gateway = new InProcessMockPaymentGateway();
        PaymentAttemptId attemptId = PaymentAttemptId.of("attempt_aaaaaaaa");

        String first = gateway.request(attemptId, CHARGE);
        String again = gateway.request(attemptId, CHARGE);

        assertEquals(first, again);
        assertTrue(first.matches("mock_tx_[0-9a-f]{32}"), first);
        assertNotEquals(first, gateway.request(PaymentAttemptId.of("attempt_bbbbbbbb"), CHARGE));
    }

    @Test
    void Y26_같은_attemptId의_refund는_두_번_불러도_업무_효과가_하나이고_예외가_없다() {
        InProcessMockPaymentGateway gateway = new InProcessMockPaymentGateway();
        PaymentAttemptId attemptId = PaymentAttemptId.of("attempt_cccccccc");
        assertFalse(gateway.hasRefunded(attemptId));

        assertDoesNotThrow(() -> {
            gateway.refund(attemptId, CHARGE);
            gateway.refund(attemptId, CHARGE);
        });

        assertTrue(gateway.hasRefunded(attemptId));
    }
}
