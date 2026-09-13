/**
 * 결제 컨텍스트의 API 층. 설계 근거: 11 내부 처리와 Mock 이벤트 절의 INTERNAL-01, 11 인증과
 * 접근 제어(MOCK_SYSTEM 행과 셋째 문단의 개발 프로파일 밖 비활성), 06-4 1-4(형식 검증은
 * 컨트롤러), task-S9-payment 2절 INTERNAL-01 검사 순서와 7절 D-3.
 *
 * HTTP를 도메인 말로 바꾼다. 형식을 보고 규칙은 보지 않는다. 경로는 INTERNAL-01 하나다(F21).
 * PAY-01과 PAY-02는 예약이 받아 결제에 넘기는 중계라 예약 2차의 몫이고, 이 층은 그 예외
 * 넷(AttemptInProgress, AttemptLimitExceeded, AlreadyApproved, NoApprovedAttempt)의 HTTP
 * 매핑을 하지 않는다. 컨트롤러는 dev 프로파일에서만 뜬다(T30).
 */
package com.o2o.payment.api;
