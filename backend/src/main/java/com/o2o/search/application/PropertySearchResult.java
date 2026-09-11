package com.o2o.search.application;

import java.util.List;

import com.o2o.catalog.domain.Property;

/**
 * 설계 근거: 11 응답 모델 PropertySearchResult 네 칸. lowestTotalAmount는 포함된 객실의 최저
 * 할인 후 총액이고 currency는 KRW 고정이다.
 */
public record PropertySearchResult(Property property, long lowestTotalAmount, String currency,
                                   List<RoomSearchResult> availableRoomTypes) {
}
