/**
 * 예약 컨텍스트의 응용 층. 설계 근거: 06-2 6절 예약 CRC의 BookingApplicationService 행, 06-4 1-4
 * (컨텍스트를 넘는 선행조건은 앱 서비스), 06-2 4절 트랜잭션 경계, 11 명세 멱등 처리 절.
 *
 * 트랜잭션 경계를 열고 인원과 요금 존재를 확인하며 재고 선점과 예약 생성과 멱등 기록을 한
 * 트랜잭션으로 묶는다. 가격 계산은 PriceQuotePort 뒤에 둔다. 1차는 요금만 합산하는 어댑터이고
 * 2차가 프로모션 컨텍스트의 PricingService 어댑터로 바꾼다(task-S9-booking 7절 D-4).
 */
package com.o2o.booking.application;
