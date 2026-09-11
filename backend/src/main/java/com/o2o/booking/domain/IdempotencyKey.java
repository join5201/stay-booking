package com.o2o.booking.domain;

import java.util.regex.Pattern;

/**
 * 멱등키. 설계 근거: 11 명세 멱등 처리 절 머리. 8자 이상 128자 이하의 영문, 숫자, 하이픈,
 * 밑줄. 형식이 틀리면 400 IDEMPOTENCY_KEY_REQUIRED다(BOOK-01 에러 표). 형식 검사는 06-4 1-4로는
 * 컨트롤러 몫이지만 값의 규칙이라 값 객체가 한 번 더 지킨다. 다른 입구가 생겨도 뚫리지 않는다.
 */
public record IdempotencyKey(String value) {

    private static final Pattern SHAPE = Pattern.compile("^[A-Za-z0-9_-]{8,128}$");

    public IdempotencyKey {
        if (value == null || !SHAPE.matcher(value).matches()) {
            throw new InvalidIdempotencyKeyException(value);
        }
    }

    public static IdempotencyKey of(String value) {
        return new IdempotencyKey(value);
    }
}
