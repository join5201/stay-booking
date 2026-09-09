package com.o2o.catalog.domain;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.o2o.shared.PropertyId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * C1. 설계 근거: I14 (06-2 3-1, 06-4 1-1). 최대 인원은 0보다 크다.
 * 계약 8-1절이 이 항목의 층을 도메인 불변식으로 적는다.
 *
 * 스프링을 띄우지 않는다. I14는 애그리거트가 스스로 지키는 규칙이라 컨테이너도 DB도
 * 필요 없다. 06-4 1-4가 규칙 검증을 애그리거트에 두는 것이 그 뜻이다.
 *
 * 실패 케이스와 통과 케이스를 짝으로 붙인다(F9). 금지만 검사하면 가드가 허용 값까지
 * 막아도 초록이 뜬다. 2026-09-08 이슈 13에서 실제로 겪은 실패 유형이다.
 */
class RoomTypeTest {

    private static final Instant NOW = Instant.parse("2026-10-01T03:00:00Z");
    private static final PropertyId PROPERTY_ID = PropertyId.of("prop_test");

    @Test
    void maxOccupancy가_0이면_거절한다() {
        assertThrows(InvalidOccupancyException.class,
                () -> RoomType.register(PROPERTY_ID, "스탠다드 더블", 0, "", NOW));
    }

    @Test
    void maxOccupancy가_음수면_거절한다() {
        assertThrows(InvalidOccupancyException.class,
                () -> RoomType.register(PROPERTY_ID, "스탠다드 더블", -1, "", NOW));
    }

    @Test
    void maxOccupancy가_1이면_생성한다() {
        RoomType roomType = RoomType.register(PROPERTY_ID, "스탠다드 싱글", 1, "", NOW);

        assertEquals(1, roomType.maxOccupancy());
        assertEquals(PROPERTY_ID, roomType.propertyId());
    }

    @Test
    void 상한인_100도_생성한다() {
        // 11 공통 요청과 응답 규칙이 maxOccupancy를 1 이상 100 이하로 적는다.
        // 상한 검사는 컨트롤러 몫이라 도메인은 100을 그대로 받는다(06-4 1-4)
        RoomType roomType = RoomType.register(PROPERTY_ID, "단체실", 100, "", NOW);

        assertEquals(100, roomType.maxOccupancy());
    }
}
