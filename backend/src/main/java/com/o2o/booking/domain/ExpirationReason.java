package com.o2o.booking.domain;

/**
 * HELD가 EXPIRED로 끝난 원인. 설계 근거: 11 응답 모델 Booking의 expirationReason(EXPIRED이면
 * TTL_EXPIRED 또는 PAYMENT_FAILED, 그 외 null), 11 상태 전이와 시간 경계 표, 06-4 1-2 expire(reason).
 *
 * 둘이 겹치면 이미 확정된 원인을 덮어쓰지 않고, 아직 HELD인데 실패 처리 시각에 TTL이 지났으면
 * TTL_EXPIRED가 우선한다(11 상태 전이와 시간 경계, 08-3 결정 8). 그 분기는 앱 서비스가 한다.
 */
public enum ExpirationReason {
    TTL_EXPIRED,
    PAYMENT_FAILED
}
