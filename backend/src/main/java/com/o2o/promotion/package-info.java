/**
 * 프로모션 컨텍스트. 설계 근거: 05-2 1절 프로모션 행(자동 적용 할인 정책), 06-1 R3과 R5.
 *
 * 층은 domain, application, infrastructure, api 넷이다. 앞 두 묶음과 같다.
 * 카탈로그의 Region은 읽기만 한다(R3, Conformist). 이 컨텍스트는 Region 애그리거트를
 * 만들지 않고 regionCode 문자열을 조건으로 가진다. 계약 3절.
 */
package com.o2o.promotion;
