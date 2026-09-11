/**
 * 예약 컨텍스트의 도메인 층. 설계 근거: 05-2 1절 예약(Booking), 06-2 1절 예약 행, 06-4 1-4.
 *
 * 이 패키지는 Spring과 Spring Data를 참조하지 않는다. jakarta.persistence만 쓴다. 근거는
 * 재고와 요금 컨텍스트와 같다. 값 객체 BookingId, UserId, IdempotencyKey를 shared가 아니라
 * 여기 두는 근거는 backend/.claude/rules/layers.md 3-2다. 두 번째 컨텍스트가 쓰기 전에는
 * 올리지 않는다(task-S9-booking 7절 D-1).
 */
package com.o2o.booking.domain;
