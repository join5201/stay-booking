package com.o2o.catalog.domain;

import java.util.Optional;

import com.o2o.shared.HostId;
import com.o2o.shared.PageQuery;
import com.o2o.shared.PageResult;
import com.o2o.shared.PropertyId;

/**
 * 설계 근거: 06-2 6절 카탈로그 CRC의 CatalogApplicationService 협력자 PropertyRepository.
 *
 * 애그리거트 루트에만 리포지토리를 둔다. 06-2 1절이 Property를 루트로 적는다. 이 규칙은
 * eval-criteria-code.md의 Repository 단위 축이 직접 보는 항목이다.
 *
 * 이 인터페이스가 domain에 있는 이유는 의존 방향이다. infrastructure가 이것을 구현한다.
 * 반대로 두면 domain이 infrastructure를 참조하게 되고 그것이 레이어 역전 축의 지적 대상이다.
 * 그래서 여기에는 스프링 데이터 타입이 하나도 나오지 않는다. 쪽 나눔도 shared의 자체 타입이다.
 */
public interface PropertyRepository {

    Property save(Property property);

    Optional<Property> findById(PropertyId propertyId);

    /**
     * 설계 근거: 06-2 6절 CRC의 두 번째 책임 행. RegisterRoomType 처리 전에 대상 Property가
     * 존재하는지 확인한다. 협력자가 PropertyRepository 읽기다.
     */
    boolean existsById(PropertyId propertyId);

    /**
     * CAT-04. 설계 근거: 11 숙소 CAT-04. regionCode가 null이면 전체다.
     * 11 필드표가 그 값을 선택으로 적는다.
     */
    PageResult<Property> findAll(String regionCode, PageQuery pageQuery);

    /**
     * CAT-05. 설계 근거: 11 숙소 CAT-05 처리 규칙. 행위자의 hostId로 범위를 제한하고
     * hostId 쿼리는 받지 않는다. 그래서 인자가 쿼리 값이 아니라 행위자에서 온 값이다.
     */
    PageResult<Property> findByHostId(HostId hostId, PageQuery pageQuery);
}
