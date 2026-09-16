package com.o2o.shared;

/**
 * 요청 인원. 설계 근거: 11 공통 요청과 응답 규칙의 guestCount는 1 이상 100 이하 행.
 *
 * shared에 두는 근거는 backend/CLAUDE.md 3-2다. PROMO-05와 SEARCH-01부터 03이 같은 제약의
 * guestCount를 받고 11 BOOK-01도 같은 행을 적는다. 프로모션과 검색이 첫 사용처이고 예약이
 * 세 번째다.
 *
 * 범위 위반을 IllegalArgumentException으로 던지는 것은 PageQuery와 같은 판단이다. 11 공통
 * 절이 정한 값이라 shared의 핸들러가 400 INVALID_REQUEST로 낸다. 컨트롤러의 형식 검증이
 * 먼저 걸러야 하고(06-4 1-4) 여기 검사는 도메인 경계로 넘어가기 전 마지막 가드다.
 *
 * 객실이 이 인원을 받을 수 있는지는 여기서 보지 않는다. 그것은 RoomType.maxOccupancy와의
 * 비교이고 API별 처리 규칙에 따라 앱 서비스가 한다(11 명세의 같은 행 끝 문장).
 */
public record GuestCount(int value) {

    public static final int MIN = 1;
    public static final int MAX = 100;

    public GuestCount {
        if (value < MIN || value > MAX) {
            throw new IllegalArgumentException(
                    "guestCount는 " + MIN + " 이상 " + MAX + " 이하여야 한다: " + value);
        }
    }

    public static GuestCount of(int value) {
        return new GuestCount(value);
    }
}
