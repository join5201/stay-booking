package com.o2o.catalog.domain;

import com.o2o.shared.PropertyId;

/**
 * 설계 근거: 06-4 1-2 카탈로그 계약표 registerRoomType의 위반 시 예외 PropertyNotFound와
 * updateProperty의 NotFound. 11 CAT-03 조회의 404도 이 예외에서 나온다.
 */
public class PropertyNotFoundException extends RuntimeException {

    public PropertyNotFoundException(PropertyId propertyId) {
        super("숙소를 찾을 수 없다: " + propertyId.value());
    }
}
