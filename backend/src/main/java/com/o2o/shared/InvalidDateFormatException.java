package com.o2o.shared;

/**
 * 400 INVALID_DATE_RANGE의 형식 몫. 설계 근거: 11 에러 응답 표 400 INVALID_DATE_RANGE가
 * 날짜 형식과 순서와 박수 제한 오류를 한 줄로 묶는다.
 *
 * inventory/api에 같은 이름의 패키지 전용 예외가 있다. 두 번째 컨텍스트(프로모션)와 세 번째
 * (검색)가 같은 규칙을 쓰므로 backend/CLAUDE.md 3-2에 따라 shared에 올린다. 기존 inventory
 * 쪽을 옮기는 것은 이번 범위 밖이라 그대로 둔다. 응답으로 바꾸는 핸들러는 promotion/api의
 * PromotionExceptionHandler에 있다. SharedExceptionHandler가 이번 세션에 동결이라서다.
 */
public class InvalidDateFormatException extends RuntimeException {

    public InvalidDateFormatException(String field, String value) {
        super("날짜 형식이 올바르지 않다. " + field + "=" + value);
    }
}
