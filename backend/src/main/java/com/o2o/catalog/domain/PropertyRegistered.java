package com.o2o.catalog.domain;

import java.time.Instant;

import com.o2o.shared.HostId;
import com.o2o.shared.PropertyId;

/**
 * 숙소가 등록됨. 설계 근거: 06-4 1-2 카탈로그 계약표 registerProperty의 Post 열,
 * 03 1절 이벤트 목록, 05-2 2절 숙소 카탈로그가 내는 이벤트.
 *
 * 지금 이 이벤트를 받는 곳이 없다. 06-4 2-1 이벤트에서 커맨드로의 트리거 맵핑에
 * 카탈로그 이벤트가 한 줄도 없다. 그래도 발행하는 이유는 계약표의 Post 열이 그것을
 * 후행조건으로 적기 때문이다. 구독자가 없다는 사실이 발행하지 않을 근거는 아니다.
 *
 * 실을 값을 정한 문서가 없다. 그래서 애그리거트 식별자와 소유자와 발생 시각 셋만 싣는다.
 * 받는 쪽이 생기면 그때 필요한 값을 문서에 먼저 적고 늘린다.
 */
public record PropertyRegistered(PropertyId propertyId, HostId hostId, Instant occurredAt) {

    public static PropertyRegistered of(Property property) {
        return new PropertyRegistered(property.id(), property.hostId(), property.createdAt());
    }
}
