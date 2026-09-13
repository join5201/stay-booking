package com.o2o.payment.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.o2o.payment.domain.AmountMismatchException;
import com.o2o.payment.domain.InvalidAttemptTransitionException;
import com.o2o.payment.domain.MockEventConflictException;
import com.o2o.payment.domain.PgTransactionMismatchException;
import com.o2o.payment.domain.UnknownAttemptException;
import com.o2o.shared.ErrorResponse;

/**
 * 결제 예외를 11 에러 응답 표의 코드로 바꾼다. 설계 근거: 11 INTERNAL-01 에러 표와 중복과 결과
 * 처리 규칙 1과 2와 4, 11 에러 응답 표(112행부터), 계약 2절 검사 순서 3행과 5행부터 9행, 7절 D-4.
 *
 * 여기 있는 것은 INTERNAL-01이 내는 넷이다. openAttempt와 refund의 예외 넷(AttemptInProgress,
 * AttemptLimitExceeded, AlreadyApproved, NoApprovedAttempt)은 PAY-01과 취소를 받는 예약 2차가
 * 매핑한다(계약 2절). 처리기는 전역이라 예약 2차의 컨트롤러도 아래 넷은 그대로 쓴다.
 */
@RestControllerAdvice
public class PaymentExceptionHandler {

    /** 404 RESOURCE_NOT_FOUND. 없는 시도(규칙 1). ORPHAN 시도도 NORMAL이 아니라 같은 응답이다 */
    @ExceptionHandler(UnknownAttemptException.class)
    public ResponseEntity<ErrorResponse> handleUnknownAttempt(UnknownAttemptException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of("RESOURCE_NOT_FOUND", "자원을 찾을 수 없습니다."));
    }

    /**
     * 409 MOCK_EVENT_CONFLICT. 셋을 한 코드로 묶는다. 다른 거래 번호(규칙 1), 같은 eventId 다른
     * body(규칙 2), 종착 시도에 반대 결과(규칙 4. 7절 D-4). 11 에러 표가 처리된 거래의 다른 결과
     * 또는 같은 이벤트 ID의 다른 내용을 한 줄로 적는다.
     */
    @ExceptionHandler({PgTransactionMismatchException.class, MockEventConflictException.class,
            InvalidAttemptTransitionException.class})
    public ResponseEntity<ErrorResponse> handleMockEventConflict(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of("MOCK_EVENT_CONFLICT",
                        "처리된 거래의 다른 결과이거나 같은 이벤트 ID의 다른 내용입니다."));
    }

    /** 409 PAYMENT_AMOUNT_MISMATCH. 금액 또는 통화가 시도와 다르다(규칙 1) */
    @ExceptionHandler(AmountMismatchException.class)
    public ResponseEntity<ErrorResponse> handleAmountMismatch(AmountMismatchException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of("PAYMENT_AMOUNT_MISMATCH",
                        "Mock 거래 결과의 금액 또는 통화가 일치하지 않습니다."));
    }
}
