package com.o2o.catalog.api;

import com.o2o.catalog.domain.Property;

/**
 * 설계 근거: 11 응답 모델 Property. 아홉 필드가 그 표 순서 그대로다.
 *
 * 도메인 객체를 그대로 내보내지 않는 이유는 11이 응답 모양을 정하고 06-2가 도메인 모양을
 * 정하기 때문이다. 01 Step 9도 API 명세의 응답 모델이 JPA 모델의 구조를 그대로 결정하지
 * 않는다고 적는다. 둘을 한 클래스로 묶으면 한쪽이 바뀔 때 다른 쪽이 끌려간다.
 */
public record PropertyResponse(
        String id,
        String hostId,
        String name,
        String regionCode,
        String address,
        String description,
        long version,
        String createdAt,
        String updatedAt) {

    public static PropertyResponse from(Property property) {
        return new PropertyResponse(
                property.id().value(),
                property.hostId().value(),
                property.name(),
                property.region().code(),
                property.address().value(),
                property.description(),
                property.version(),
                ApiTime.format(property.createdAt()),
                ApiTime.format(property.updatedAt()));
    }
}
