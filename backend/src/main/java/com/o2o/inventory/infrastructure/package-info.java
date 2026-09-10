/**
 * 재고와 요금 컨텍스트의 인프라 층. 설계 근거: 06-4 1-4의 유일성과 무결성은 DB.
 *
 * domain의 리포지토리 인터페이스를 여기서 구현한다. Spring Data는 이 패키지 안에서만 보인다.
 * 잠금도 여기 있다. 06-4 0절이 비관적 락으로 확정했고 잠그는 것은 DB이기 때문이다.
 */
package com.o2o.inventory.infrastructure;
