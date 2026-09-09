package com.o2o.catalog.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import com.o2o.catalog.domain.Property;

/**
 * 스프링 데이터가 구현을 만들어 주는 인터페이스. 설계 근거: 06-4 1-4의 유일성과 무결성은 DB.
 *
 * domain의 PropertyRepository와 이름이 다른 이유는 둘이 다른 것이기 때문이다. 저쪽은 도메인이
 * 선언한 항구이고 이것은 그 항구를 채우는 어댑터의 부품이다. 이 타입이 domain에 올라가면
 * domain이 스프링 데이터를 참조하게 되고 레이어 역전 축에 걸린다.
 *
 * 식별자 타입이 String인 이유는 Property가 식별자를 문자열 열로 저장하기 때문이다.
 * 그 결정의 이유는 Property 머리 주석에 있다.
 */
public interface PropertyJpaRepository extends JpaRepository<Property, String> {
}
