/**
 * 검색 컨텍스트의 응용 층. 설계 근거: 06-1 R8, 계약 task-S9-promotion-search 7절 D-1 나.
 *
 * 다른 컨텍스트의 리포지토리를 읽기 전용으로 부르고 결과를 조립한다. 가격 계산은 promotion
 * 도메인의 PricingService에 맡긴다(11 내부 처리 여섯째 줄. 검색과 예상 금액과 예약이 같은 계산
 * 규칙). 트랜잭션은 전부 읽기 전용이다.
 */
package com.o2o.search.application;
