/**
 * 결제 컨텍스트의 도메인 층. 설계 근거: 05-2 1절 결제(Payment), 06-2 1절 Payment 행, 06-4 1-4.
 *
 * 이 패키지는 Spring과 Spring Data를 참조하지 않는다. jakarta.persistence만 쓴다. 근거는
 * 입력 팩 1절의 도메인 모델과 JPA 엔티티를 분리하지 않는다는 확정 전제와, 평가 축의
 * 레이어 역전이다. 재고와 요금 컨텍스트의 같은 자리와 성격이 같다.
 *
 * 결제는 예약을 모른다(06-1 R6). 예약 식별자는 문자열 bookingId로 받고 booking 패키지를
 * 참조하지 않는다. 값 객체와 열거(PaymentId, PaymentAttemptId, MockMode, RefundReason,
 * AttemptKind, PaymentAttemptStatus)는 두 번째 컨텍스트가 쓰기 전까지 여기 둔다
 * (backend/.claude/rules/layers.md 3-2, 계약 task-S9-payment 6절 값 객체 행).
 */
package com.o2o.payment.domain;
