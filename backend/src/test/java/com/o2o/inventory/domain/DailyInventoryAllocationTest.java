package com.o2o.inventory.domain;

import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.o2o.shared.RoomTypeId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * K4와 K5. 설계 근거: task-S9-booking 8-1절, 06-4 1-2 hold와 commit과 releaseHeld와
 * releaseSold, I1과 I1a. 앞 묶음 8-1절 V2가 이월된 자리다. 앞 묶음이 I1a 검사 코드를
 * DailyInventory에 넣어 두었고 이번에 그 검사에 닿는 메서드 넷이 생겼다.
 *
 * 기존 DailyInventoryTest에 붙이지 않고 따로 둔 이유는 그 파일이 앞 묶음의 산출물이라서다.
 * 이 묶음의 변경 허용 범위는 inventory의 domain과 infrastructure 코드이고 앞 묶음의 테스트를
 * 고치지 않는다(계약 3절).
 */
class DailyInventoryAllocationTest {

    private static final Instant NOW = Instant.parse("2026-10-01T00:00:00Z");
    private static final Instant LATER = Instant.parse("2026-10-01T00:01:00Z");
    private static final RoomTypeId ROOM_TYPE_ID = RoomTypeId.of("room_test");
    private static final LocalDate STAY_DATE = LocalDate.parse("2026-10-10");

    @Test
    void K4_가용이_있으면_선점이_1_오르고_총량과_판매는_그대로다() {
        DailyInventory inventory = DailyInventory.open(ROOM_TYPE_ID, STAY_DATE, 2, NOW);

        inventory.hold(1, LATER);

        assertEquals(1, inventory.heldCount());
        assertEquals(2, inventory.totalCount());
        assertEquals(0, inventory.soldCount());
        assertEquals(1, inventory.availableCount());
        assertEquals(LATER, inventory.updatedAt());
    }

    @Test
    void K4_가용이_0이면_선점을_거절하고_수량이_그대로다() {
        // I1. 총량 1에 선점 1이면 가용 0이다. 두 번째 hold가 InventoryShortage다(06-4 1-2 hold)
        DailyInventory inventory = DailyInventory.open(ROOM_TYPE_ID, STAY_DATE, 1, NOW);
        inventory.hold(1, NOW);

        assertThrows(InventoryShortageException.class, () -> inventory.hold(1, LATER));

        assertEquals(1, inventory.heldCount());
        assertEquals(NOW, inventory.updatedAt());
    }

    @Test
    void K4_총량_0인_행은_선점할_수_없다() {
        // 11 명세 139행이 totalCount 0 등록을 허용하고 P10이 재고 0을 판매 불가로 적는다
        DailyInventory inventory = DailyInventory.open(ROOM_TYPE_ID, STAY_DATE, 0, NOW);

        assertThrows(InventoryShortageException.class, () -> inventory.hold(1, NOW));
    }

    @Test
    void K4_수량_0이하의_선점은_거절한다() {
        DailyInventory inventory = DailyInventory.open(ROOM_TYPE_ID, STAY_DATE, 5, NOW);

        assertThrows(IllegalArgumentException.class, () -> inventory.hold(0, NOW));
        assertThrows(IllegalArgumentException.class, () -> inventory.hold(-1, NOW));
    }

    @Test
    void K5_선점된_수만큼_판매로_옮긴다() {
        // 06-4 1-2 commit(n)의 Post. 선점이 줄고 판매가 는다. 총량은 그대로다
        DailyInventory inventory = DailyInventory.open(ROOM_TYPE_ID, STAY_DATE, 3, NOW);
        inventory.hold(1, NOW);

        inventory.commit(1, LATER);

        assertEquals(0, inventory.heldCount());
        assertEquals(1, inventory.soldCount());
        assertEquals(3, inventory.totalCount());
        assertEquals(2, inventory.availableCount());
    }

    @Test
    void K5_선점이_0이면_판매로_옮길_수_없다() {
        // I1a. heldCount가 음수로 가는 길을 막는다. 06-4 1-2 commit의 InsufficientHold
        DailyInventory inventory = DailyInventory.open(ROOM_TYPE_ID, STAY_DATE, 3, NOW);

        assertThrows(InsufficientHoldException.class, () -> inventory.commit(1, NOW));

        assertEquals(0, inventory.soldCount());
        assertEquals(0, inventory.heldCount());
    }

    @Test
    void K5_선점을_반환하면_선점만_준다() {
        DailyInventory inventory = DailyInventory.open(ROOM_TYPE_ID, STAY_DATE, 3, NOW);
        inventory.hold(1, NOW);

        inventory.releaseHeld(1, LATER);

        assertEquals(0, inventory.heldCount());
        assertEquals(0, inventory.soldCount());
        assertEquals(3, inventory.availableCount());
    }

    @Test
    void K5_선점이_0이면_반환할_수_없다() {
        // I1a. 06-4 1-2 releaseHeld의 InsufficientHold. 만료를 두 번 처리해도 음수가 안 된다
        DailyInventory inventory = DailyInventory.open(ROOM_TYPE_ID, STAY_DATE, 3, NOW);

        assertThrows(InsufficientHoldException.class, () -> inventory.releaseHeld(1, NOW));

        assertEquals(0, inventory.heldCount());
    }

    @Test
    void K5_판매분을_반환하면_판매만_준다() {
        DailyInventory inventory = DailyInventory.open(ROOM_TYPE_ID, STAY_DATE, 3, NOW);
        inventory.hold(1, NOW);
        inventory.commit(1, NOW);

        inventory.releaseSold(1, LATER);

        assertEquals(0, inventory.soldCount());
        assertEquals(0, inventory.heldCount());
        assertEquals(3, inventory.availableCount());
    }

    @Test
    void K5_판매가_0이면_반환할_수_없다() {
        // I1a. 06-4 1-2 releaseSold의 InsufficientSold. 취소를 두 번 처리해도 음수가 안 된다
        DailyInventory inventory = DailyInventory.open(ROOM_TYPE_ID, STAY_DATE, 3, NOW);

        assertThrows(InsufficientSoldException.class, () -> inventory.releaseSold(1, NOW));

        assertEquals(0, inventory.soldCount());
    }
}
