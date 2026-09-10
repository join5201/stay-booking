package com.o2o.inventory.domain;

import java.time.LocalDate;

/**
 * 설계 근거: 11 INV-01과 INV-03과 RATE-01과 RATE-02의 등록일은 서버의 오늘 이상 제약(명세 198행).
 *
 * 06-4 1-2 계약표의 Pre 열에는 이 조건이 없다. 11 명세에만 있는 제약이라 그 사실을 적어 둔다.
 * 검사 위치가 앱 서비스인 근거는 서버의 오늘이 Clock에서 나오고 도메인은 Clock을 갖지 않기
 * 때문이다. 06-4 1-4는 형식 검증을 컨트롤러로 보내지만 이 검사는 형식이 아니라 서버 시각과의
 * 비교라 컨트롤러의 DTO 검증으로 내려가지 않는다.
 */
public class PastStayDateException extends RuntimeException {

    public PastStayDateException(LocalDate stayDate, LocalDate today) {
        super("등록과 수정 날짜는 오늘 이상이어야 한다. 요청 " + stayDate + ", 오늘 " + today);
    }
}
