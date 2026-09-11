/**
 * 예약 컨텍스트의 API 층. 설계 근거: 11 예약과 결제 절의 BOOK-01부터 BOOK-03, 06-4 1-4(형식
 * 검증은 컨트롤러), 11 명세 멱등 처리 절.
 *
 * HTTP를 도메인 말로 바꾼다. 형식을 보고 규칙은 보지 않는다. API의 guestId와 guestCount를
 * 도메인의 userId와 userCount로 바꾸는 자리가 여기다(05-3 v8). 예약 요청은 멱등 실행기로
 * 감싸고 응답 JSON은 이 층이 만들어 실행기에 넘긴다.
 */
package com.o2o.booking.api;
