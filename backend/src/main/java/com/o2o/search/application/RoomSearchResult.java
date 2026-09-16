package com.o2o.search.application;

import com.o2o.shared.RoomTypeId;

/**
 * 설계 근거: 11 응답 모델 RoomSearchResult 다섯 칸. availableCount는 숙박 전 날짜 최소 가용 수,
 * totalAmount는 할인 후 전체 숙박 금액이다.
 */
public record RoomSearchResult(RoomTypeId roomTypeId, String name, int maxOccupancy,
                               int availableCount, long totalAmount) {
}
