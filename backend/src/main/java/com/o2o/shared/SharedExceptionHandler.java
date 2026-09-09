package com.o2o.shared;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 컨텍스트를 가리지 않는 오류를 11 에러 응답 표의 코드로 바꾼다.
 * 설계 근거: 11 에러 응답 표와 인증과 접근 제어.
 *
 * 카탈로그 고유 예외는 여기서 다루지 않는다. shared가 catalog를 참조하면 공유 커널이
 * 한 컨텍스트에 묶인다. 그쪽은 catalog/api의 핸들러가 맡는다.
 *
 * 8-1절 C9와 C10이 이 클래스를 검사한다.
 */
@RestControllerAdvice
public class SharedExceptionHandler {

    @ExceptionHandler(ActorRequiredException.class)
    public ResponseEntity<ErrorResponse> handleActorRequired(ActorRequiredException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.of("ACTOR_REQUIRED", "필요한 개발 행위자가 없거나 미등록입니다."));
    }

    @ExceptionHandler(ActorAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(ActorAccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of("ACCESS_DENIED", "이 작업에 필요한 역할이 아닙니다."));
    }

    /**
     * 필드 검증 실패. 11 에러 응답 표의 INVALID_REQUEST다. 어느 필드가 왜 걸렸는지를
     * details에 담는다. 같은 절이 field와 reason 두 칸을 적는다.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleInvalid(MethodArgumentNotValidException e) {
        List<ErrorResponse.ErrorDetail> details = e.getBindingResult().getFieldErrors().stream()
                .map((f) -> new ErrorResponse.ErrorDetail(f.getField(), f.getDefaultMessage()))
                .toList();
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of("INVALID_REQUEST", "요청 값이 올바르지 않습니다.", details));
    }

    /**
     * body를 읽지 못한 경우다. 명세에 없는 필드와 잘못된 타입이 여기로 온다.
     * 11 공통 요청과 응답 규칙이 셋을 한 줄로 묶어 400이라고 적는다. 8-1절 C9가 이것이다.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of("INVALID_REQUEST", "요청 본문을 읽을 수 없습니다."));
    }

    /**
     * 수정 버전 불일치. 11 에러 응답 표의 409 VERSION_CONFLICT다.
     * 2026-09-09 결정으로 동시 수정을 낙관적 잠금으로 처리하기로 했고 그 거절이 여기로 온다.
     * 8-2절 C4가 이 경로를 검사한다.
     */
    @ExceptionHandler(VersionConflictException.class)
    public ResponseEntity<ErrorResponse> handleVersionConflict(VersionConflictException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of("VERSION_CONFLICT", "수정 버전이 일치하지 않습니다."));
    }

    /**
     * 쪽 나눔 값의 범위 위반. 11 공통 목록과 날짜 범위가 잘못된 범위를 400으로 적는다.
     * PageQuery가 던지는 것을 여기서 받는다. 8-2절 C7이 이 경로를 검사한다.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of("INVALID_REQUEST", "요청 값의 범위가 올바르지 않습니다."));
    }
}
