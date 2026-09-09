package com.o2o.catalog.application;

import java.time.Clock;
import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.o2o.catalog.domain.Address;
import com.o2o.catalog.domain.Property;
import com.o2o.catalog.domain.PropertyNotFoundException;
import com.o2o.catalog.domain.PropertyRepository;
import com.o2o.catalog.domain.Region;
import com.o2o.catalog.domain.RoomType;
import com.o2o.catalog.domain.RoomTypeNotFoundException;
import com.o2o.catalog.domain.RoomTypeRepository;
import com.o2o.shared.HostId;
import com.o2o.shared.PropertyId;
import com.o2o.shared.RoomTypeId;

/**
 * 설계 근거: 06-2 6절 카탈로그 CRC의 CatalogApplicationService 책임 두 행.
 * 등록 플로우를 오케스트레이션하고 트랜잭션 경계를 열며, RegisterRoomType 처리 전에
 * 대상 Property의 존재를 확인한다.
 *
 * 조회 두 메서드는 CRC에 대응 행이 없다. 근거가 11 CAT-03과 CAT-08이다. CRC와 06-4 계약표는
 * 커맨드만 적고 조회는 커맨드가 아니라서 그 표에 자리가 없다.
 *
 * 시각을 Clock에서 받는 이유는 eval-criteria-code.md의 테스트 격리와 재현성 축이다.
 * 시간을 제어할 수 있어야 한다고 그 축이 요구한다. 도메인은 Instant를 받기만 한다.
 */
@Service
@Transactional
public class CatalogApplicationService {

    private final PropertyRepository propertyRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final Clock clock;

    public CatalogApplicationService(PropertyRepository propertyRepository,
                                     RoomTypeRepository roomTypeRepository,
                                     Clock clock) {
        this.propertyRepository = propertyRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.clock = clock;
    }

    /**
     * CAT-01. 설계 근거: 11 숙소 CAT-01, 06-4 1-2 registerProperty.
     *
     * hostId를 인자로 받고 요청 body에서 읽지 않는다. 11 인증과 접근 제어가 body의 hostId로
     * 권한을 정하지 말라고 적는다. 그 규칙을 8-1절 C6이 검사한다.
     */
    public Property registerProperty(HostId hostId, String name, String regionCode,
                                     String address, String description) {
        Instant now = Instant.now(clock);
        Property property = Property.register(hostId, name, Region.of(regionCode),
                Address.of(address), description, now);
        return propertyRepository.save(property);
    }

    /**
     * CAT-06. 설계 근거: 11 객실 타입 CAT-06, 06-4 1-2 registerRoomType.
     *
     * Property 존재 확인이 여기 있는 근거는 06-4 1-2 Pre 열의 괄호다. Property 존재를 서비스가
     * 확인한다고 적는다. 06-4 1-4도 컨텍스트를 넘는 선행조건을 앱 서비스에 둔다.
     * I14는 여기서 검사하지 않는다. 그것은 RoomType이 지킨다. 8-1절 C2가 이 경로를 검사한다.
     */
    public RoomType registerRoomType(HostId hostId, PropertyId propertyId, String name,
                                     int maxOccupancy, String description) {
        // 11 CAT-06 처리 규칙이 부모 숙소의 소유자를 검사하라고 적는다. 없는 숙소와
        // 남의 숙소를 같은 예외로 묶는 근거는 11 인증과 접근 제어다. 다른 사용자 소유
        // 자원은 404이고 자원 정보를 흘리지 않는다. 8-1절 C6이 이 경로를 본다
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new PropertyNotFoundException(propertyId));
        if (!property.hostId().equals(hostId)) {
            throw new PropertyNotFoundException(propertyId);
        }
        Instant now = Instant.now(clock);
        RoomType roomType = RoomType.register(propertyId, name, maxOccupancy, description, now);
        return roomTypeRepository.save(roomType);
    }

    /**
     * CAT-03. 설계 근거: 11 숙소 CAT-03. 인증이 불필요한 공개 조회다.
     */
    @Transactional(readOnly = true)
    public Property getProperty(PropertyId propertyId) {
        return propertyRepository.findById(propertyId)
                .orElseThrow(() -> new PropertyNotFoundException(propertyId));
    }

    /**
     * CAT-08. 설계 근거: 11 객실 타입 CAT-08. 인증이 불필요한 공개 조회다.
     */
    @Transactional(readOnly = true)
    public RoomType getRoomType(RoomTypeId roomTypeId) {
        return roomTypeRepository.findById(roomTypeId)
                .orElseThrow(() -> new RoomTypeNotFoundException(roomTypeId));
    }
}
