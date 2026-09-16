package com.o2o.shared;

/**
 * 개발 행위자의 역할. 설계 근거: 11 인증과 접근 제어의 주체 표, P07.
 *
 * 회원 체계가 아니다. 같은 절이 AccessToken과 RefreshToken과 회원가입과 로그인을
 * 이번 범위에 없다고 적는다. 로컬 역할과 소유권 테스트용이다.
 */
public enum ActorRole {
    HOST,
    OPERATOR,
    GUEST,
    MOCK_SYSTEM
}
