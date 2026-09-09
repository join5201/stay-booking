package com.o2o.catalog.application;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.annotation.Transactional;

import com.o2o.catalog.domain.Property;
import com.o2o.catalog.domain.PropertyNotFoundException;
import com.o2o.catalog.domain.PropertyRegistered;
import com.o2o.catalog.domain.RoomType;
import com.o2o.catalog.domain.RoomTypeRegistered;
import com.o2o.shared.HostId;
import com.o2o.shared.PropertyId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * C2와 C6의 앱 서비스 몫. 설계 근거: 계약 8-1절, 06-4 1-2 registerRoomType, P07.
 *
 * 실제 MySQL에 붙는다. 메모리 저장소로 대신하지 않는 근거는 eval-criteria-code.md의
 * 테스트 격리와 재현성 축이다. 유일성과 무결성은 DB 책임이고(06-4 1-4) 그 책임은
 * 진짜 DB에서만 확인된다.
 *
 * 트랜잭션 롤백으로 데이터를 정리한다. 같은 축이 테스트 뒤 데이터 정리를 요구한다.
 *
 * C6의 절반만 여기서 검사한다. 헤더 행위자가 소유자가 된다는 것은 앱 서비스가 hostId를
 * 인자로만 받는다는 사실로 확인되지만, body의 hostId가 무시된다는 것은 요청 body가 있는
 * 컨트롤러에서만 검사할 수 있다. 나머지 절반은 6단계다.
 */
@SpringBootTest
@Transactional
@RecordApplicationEvents
class CatalogApplicationServiceTest {

    private static final HostId HOST = HostId.of("host_001");

    @Autowired
    private CatalogApplicationService catalogApplicationService;

    @Autowired
    private ApplicationEvents events;

    @Test
    void 없는_숙소에_객실_타입을_등록하면_거절한다() {
        // C2 실패 케이스. 06-4 1-2 registerRoomType의 위반 시 예외 PropertyNotFound
        PropertyId 없는_숙소 = PropertyId.of("prop_없는것");

        assertThrows(PropertyNotFoundException.class,
                () -> catalogApplicationService.registerRoomType(HOST, 없는_숙소, "스탠다드", 2, ""));
    }

    @Test
    void 남의_숙소에_객실_타입을_등록하면_거절한다() {
        // C6의 앱 서비스 몫 둘째. 11 인증과 접근 제어가 다른 사용자 소유 자원을 404로 적는다.
        // 없는 숙소와 같은 예외를 쓰는 것이 자원 정보를 흘리지 않는다는 그 규칙이다
        Property 남의_숙소 = catalogApplicationService.registerProperty(
                HostId.of("host_002"), "남의 스테이", "SEOUL", "서울특별시 중구 예시로 1", "");

        assertThrows(PropertyNotFoundException.class,
                () -> catalogApplicationService.registerRoomType(HOST, 남의_숙소.id(), "스탠다드", 2, ""));
    }

    @Test
    void 있는_숙소에_객실_타입을_등록하면_생성한다() {
        // C2 통과 케이스
        Property property = catalogApplicationService.registerProperty(
                HOST, "서울 스테이", "SEOUL", "서울특별시 종로구 예시로 10", "");

        RoomType roomType = catalogApplicationService.registerRoomType(
                HOST, property.id(), "스탠다드 더블", 2, "2인 객실");

        assertNotNull(roomType.id());
        assertEquals(property.id(), roomType.propertyId());
    }

    @Test
    void 소유자는_인자로_받은_행위자다() {
        // C6의 앱 서비스 몫. 11 인증과 접근 제어가 소유자를 행위자 정보에서 채우라고 적는다.
        // 이 서비스는 소유자를 받을 통로가 hostId 인자 하나뿐이다
        Property property = catalogApplicationService.registerProperty(
                HOST, "부산 스테이", "BUSAN", "부산광역시 해운대구 예시로 20", "");

        assertEquals(HOST, property.hostId());
    }

    @Test
    void 등록한_숙소를_다시_조회할_수_있다() {
        // CAT-01과 CAT-03이 한 줄기로 이어지는지 본다. 11 CAT-03
        Property saved = catalogApplicationService.registerProperty(
                HOST, "제주 스테이", "JEJU", "제주특별자치도 제주시 예시로 30", "설명");

        Property found = catalogApplicationService.getProperty(saved.id());

        assertEquals(saved.id(), found.id());
        assertEquals("제주 스테이", found.name());
        assertEquals("JEJU", found.region().code());
    }

    @Test
    void 없는_숙소를_조회하면_거절한다() {
        assertThrows(PropertyNotFoundException.class,
                () -> catalogApplicationService.getProperty(PropertyId.of("prop_없는것")));
    }

    // ---------- 계약표 Post 열의 이벤트 발행 ----------

    @Test
    void 숙소를_등록하면_PropertyRegistered를_발행한다() {
        // 06-4 1-2 registerProperty의 Post 열. 2026-09-09까지 이 후행조건이 코드에 없었다
        Property property = catalogApplicationService.registerProperty(
                HOST, "이벤트 확인용", "SEOUL", "서울특별시 중구 예시로 11", "");

        List<PropertyRegistered> published =
                events.stream(PropertyRegistered.class).toList();
        assertEquals(1, published.size());
        assertEquals(property.id(), published.get(0).propertyId());
        assertEquals(HOST, published.get(0).hostId());
    }

    @Test
    void 객실_타입을_등록하면_RoomTypeRegistered를_발행한다() {
        // 06-4 1-2 registerRoomType의 Post 열
        Property property = catalogApplicationService.registerProperty(
                HOST, "이벤트 확인용 2", "SEOUL", "서울특별시 중구 예시로 12", "");

        RoomType roomType = catalogApplicationService.registerRoomType(
                HOST, property.id(), "스탠다드", 2, "");

        List<RoomTypeRegistered> published =
                events.stream(RoomTypeRegistered.class).toList();
        assertEquals(1, published.size());
        assertEquals(roomType.id(), published.get(0).roomTypeId());
        assertEquals(property.id(), published.get(0).propertyId());
    }

    @Test
    void 등록이_거절되면_이벤트를_발행하지_않는다() {
        // 실패 케이스와 짝이다(F9). 후행조건은 행동이 성공했을 때만 성립한다.
        // 이것이 없으면 예외 경로에서 이벤트가 새도 초록이 뜬다
        assertThrows(PropertyNotFoundException.class,
                () -> catalogApplicationService.registerRoomType(
                        HOST, PropertyId.of("prop_없는것"), "스탠다드", 2, ""));

        assertEquals(0, events.stream(RoomTypeRegistered.class).count());
    }
}
