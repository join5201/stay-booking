package com.o2o.catalog.application;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.o2o.catalog.domain.Property;
import com.o2o.catalog.domain.PropertyNotFoundException;
import com.o2o.catalog.domain.RoomType;
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
class CatalogApplicationServiceTest {

    private static final HostId HOST = HostId.of("host_001");

    @Autowired
    private CatalogApplicationService catalogApplicationService;

    @Test
    void 없는_숙소에_객실_타입을_등록하면_거절한다() {
        // C2 실패 케이스. 06-4 1-2 registerRoomType의 위반 시 예외 PropertyNotFound
        PropertyId 없는_숙소 = PropertyId.of("prop_없는것");

        assertThrows(PropertyNotFoundException.class,
                () -> catalogApplicationService.registerRoomType(없는_숙소, "스탠다드", 2, ""));
    }

    @Test
    void 있는_숙소에_객실_타입을_등록하면_생성한다() {
        // C2 통과 케이스
        Property property = catalogApplicationService.registerProperty(
                HOST, "서울 스테이", "SEOUL", "서울특별시 종로구 예시로 10", "");

        RoomType roomType = catalogApplicationService.registerRoomType(
                property.id(), "스탠다드 더블", 2, "2인 객실");

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
}
