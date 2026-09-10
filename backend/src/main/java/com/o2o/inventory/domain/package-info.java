/**
 * 재고와 요금 컨텍스트의 도메인 층. 설계 근거: 05-2 1절 재고와 요금(Inventory), 06-4 1-4.
 *
 * 이 패키지는 Spring과 Spring Data를 참조하지 않는다. jakarta.persistence만 쓴다. 근거는
 * 입력 팩 1절의 도메인 모델과 JPA 엔티티를 분리하지 않는다는 확정 전제와, 평가 축의
 * 레이어 역전이다. 도메인이 인프라를 참조하면 그 축에 걸린다.
 */
package com.o2o.inventory.domain;
