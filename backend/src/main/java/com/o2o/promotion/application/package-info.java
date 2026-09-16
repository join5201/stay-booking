/**
 * 프로모션 컨텍스트의 응용 층. 설계 근거: 06-2 6절 프로모션 CRC의 PromotionApplicationService,
 * 06-4 0절 트랜잭션과 커밋 후 발행 규칙, backend/CLAUDE.md 레이어 규칙의 application 행.
 *
 * 트랜잭션 경계와 컨텍스트 간 선행조건(객실 타입 존재, 인원)과 이벤트 발행이 여기다.
 * 계산 규칙은 domain의 PricingService에 있고 이 층은 부르기만 한다.
 */
package com.o2o.promotion.application;
