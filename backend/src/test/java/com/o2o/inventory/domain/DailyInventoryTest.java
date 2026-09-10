package com.o2o.inventory.domain;

import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.o2o.shared.RoomTypeId;
import com.o2o.shared.VersionConflictException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * V1의 도메인 몫. 설계 근거: I1과 I1a (06-2 3-1, 06-4 1-1), 06-4 1-2 openInventory와 adjust.
 *
 * 스프링을 띄우지 않는다. I1은 애그리거트가 스스로 지키는 규칙이라 컨테이너도 DB도 필요 없다.
 * 06-4 1-4가 규칙 검증을 애그리거트에 두는 것이 그 뜻이다.
 *
 * V1의 나머지 몫은 InventoryApplicationServiceTest에 있다. 판매 수가 0보다 큰 상태는
 * 이번 묶음의 코드로는 만들 수 없고 DB에 직접 넣어야 만들어진다. 여기서는 총 수량 자체가
 * 음수인 경우만 이 층에서 막힌다.
 *
 * 실패 케이스와 통과 케이스를 짝으로 붙인다(F9). 금지만 검사하면 가드가 허용 값까지 막아도
 * 초록이 뜬다.
 */
class DailyInventoryTest {

    private static final Instant NOW = Instant.parse("2026-10-01T00:00:00Z");
    private static final RoomTypeId ROOM_TYPE_ID = RoomTypeId.of("room_test");
    private static final LocalDate STAY_DATE = LocalDate.parse("2026-10-10");

    @Test
    void 총_수량이_음수면_거절한다() {
        // I1. 판매와 선점이 0인 새 행이라 음수 총 수량이 곧 I1 위반이다
        assertThrows(InventoryBelowOccupiedException.class,
                () -> DailyInventory.open(ROOM_TYPE_ID, STAY_DATE, -1, NOW));
    }

    @Test
    void 총_수량이_0이면_개설한다() {
        // 06-4 1-2 openInventory의 Pre는 totalCount 양수를 [가설]로 적고 11 명세 139행이
        // totalCount 0 등록을 허용한다. 계약 6절이 명세를 따르기로 확정했다
        DailyInventory inventory = DailyInventory.open(ROOM_TYPE_ID, STAY_DATE, 0, NOW);

        assertEquals(0, inventory.totalCount());
        assertEquals(0, inventory.availableCount());
        assertEquals(0L, inventory.version());
    }

    @Test
    void 개설하면_판매와_선점이_0이다() {
        // 06-4 1-2 openInventory의 Post 열. 11 INV-01 처리 규칙도 입력받지 않는다고 적는다
        DailyInventory inventory = DailyInventory.open(ROOM_TYPE_ID, STAY_DATE, 5, NOW);

        assertEquals(0, inventory.soldCount());
        assertEquals(0, inventory.heldCount());
        assertEquals(5, inventory.availableCount());
    }

    @Test
    void 조정하면_총_수량과_버전이_바뀐다() {
        DailyInventory inventory = DailyInventory.open(ROOM_TYPE_ID, STAY_DATE, 5, NOW);

        inventory.adjust(0L, 8, NOW.plusSeconds(60));

        assertEquals(8, inventory.totalCount());
        assertEquals(1L, inventory.version());
        assertEquals(8, inventory.availableCount());
    }

    @Test
    void 지난_버전으로_조정하면_거절한다() {
        // T05. 계약 7절 D-2의 version 대조 몫. 잠금과 달리 오래된 화면의 요청을 막는다
        DailyInventory inventory = DailyInventory.open(ROOM_TYPE_ID, STAY_DATE, 5, NOW);
        inventory.adjust(0L, 8, NOW.plusSeconds(60));

        assertThrows(VersionConflictException.class,
                () -> inventory.adjust(0L, 9, NOW.plusSeconds(120)));
        assertEquals(8, inventory.totalCount());
    }

    @Test
    void 조정으로_총_수량을_음수로_내리면_거절한다() {
        DailyInventory inventory = DailyInventory.open(ROOM_TYPE_ID, STAY_DATE, 5, NOW);

        assertThrows(InventoryBelowOccupiedException.class,
                () -> inventory.adjust(0L, -1, NOW.plusSeconds(60)));
        assertEquals(5, inventory.totalCount());
        assertEquals(0L, inventory.version());
    }

    @Test
    void 조정으로_0까지는_내릴_수_있다() {
        // 거절 케이스의 짝이다. 점유가 없으면 0까지 내려도 I1이 깨지지 않는다
        DailyInventory inventory = DailyInventory.open(ROOM_TYPE_ID, STAY_DATE, 5, NOW);

        inventory.adjust(0L, 0, NOW.plusSeconds(60));

        assertEquals(0, inventory.totalCount());
        assertEquals(1L, inventory.version());
    }
}
