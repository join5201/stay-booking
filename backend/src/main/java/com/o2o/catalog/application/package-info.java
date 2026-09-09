/**
 * 카탈로그 앱 서비스. 유스케이스 한 건이 메서드 하나다.
 *
 * 설계 근거: 06-4 1-2 카탈로그 계약표, 06-4 1-4 검증 책임 위치 표의 앱 서비스 행.
 *
 * 여기서 하는 검증은 컨텍스트를 넘는 선행조건이다. 없는 propertyId로 객실 타입을
 * 등록하려는 경우가 그것이다(06-4 1-2 registerRoomType의 PropertyNotFound, 테스트 C2).
 * 형식 검증은 api가, 규칙 검증은 domain이 한다. 여기서 다시 하지 않는다.
 */
package com.o2o.catalog.application;
