package com.o2o.search.api;

import java.util.List;

import com.o2o.catalog.domain.Property;
import com.o2o.search.application.PropertySearchResult;
import com.o2o.search.application.RoomSearchResult;
import com.o2o.shared.ApiTime;

/**
 * 응답 모델 PropertySearchResult. 설계 근거: 11 응답 모델 PropertySearchResult 네 칸,
 * RoomSearchResult 다섯 칸, Property 아홉 칸.
 *
 * 숙소 아홉 칸을 여기서 다시 적는 이유는 catalog/api의 PropertyResponse가 그 컨텍스트의 응답이기
 * 때문이다. 검색은 읽기 모델이라 자기 응답을 갖는다(06-1 R8의 Published Language 성격). 두 응답의
 * 칸은 11 Property 표 하나에서 나온다.
 */
public record PropertySearchResultResponse(PropertyPart property, long lowestTotalAmount,
                                           String currency, List<RoomPart> availableRoomTypes) {

    public record PropertyPart(String id, String hostId, String name, String regionCode,
                               String address, String description, long version,
                               String createdAt, String updatedAt) {

        static PropertyPart from(Property property) {
            return new PropertyPart(property.id().value(), property.hostId().value(),
                    property.name(), property.region().code(), property.address().value(),
                    property.description(), property.version(),
                    ApiTime.format(property.createdAt()), ApiTime.format(property.updatedAt()));
        }
    }

    public record RoomPart(String roomTypeId, String name, int maxOccupancy, int availableCount,
                           long totalAmount) {

        static RoomPart from(RoomSearchResult room) {
            return new RoomPart(room.roomTypeId().value(), room.name(), room.maxOccupancy(),
                    room.availableCount(), room.totalAmount());
        }
    }

    public static PropertySearchResultResponse from(PropertySearchResult result) {
        return new PropertySearchResultResponse(PropertyPart.from(result.property()),
                result.lowestTotalAmount(), result.currency(),
                result.availableRoomTypes().stream().map(RoomPart::from).toList());
    }
}
