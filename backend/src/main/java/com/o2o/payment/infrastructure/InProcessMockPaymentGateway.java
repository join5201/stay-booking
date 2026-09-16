package com.o2o.payment.infrastructure;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.o2o.payment.application.MockPaymentGateway;
import com.o2o.payment.domain.PaymentAttemptId;
import com.o2o.shared.Money;

/**
 * 프로세스 안 Mock PG. 설계 근거: 06-1 R7(결제 컨텍스트의 부패 방지 계층 뒤의 PG), 11 결제 접수와
 * 환불 절(Mock 결제와 Mock 환불), 계약 1절 외부 시스템 행(프로세스 안 어댑터. 실패가 없다).
 *
 * 하는 일은 둘이다. 요청에 거래 번호를 발급하고, 환불을 받는다. 둘 다 attemptId가 멱등키라 같은
 * 키의 재요청은 같은 거래 번호와 같은 결과다(08-3 결정 4, 06-4 v5 1-2). 거래 번호 형식은 계약
 * 6절 ID 형식 행(mock_tx_ 뒤 UUID 32자)이다.
 *
 * 결과(승인, 실패)는 여기서 정하지 않는다. 시도에 저장된 mockMode를 보고 커밋 뒤 자동 결과
 * 어댑터가 INTERNAL-01과 같은 길로 넣는다(계약 7절 D-1). 그래서 이 클래스는 상태 기계를 모른다.
 * 기억은 프로세스 안이라 재시작하면 비지만, 같은 시도의 재요청은 시도가 이미 있어 생기지 않는다.
 */
@Component
public class InProcessMockPaymentGateway implements MockPaymentGateway {

    private static final String TRANSACTION_PREFIX = "mock_tx_";

    private final Map<String, String> transactionsByAttempt = new ConcurrentHashMap<>();
    private final Set<String> refundedAttempts = ConcurrentHashMap.newKeySet();

    @Override
    public String request(PaymentAttemptId attemptId, Money amount) {
        return transactionsByAttempt.computeIfAbsent(attemptId.value(),
                key -> TRANSACTION_PREFIX + UUID.randomUUID().toString().replace("-", ""));
    }

    @Override
    public void refund(PaymentAttemptId attemptId, Money amount) {
        // 같은 키의 둘째 환불은 첫째와 같은 결과다. 실 PG였다면 여기가 멱등키를 보내는 자리다
        refundedAttempts.add(attemptId.value());
    }

    /** 검증용. 그 시도의 환불을 받았는지 */
    public boolean hasRefunded(PaymentAttemptId attemptId) {
        return refundedAttempts.contains(attemptId.value());
    }
}
