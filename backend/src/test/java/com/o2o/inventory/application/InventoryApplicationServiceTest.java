package com.o2o.inventory.application;

import java.sql.Date;
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
import org.springframework.transaction.annotation.Transactional;

import com.o2o.catalog.application.CatalogApplicationService;
import com.o2o.catalog.domain.Property;
import com.o2o.catalog.domain.RoomType;
import com.o2o.catalog.domain.RoomTypeNotFoundException;
import com.o2o.inventory.domain.DailyInventory;
import com.o2o.inventory.domain.DailyInventoryRepository;
import com.o2o.inventory.domain.DuplicateInventoryException;
import com.o2o.inventory.domain.DuplicateRateException;
import com.o2o.inventory.domain.InventoryBelowOccupiedException;
import com.o2o.shared.HostId;
import com.o2o.shared.RoomTypeId;

import jakarta.persistence.EntityManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * V1과 V4. 설계 근거: 계약 8-1절, 06-4 1-2 adjust와 openInventory와 registerRate, T04.
 *
 * 실제 MySQL에 붙는다. 메모리 저장소로 대신하지 않는 근거는 eval-criteria-code.md의 테스트
 * 격리와 재현성 축이다. V4가 확인하는 유일성은 DB 책임이고(06-4 1-4) 진짜 DB에서만 확인된다.
 * 트랜잭션 롤백으로 데이터를 정리한다.
 *
 * V1이 여기 있는 이유. 판매 수가 0보다 큰 재고 행은 이번 묶음의 코드로 만들 수 없다.
 * 그 값을 올리는 행동이 hold와 commit이고 둘 다 계약 2-2절로 범위 밖이다. 그래서 상태를
 * DB에 직접 넣어 만든다. 이것은 프로덕션에 없는 경로를 테스트하는 것이 아니라 프로덕션에서
 * 예약 묶음이 만들 상태를 미리 놓는 것이다. T04가 요구하는 시나리오가 정확히 그 상태다.
 *
 * 시각을 고정한다. 같은 축이 시간 제어를 요구하고, 앱 서비스의 과거 날짜 검사가 서버의
 * 오늘을 기준으로 하기 때문이다. 고정하지 않으면 며칠 뒤에 이 테스트가 다른 것을 검사한다.
 */
@SpringBootTest
@Transactional
class InventoryApplicationServiceTest {

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
    private DailyInventoryRepository inventoryRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void 판매분_아래로_조정하면_거절하고_수량을_유지한다() {
        // V1과 T04. 06-4 1-2 adjust의 Pre가 바꾼 totalCount는 soldCount + heldCount 이상이라 적는다
        RoomTypeId roomTypeId = 내_객실_타입();
        DailyInventory opened = inventoryApplicationService.openInventory(
                HOST, roomTypeId, STAY_DATE, 5);
        판매분을_직접_올린다(roomTypeId, STAY_DATE, 3);

        assertThrows(InventoryBelowOccupiedException.class,
                () -> inventoryApplicationService.adjustInventory(
                        HOST, roomTypeId, STAY_DATE, opened.version(), 2));

        entityManager.clear();
        DailyInventory 현재 = inventoryRepository.findByRoomTypeIdAndStayDate(roomTypeId, STAY_DATE)
                .orElseThrow();
        assertEquals(5, 현재.totalCount());
        assertEquals(0L, 현재.version());
    }

    @Test
    void 판매분과_같은_값으로는_조정할_수_있다() {
        // 거절 케이스의 짝이다. 경계에서 막히면 안 된다. I1은 이상이지 초과가 아니다
        RoomTypeId roomTypeId = 내_객실_타입();
        DailyInventory opened = inventoryApplicationService.openInventory(
                HOST, roomTypeId, STAY_DATE, 5);
        판매분을_직접_올린다(roomTypeId, STAY_DATE, 3);

        DailyInventory 조정됨 = inventoryApplicationService.adjustInventory(
                HOST, roomTypeId, STAY_DATE, opened.version(), 3);

        assertEquals(3, 조정됨.totalCount());
        assertEquals(0, 조정됨.availableCount());
        assertEquals(1L, 조정됨.version());
    }

    @Test
    void 같은_날짜에_재고를_두_번_등록하면_거절한다() {
        // V4. U2. 11 INV-01의 409 RESOURCE_ALREADY_EXISTS와 처리 규칙 덮어쓰지 않는다
        RoomTypeId roomTypeId = 내_객실_타입();
        inventoryApplicationService.openInventory(HOST, roomTypeId, STAY_DATE, 5);

        assertThrows(DuplicateInventoryException.class,
                () -> inventoryApplicationService.openInventory(HOST, roomTypeId, STAY_DATE, 9));

        entityManager.clear();
        assertEquals(5, inventoryRepository
                .findByRoomTypeIdAndStayDate(roomTypeId, STAY_DATE).orElseThrow().totalCount());
    }

    @Test
    void 다른_날짜에는_재고를_등록한다() {
        // 거절 케이스의 짝이다. 유니크가 날짜까지 묶은 조합이라는 것을 확인한다
        RoomTypeId roomTypeId = 내_객실_타입();
        inventoryApplicationService.openInventory(HOST, roomTypeId, STAY_DATE, 5);

        DailyInventory 다음날 = inventoryApplicationService.openInventory(
                HOST, roomTypeId, STAY_DATE.plusDays(1), 7);

        assertEquals(7, 다음날.totalCount());
        assertEquals(STAY_DATE.plusDays(1), 다음날.stayDate());
    }

    @Test
    void 같은_날짜에_요금을_두_번_등록하면_거절한다() {
        // V4의 요금 몫. 06-4 1-2 registerRate의 위반 시 예외 DuplicateRate
        RoomTypeId roomTypeId = 내_객실_타입();
        inventoryApplicationService.registerRate(HOST, roomTypeId, STAY_DATE, 100_000);

        assertThrows(DuplicateRateException.class,
                () -> inventoryApplicationService.registerRate(
                        HOST, roomTypeId, STAY_DATE, 120_000));
    }

    @Test
    void 남의_객실_타입에_재고를_등록하면_거절한다() {
        // T02. 11 인증과 접근 제어가 다른 사용자 소유 자원을 404로 적는다.
        // 06-2 6절 CRC의 세 번째 책임 행이 요구하는 카탈로그 존재 확인과 같은 자리다
        RoomTypeId 남의_객실_타입 = 남의_객실_타입();

        assertThrows(RoomTypeNotFoundException.class,
                () -> inventoryApplicationService.openInventory(
                        HOST, 남의_객실_타입, STAY_DATE, 5));
    }

    @Test
    void 없는_객실_타입에_요금을_등록하면_거절한다() {
        // 06-1 R1. 카탈로그가 상류다. 남의 것과 같은 예외를 쓰는 것이 자원 정보를 흘리지 않는다는 규칙이다
        assertThrows(RoomTypeNotFoundException.class,
                () -> inventoryApplicationService.registerRate(
                        HOST, RoomTypeId.of("room_없는것"), STAY_DATE, 100_000));
    }

    private RoomTypeId 내_객실_타입() {
        Property property = catalogApplicationService.registerProperty(
                HOST, "테스트 스테이", "SEOUL", "서울특별시 중구 예시로 1", "");
        RoomType roomType = catalogApplicationService.registerRoomType(
                HOST, property.id(), "스탠다드", 2, "");
        return roomType.id();
    }

    private RoomTypeId 남의_객실_타입() {
        HostId 남 = HostId.of("host_002");
        Property property = catalogApplicationService.registerProperty(
                남, "남의 스테이", "SEOUL", "서울특별시 중구 예시로 2", "");
        RoomType roomType = catalogApplicationService.registerRoomType(
                남, property.id(), "스탠다드", 2, "");
        return roomType.id();
    }

    /**
     * 예약 묶음이 만들 상태를 미리 놓는다. 이번 묶음에 판매 수를 올리는 코드가 없어서
     * DB에 직접 넣는다. 근거는 이 클래스 머리 주석이다.
     */
    private void 판매분을_직접_올린다(RoomTypeId roomTypeId, LocalDate stayDate, int soldCount) {
        entityManager.flush();
        entityManager.createNativeQuery(
                        "update daily_inventory set sold_count = ?1 "
                                + "where room_type_id = ?2 and stay_date = ?3")
                .setParameter(1, soldCount)
                .setParameter(2, roomTypeId.value())
                .setParameter(3, Date.valueOf(stayDate))
                .executeUpdate();
        entityManager.clear();
    }
}
