package com.o2o.inventory.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.o2o.catalog.application.CatalogApplicationService;
import com.o2o.catalog.domain.Property;
import com.o2o.catalog.domain.RoomType;
import com.o2o.inventory.application.InventoryApplicationService;
import com.o2o.shared.HostId;
import com.o2o.shared.RoomTypeId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * L4. 설계 근거: task-S9-booking-lifecycle 8-1절, 06-4 1-2 commit과 releaseHeld와 releaseSold 행,
 * I1과 I1a, A1(한 행이라도 위반이면 전체 롤백), 08-3 결정 3(재고 N행은 날짜 오름차순 잠금).
 *
 * DailyInventoryAllocationTest(1차 K4와 K5)가 행 하나의 규칙을 봤다면 여기는 N행 적용과 그 원자성을
 * 본다. 원자성은 실제 MySQL에서 본다(T3). 셋째 날이 부족해 예외가 나면 첫째 날에 이미 적용한
 * 변경이 롤백으로 지워져야 한다. 도메인 서비스는 트랜잭션을 열지 않으므로 테스트가 TransactionTemplate
 * 으로 감싼다. 호출자(예약의 BookingLifecycle)도 그렇게 부른다.
 */
@SpringBootTest
class InventoryAllocationServiceTest {

    private static final HostId HOST = HostId.of("host_001");
    private static final LocalDate CHECK_IN = LocalDate.parse("2026-10-10");
    private static final LocalDate CHECK_OUT = CHECK_IN.plusDays(3);
    private static final Instant NOW = Instant.parse("2026-10-01T20:00:00Z");

    @Autowired
    private InventoryAllocationService allocationService;

    @Autowired
    private InventoryApplicationService inventoryApplicationService;

    @Autowired
    private CatalogApplicationService catalogApplicationService;

    @Autowired
    private DailyInventoryRepository inventoryRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void L4_commit은_N행_전부에서_선점을_판매로_옮긴다() {
        RoomTypeId roomTypeId = 객실_타입();
        재고(roomTypeId, 2, 2, 2);
        tx().executeWithoutResult((status) -> allocationService.hold(roomTypeId, CHECK_IN, CHECK_OUT, 1, NOW));

        List<DailyInventory> committed = tx().execute(
                (status) -> allocationService.commit(roomTypeId, CHECK_IN, CHECK_OUT, 1, NOW));

        assertEquals(3, committed.size());
        assertEquals(List.of(CHECK_IN, CHECK_IN.plusDays(1), CHECK_IN.plusDays(2)),
                committed.stream().map(DailyInventory::stayDate).toList());
        for (int i = 0; i < 3; i++) {
            DailyInventory row = 행(roomTypeId, CHECK_IN.plusDays(i));
            assertEquals(0, row.heldCount(), "day " + i);
            assertEquals(1, row.soldCount(), "day " + i);
            assertEquals(2, row.totalCount(), "day " + i);
        }
    }

    @Test
    void L4_releaseHeld는_N행_전부에서_선점을_되돌리고_releaseSold는_판매를_되돌린다() {
        RoomTypeId roomTypeId = 객실_타입();
        재고(roomTypeId, 2, 2, 2);
        tx().executeWithoutResult((status) -> allocationService.hold(roomTypeId, CHECK_IN, CHECK_OUT, 1, NOW));

        tx().executeWithoutResult((status) -> allocationService.releaseHeld(roomTypeId, CHECK_IN, CHECK_OUT, 1, NOW));
        for (int i = 0; i < 3; i++) {
            assertEquals(0, 행(roomTypeId, CHECK_IN.plusDays(i)).heldCount(), "held day " + i);
            assertEquals(2, 행(roomTypeId, CHECK_IN.plusDays(i)).availableCount(), "available day " + i);
        }

        // 판매분 반환의 짝. 선점 뒤 확정한 것을 되돌린다
        tx().executeWithoutResult((status) -> {
            allocationService.hold(roomTypeId, CHECK_IN, CHECK_OUT, 1, NOW);
            allocationService.commit(roomTypeId, CHECK_IN, CHECK_OUT, 1, NOW);
        });
        assertEquals(1, 행(roomTypeId, CHECK_IN.plusDays(2)).soldCount());

        tx().executeWithoutResult((status) -> allocationService.releaseSold(roomTypeId, CHECK_IN, CHECK_OUT, 1, NOW));
        for (int i = 0; i < 3; i++) {
            assertEquals(0, 행(roomTypeId, CHECK_IN.plusDays(i)).soldCount(), "sold day " + i);
            assertEquals(2, 행(roomTypeId, CHECK_IN.plusDays(i)).availableCount(), "available day " + i);
        }
    }

    @Test
    void L4_셋째_날_선점이_부족하면_commit이_예외로_나가고_첫째_날의_이동도_남지_않는다() {
        // A1. 첫째와 둘째 날만 선점하고 셋째 날은 선점이 없다. 순서대로 적용하다 셋째에서 깨진다
        RoomTypeId roomTypeId = 객실_타입();
        재고(roomTypeId, 2, 2, 2);
        tx().executeWithoutResult((status) -> allocationService.hold(roomTypeId, CHECK_IN, CHECK_IN.plusDays(2), 1, NOW));

        assertThrows(InsufficientHoldException.class, () -> tx().executeWithoutResult(
                (status) -> allocationService.commit(roomTypeId, CHECK_IN, CHECK_OUT, 1, NOW)));

        assertEquals(1, 행(roomTypeId, CHECK_IN).heldCount());
        assertEquals(0, 행(roomTypeId, CHECK_IN).soldCount());
        assertEquals(1, 행(roomTypeId, CHECK_IN.plusDays(1)).heldCount());
        assertEquals(0, 행(roomTypeId, CHECK_IN.plusDays(2)).heldCount());
    }

    @Test
    void L4_셋째_날_판매가_없으면_releaseSold가_예외로_나가고_첫째_날의_반환도_남지_않는다() {
        RoomTypeId roomTypeId = 객실_타입();
        재고(roomTypeId, 2, 2, 2);
        tx().executeWithoutResult((status) -> {
            allocationService.hold(roomTypeId, CHECK_IN, CHECK_IN.plusDays(2), 1, NOW);
            allocationService.commit(roomTypeId, CHECK_IN, CHECK_IN.plusDays(2), 1, NOW);
        });

        assertThrows(InsufficientSoldException.class, () -> tx().executeWithoutResult(
                (status) -> allocationService.releaseSold(roomTypeId, CHECK_IN, CHECK_OUT, 1, NOW)));

        assertEquals(1, 행(roomTypeId, CHECK_IN).soldCount());
        assertEquals(1, 행(roomTypeId, CHECK_IN.plusDays(1)).soldCount());
        assertEquals(0, 행(roomTypeId, CHECK_IN.plusDays(2)).soldCount());
    }

    @Test
    void L4_빠진_날짜가_있으면_잠금_단계에서_거절한다() {
        // 셋째 날 재고 행이 없다. 적용 전 잠금이 InventoryNotOpened를 낸다. 앞 날짜는 그대로다
        RoomTypeId roomTypeId = 객실_타입();
        재고(roomTypeId, 2, 2);
        tx().executeWithoutResult((status) -> allocationService.hold(roomTypeId, CHECK_IN, CHECK_IN.plusDays(2), 1, NOW));

        assertThrows(InventoryNotOpenedException.class, () -> tx().executeWithoutResult(
                (status) -> allocationService.releaseHeld(roomTypeId, CHECK_IN, CHECK_OUT, 1, NOW)));

        assertEquals(1, 행(roomTypeId, CHECK_IN).heldCount());
        assertEquals(1, 행(roomTypeId, CHECK_IN.plusDays(1)).heldCount());
    }

    private TransactionTemplate tx() {
        return new TransactionTemplate(transactionManager);
    }

    private RoomTypeId 객실_타입() {
        String region = "T" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        Property property = catalogApplicationService.registerProperty(
                HOST, "N행 테스트 스테이", region, "서울특별시 중구 예시로 1", "");
        RoomType roomType = catalogApplicationService.registerRoomType(
                HOST, property.id(), "스탠다드", 2, "");
        return roomType.id();
    }

    /** CHECK_IN부터 날짜마다 총량을 준 만큼 연다 */
    private void 재고(RoomTypeId roomTypeId, int... totals) {
        for (int i = 0; i < totals.length; i++) {
            inventoryApplicationService.openInventory(HOST, roomTypeId, CHECK_IN.plusDays(i), totals[i]);
        }
    }

    private DailyInventory 행(RoomTypeId roomTypeId, LocalDate stayDate) {
        return inventoryRepository.findByRoomTypeIdAndStayDate(roomTypeId, stayDate).orElseThrow();
    }
}
