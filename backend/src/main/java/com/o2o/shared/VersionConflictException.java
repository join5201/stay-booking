package com.o2o.shared;

/**
 * 409 VERSION_CONFLICT. 설계 근거: 11 공통 요청과 응답 규칙, 11 에러 응답 표.
 *
 * 그 절이 수정 요청의 version은 마지막 조회에서 받은 값이고 불일치하면 409이며 적용하지
 * 않는다고 적는다. 2026-09-09 사용자 결정으로 동시 수정 처리를 낙관적 잠금(Optimistic Lock)으로
 * 확정했다. 저장할 때 요청의 숫자와 서버의 숫자를 대조하는 방식이다.
 *
 * shared에 두는 이유는 11 공통 절이 정한 규칙이라 카탈로그 밖의 수정 API도 같은 예외를 쓰기
 * 때문이다. 재고와 요금의 PATCH가 곧 온다.
 */
public class VersionConflictException extends RuntimeException {

    public VersionConflictException(long expected, long actual) {
        super("수정 버전이 다르다. 요청 " + expected + ", 현재 " + actual);
    }
}
