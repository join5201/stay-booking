package com.o2o.search.api;

import com.o2o.promotion.api.PriceSnapshotResponse;
import com.o2o.search.application.PriceQuoteResult;
import com.o2o.shared.ApiDate;
import com.o2o.shared.ApiTime;

/**
 * 응답 모델 PriceQuote. 설계 근거: 11 응답 모델 PriceQuote 일곱 칸. nights는 checkOut - checkIn.
 */
public record PriceQuoteResponse(String roomTypeId, String checkIn, String checkOut, int guestCount,
                                 int nights, String estimatedAt, PriceSnapshotResponse price) {

    public static PriceQuoteResponse from(PriceQuoteResult result) {
        return new PriceQuoteResponse(result.roomTypeId().value(),
                ApiDate.format(result.stay().checkIn()), ApiDate.format(result.stay().checkOut()),
                result.guestCount().value(), result.stay().nights(),
                ApiTime.format(result.estimatedAt()), PriceSnapshotResponse.from(result.price()));
    }
}
