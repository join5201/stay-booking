/**
 * 카탈로그 컨텍스트. 숙소(Property)와 객실 타입(RoomType)을 등록하고 조회한다.
 *
 * 설계 근거: 05-2 1절 컨텍스트 목록, 06-1 카탈로그가 상류인 관계 행, 06-2 6절 카탈로그 CRC.
 *
 * 아래 네 층으로 가른다. 층 이름의 근거는 06-4 1-4 검증 책임 위치 표다.
 *   api            형식 검증
 *   application    컨텍스트를 넘는 선행조건
 *   domain         규칙 검증. 불변식의 수호자
 *   infrastructure 유일성과 무결성
 *
 * 의존 방향은 api -> application -> domain 이고 infrastructure는 domain이 선언한
 * 리포지토리를 구현한다. domain이 infrastructure를 참조하면 레이어 역전이다.
 */
package com.o2o.catalog;
