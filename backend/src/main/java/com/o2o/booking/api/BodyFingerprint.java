package com.o2o.booking.api;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * 멱등 규칙 2의 body 대조 값. 설계 근거: 11 멱등 처리 절(같은 키에 다른 body는 409), 1차 계약
 * BOOK-01 요청의 fingerprint 방식.
 *
 * JSON 원문이 아니라 형식 검증을 통과한 필드 값을 정한 순서로 이어 붙인 정규형의 sha256이다.
 * 키 순서나 공백이 달라도 값이 같으면 같은 body다. PAY-01과 BOOK-04의 요청 본문이 이것을 쓴다.
 * BOOK-01 요청은 1차에 같은 계산을 자기 안에 가지고 있고 이 묶음은 그 파일을 고치지 않는다.
 */
final class BodyFingerprint {

    private BodyFingerprint() {
    }

    static String sha256(String canonical) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256이 없다", e);
        }
    }
}
