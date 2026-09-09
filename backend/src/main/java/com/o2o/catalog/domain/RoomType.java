package com.o2o.catalog.domain;

import java.time.Instant;

import com.o2o.shared.PropertyId;
import com.o2o.shared.RoomTypeId;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 객실 타입 애그리거트 루트. 설계 근거: 06-2 1절 RoomType 행, 06-2 6절 카탈로그 CRC.
 *
 * CRC의 책임 두 줄에 대응한다. 소속 숙소와 이름과 최대 인원을 알고, 객실 타입을 등록하며
 * I14를 검사한다. 수정은 CAT-07에 걸려 개정 1로 이월했다.
 *
 * 이 애그리거트가 지키는 불변식은 I14 하나다(06-2 3-1, 06-4 1-1). 최대 인원은 0보다 크다.
 * 검사 위치가 여기인 근거는 06-4 1-4다. 규칙 검증은 애그리거트와 값 객체가 한다.
 *
 * Property를 객체로 물지 않고 PropertyId만 갖는 근거는 06-2 1절이다. RoomType의 내부 요소를
 * maxOccupancy와 PropertyId로 적는다. 애그리거트 사이는 식별자로만 잇는다.
 */
@Entity
@Table(name = "room_type")
public class RoomType {

    @Id
    @Column(name = "id", nullable = false, length = 64)
    private String id;

    @Column(name = "property_id", nullable = false, length = 64)
    private String propertyId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "max_occupancy", nullable = false)
    private int maxOccupancy;

    @Column(name = "description", nullable = false, length = 2000)
    private String description;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected RoomType() {
    }

    private RoomType(RoomTypeId id, PropertyId propertyId, String name, int maxOccupancy,
                     String description, Instant now) {
        this.id = id.value();
        this.propertyId = propertyId.value();
        this.name = name;
        this.maxOccupancy = maxOccupancy;
        this.description = description;
        this.createdAt = now;
        this.updatedAt = now;
    }

    /**
     * 설계 근거: 06-4 1-2 registerRoomType. Invariant가 인원 양수(I14)이고 위반 시 예외가
     * InvalidOccupancy다. Property 존재 확인은 여기가 아니라 앱 서비스가 한다. 같은 표의
     * Pre가 그 확인에 서비스라고 괄호로 적어 두었다.
     */
    public static RoomType register(PropertyId propertyId, String name, int maxOccupancy,
                                    String description, Instant now) {
        if (maxOccupancy <= 0) {
            throw new InvalidOccupancyException(maxOccupancy);
        }
        return new RoomType(RoomTypeId.newId(), propertyId, name, maxOccupancy, description, now);
    }

    public RoomTypeId id() {
        return RoomTypeId.of(id);
    }

    public PropertyId propertyId() {
        return PropertyId.of(propertyId);
    }

    public String name() {
        return name;
    }

    public int maxOccupancy() {
        return maxOccupancy;
    }

    public String description() {
        return description;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
