package com.o2o.inventory.application;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.annotation.Transactional;

import com.o2o.catalog.application.CatalogApplicationService;
import com.o2o.catalog.domain.Property;
import com.o2o.catalog.domain.RoomType;
import com.o2o.inventory.domain.InventoryAdjusted;
import com.o2o.inventory.domain.InventoryOpened;
import com.o2o.inventory.domain.RateAdjusted;
import com.o2o.inventory.domain.RateRegistered;
import com.o2o.shared.HostId;
import com.o2o.shared.RoomTypeId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * V12. 설계 근거: 06-4 1-2 계약표의 Post 열이 네 연산마다 발행할 이벤트를 적는다.
 * openInventory는 InventoryOpened, adjust는 InventoryAdjusted, registerRate는 RateRegistered,
 * adjustRate는 RateAdjusted다.
 *
 * 왜 이걸 따로 보나. 이벤트는 지금 아무도 구독하지 않는다. 구독자가 없으면 발행을 빠뜨려도
 * 다른 테스트가 전부 초록이다. 예약과 결제 컨텍스트가 붙는 순간 조용히 깨진다. 계약표가
 * 약속한 것이라 약속한 자리에서 확인한다.
 *
 * 발행을 세는 도구는 스프링 테스트의 ApplicationEvents다. 발행 자체를 보는 것이라
 * 가짜 구독자를 만드는 것보다 확인하려는 것에 가깝다.
 *
 * 실패 쪽 짝도 둔다. 거절된 요청은 이벤트를 내지 않아야 한다. 사실이 아닌 것을 알리면
 * 구독자가 없는 지금은 조용하고 붙은 뒤에 재고가 어긋난다.
 */
@SpringBootTest
@Transactional
@RecordApplicationEvents
class InventoryEventTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-10-01T00:00:00Z");
    private static final HostId HOST = HostId.of("host_001");
    private static final LocalDate STAY_DATE = LocalDate.parse("2026-10-10");

    @TestConfiguration
    static class FixedClockConfiguration {

        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
        }
    }

    @Autowired
    private InventoryApplicationService inventoryApplicationService;

    @Autowired
    private CatalogApplicationService catalogApplicationService;

    @Autowired
    private ApplicationEvents events;

    @Test
    void 재고를_개설하면_InventoryOpened가_난다() {
        RoomTypeId roomTypeId = 내_객실_타입();

        inventoryApplicationService.openInventory(HOST, roomTypeId, STAY_DATE, 5);

        assertEquals(1, events.stream(InventoryOpened.class).count());
        InventoryOpened event = events.stream(InventoryOpened.class).findFirst().orElseThrow();
        assertEquals(roomTypeId, event.roomTypeId());
        assertEquals(STAY_DATE, event.stayDate());
        assertEquals(5, event.totalCount());
    }

    @Test
    void 일괄_개설은_날짜마다_InventoryOpened가_난다() {
        // 이벤트는 행 단위 사실이라 행마다 낸다. 기간 하나에 하나가 아니다
        RoomTypeId roomTypeId = 내_객실_타입();

        inventoryApplicationService.openInventories(
                HOST, roomTypeId, STAY_DATE, STAY_DATE.plusDays(3), 5);

        assertEquals(3, events.stream(InventoryOpened.class).count());
    }

    @Test
    void 재고를_조정하면_InventoryAdjusted가_난다() {
        RoomTypeId roomTypeId = 내_객실_타입();
        inventoryApplicationService.openInventory(HOST, roomTypeId, STAY_DATE, 5);

        inventoryApplicationService.adjustInventory(HOST, roomTypeId, STAY_DATE, 0L, 8);

        assertEquals(1, events.stream(InventoryAdjusted.class).count());
        assertEquals(8, events.stream(InventoryAdjusted.class).findFirst().orElseThrow()
                .totalCount());
    }

    @Test
    void 요금을_등록하고_조정하면_각각_이벤트가_난다() {
        RoomTypeId roomTypeId = 내_객실_타입();

        inventoryApplicationService.registerRate(HOST, roomTypeId, STAY_DATE, 100_000);
        inventoryApplicationService.adjustRate(HOST, roomTypeId, STAY_DATE, 0L, 120_000);

        assertEquals(1, events.stream(RateRegistered.class).count());
        assertEquals(1, events.stream(RateAdjusted.class).count());
        assertEquals(120_000L, events.stream(RateAdjusted.class).findFirst().orElseThrow()
                .rate().amount());
    }

    @Test
    void 거절된_조정은_이벤트를_내지_않는다() {
        // 발행 확인의 짝이다. 항상 내는 코드도 발행 검사만 보면 초록이다
        RoomTypeId roomTypeId = 내_객실_타입();
        inventoryApplicationService.openInventory(HOST, roomTypeId, STAY_DATE, 5);

        assertThrows(RuntimeException.class, () -> inventoryApplicationService.adjustInventory(
                HOST, roomTypeId, STAY_DATE, 99L, 8));

        assertEquals(0, events.stream(InventoryAdjusted.class).count());
    }

    private RoomTypeId 내_객실_타입() {
        Property property = catalogApplicationService.registerProperty(
                HOST, "이벤트 테스트 스테이", "SEOUL", "서울특별시 중구 예시로 4", "");
        RoomType roomType = catalogApplicationService.registerRoomType(
                HOST, property.id(), "스탠다드", 2, "");
        return roomType.id();
    }
}
