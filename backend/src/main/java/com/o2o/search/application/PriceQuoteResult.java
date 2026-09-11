package com.o2o.search.application;

import java.time.Instant;

import com.o2o.promotion.domain.PriceSnapshot;
import com.o2o.promotion.domain.StayRange;
import com.o2o.shared.GuestCount;
import com.o2o.shared.RoomTypeId;

/**
 * 설계 근거: 11 응답 모델 PriceQuote 일곱 칸. price는 조회 시점의 계산 결과이고 예약 확정
 * 금액이 아니다(11 SEARCH-03 처리 규칙 둘째 줄).
 */
public record PriceQuoteResult(RoomTypeId roomTypeId, StayRange stay, GuestCount guestCount,
                               Instant estimatedAt, PriceSnapshot price) {
}
