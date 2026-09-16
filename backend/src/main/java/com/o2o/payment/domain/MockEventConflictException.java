package com.o2o.payment.domain;

/**
 * 같은 eventId의 다른 내용. 설계 근거: 11 INTERNAL-01 규칙 2(같은 eventId와 같은 body의 재전달은
 * DUPLICATE다. 다른 body면 MOCK_EVENT_CONFLICT다), 계약 2절 INTERNAL-01 표 7행.
 *
 * 이벤트 기록(MockPaymentEvent)의 body 해시가 다를 때 앱 서비스가 던진다. 거래 번호 불일치와
 * 역행 전이도 같은 HTTP 코드(409 MOCK_EVENT_CONFLICT)로 나가지만 예외는 원인별로 가른다.
 * 검증 항목은 Y17이다.
 */
public class MockEventConflictException extends RuntimeException {

    public MockEventConflictException(String eventId) {
        super("같은 이벤트 ID에 다른 내용이 왔다: " + eventId);
    }
}
