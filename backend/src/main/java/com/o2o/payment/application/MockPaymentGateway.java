package com.o2o.payment.application;

import com.o2o.payment.domain.PaymentAttemptId;
import com.o2o.shared.Money;

/**
 * Mock PG 포트. 설계 근거: 06-1 R7(PG는 결제 컨텍스트의 부패 방지 계층 뒤에 있다), 06-4 v5 1-2
 * 결제 머리(Payment 애그리거트는 PG를 호출하지 않는다. 외부 호출은 결제 앱 서비스가 한다).
 *
 * 요청과 환불 둘 다 attemptId가 멱등키다(06-4 v5 1-2 openAttempt Post와 refund Post, 08-3 결정 4).
 * 같은 키의 재요청은 같은 결과다. 구현은 payment/infrastructure의 프로세스 안 어댑터이고 결과
 * 전달(APPROVE와 DECLINE)은 이 포트가 아니라 커밋 뒤 자동 결과 어댑터가 한다(계약 7절 D-1).
 */
public interface MockPaymentGateway {

    /** 결제 요청. 거래 번호를 돌려준다. 08-3 결정 9에 따라 openAttempt와 같은 트랜잭션에서 불린다 */
    String request(PaymentAttemptId attemptId, Money amount);

    /** 전액 환불. 08-3 결정 10에 따라 Payment 잠금을 쥔 채 불린다 */
    void refund(PaymentAttemptId attemptId, Money amount);
}
