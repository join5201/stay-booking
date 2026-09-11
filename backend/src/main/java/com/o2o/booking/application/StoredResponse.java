package com.o2o.booking.application;

import java.util.Objects;

/**
 * 멱등 기록에 저장하는 성공 응답. 설계 근거: 11 멱등 규칙 3(처음 저장한 성공 상태 코드,
 * Location과 body를 재전송한다). body는 api 층이 만든 JSON 문자열이다. 앱 서비스는 그것을
 * 저장하고 되돌려 줄 뿐 안을 읽지 않으므로 application이 api를 가리키지 않는다.
 */
public record StoredResponse(int status, String location, String body) {

    public StoredResponse {
        Objects.requireNonNull(body, "body는 null일 수 없다");
        if (status < 200 || status > 299) {
            throw new IllegalArgumentException("성공 응답만 저장한다: " + status);
        }
    }
}
