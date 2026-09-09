package com.o2o.catalog.domain;

import java.time.Instant;

import com.o2o.shared.PropertyId;

/**
 * 숙소 정보가 수정됨. 설계 근거: 06-4 1-2 updateProperty의 Post 열, 03 1절, 05-2 2절.
 *
 * PropertyRegistered와 같이 구독자가 없다. 계약표가 후행조건으로 적어서 발행한다.
 * version을 싣는 이유는 몇 번째 수정인지가 이 이벤트를 구분하는 유일한 값이기 때문이다.
 */
public record PropertyUpdated(PropertyId propertyId, long version, Instant occurredAt) {

    public static PropertyUpdated of(Property property) {
        return new PropertyUpdated(property.id(), property.version(), property.updatedAt());
    }
}
