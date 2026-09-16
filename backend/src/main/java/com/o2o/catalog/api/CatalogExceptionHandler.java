package com.o2o.catalog.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.o2o.catalog.domain.InvalidOccupancyException;
import com.o2o.catalog.domain.PropertyNotFoundException;
import com.o2o.catalog.domain.RoomTypeNotFoundException;
import com.o2o.shared.ErrorResponse;

/**
 * 카탈로그 고유 예외를 11 에러 응답 표의 코드로 바꾼다.
 *
 * shared의 핸들러와 갈라 둔 이유는 의존 방향이다. shared가 catalog를 참조하면 공유 커널이
 * 한 컨텍스트에 묶인다.
 *
 * 없는 자원과 남의 자원이 같은 404 코드로 나가는 것이 의도다. 11 인증과 접근 제어가
 * 다른 사용자 소유 자원을 404로 적고 자원 정보를 흘리지 않는다. 둘을 가르면 남의 숙소가
 * 존재한다는 사실이 새어 나간다.
 */
@RestControllerAdvice
public class CatalogExceptionHandler {

    @ExceptionHandler({PropertyNotFoundException.class, RoomTypeNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleNotFound(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of("RESOURCE_NOT_FOUND", "자원을 찾을 수 없습니다."));
    }

    /**
     * I14 위반이 여기까지 오면 컨트롤러 검증이 샌 것이다. 11 에러 응답 표에 카탈로그 등록의
     * 인원 코드가 따로 없어 범위 위반과 같은 INVALID_REQUEST로 낸다. OCCUPANCY_EXCEEDED는
     * 예약의 인원 초과라 다른 자리다.
     */
    @ExceptionHandler(InvalidOccupancyException.class)
    public ResponseEntity<ErrorResponse> handleOccupancy(InvalidOccupancyException e) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of("INVALID_REQUEST", "최대 인원 값이 올바르지 않습니다."));
    }
}
