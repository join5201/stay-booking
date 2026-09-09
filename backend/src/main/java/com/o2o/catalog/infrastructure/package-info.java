/**
 * 카탈로그 저장소 어댑터. domain이 선언한 리포지토리의 구현이 여기 있다.
 *
 * 설계 근거: 06-4 1-4 검증 책임 위치 표의 DB 행.
 *
 * 유일성과 무결성은 여기서 지킨다. 메모리 저장소로 대신하지 않는다.
 * 통합 테스트 C6과 C10이 실제 MySQL을 요구하는 근거가 그것이다
 * (eval-criteria-code.md 테스트 격리와 재현성 축).
 */
package com.o2o.catalog.infrastructure;
