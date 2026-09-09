package com.o2o.shared;

import java.util.List;
import java.util.UUID;

/**
 * 공통 오류 응답. 설계 근거: 11 에러 응답 절.
 *
 * 그 절이 code와 message와 traceId와 details 넷을 적고, details가 없으면 빈 배열이라고
 * 못박는다. 프론트 분기는 message가 아니라 code를 쓴다고도 적혀 있어서 code를 바꾸면
 * 화면이 깨진다.
 *
 * traceId의 형식은 어느 문서도 정하지 않았다. 문자열이라는 것만 있다. 그래서 UUID를 쓰고
 * 이 줄에 그 사실을 적어 둔다. 로그 상관 체계가 생기면 그때 맞춘다.
 */
public record ErrorResponse(String code, String message, String traceId, List<ErrorDetail> details) {

    public record ErrorDetail(String field, String reason) {
    }

    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(code, message, newTraceId(), List.of());
    }

    public static ErrorResponse of(String code, String message, List<ErrorDetail> details) {
        return new ErrorResponse(code, message, newTraceId(), details);
    }

    private static String newTraceId() {
        return "trace_" + UUID.randomUUID().toString().replace("-", "");
    }
}
