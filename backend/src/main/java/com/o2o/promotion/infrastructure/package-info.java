/**
 * 프로모션 컨텍스트의 인프라 층. 설계 근거: 06-4 1-4의 유일성과 무결성은 DB, backend/CLAUDE.md
 * 레이어 규칙의 infrastructure 행.
 *
 * domain의 PromotionRepository를 여기서 구현한다. Spring Data는 이 패키지 안에서만 보인다.
 * 잠금도 여기 있다. 06-4 0절이 비관적 락으로 확정했고 잠그는 것은 DB이기 때문이다.
 * 도메인 서비스 PricingService의 빈 등록도 여기다. 도메인 층에 스프링을 들이지 않기 위해서다.
 */
package com.o2o.promotion.infrastructure;
