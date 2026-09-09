package com.o2o.catalog.domain;

import java.time.Instant;

import com.o2o.shared.HostId;
import com.o2o.shared.PropertyId;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 숙소 애그리거트 루트. 설계 근거: 06-2 1절 Property 행, 06-2 6절 카탈로그 CRC.
 *
 * CRC의 책임 두 줄에 대응한다. 이름과 주소와 지역을 알고, 숙소를 등록하며 식별자를 바꾸지
 * 않는다. 서술 속성 수정은 CAT-02에 걸려 개정 1로 두 번째 바퀴로 이월했으므로 이번 판에
 * update 메서드를 두지 않는다. 테스트 없는 기능 코드를 내지 않는다는 F9 때문이다.
 *
 * 지키는 불변식이 없다. 06-2 1절 지키는 불변식 열이 Property에 없음이라고 적는다.
 * 형식 검증도 하지 않는다. 06-4 1-4가 그 책임을 컨트롤러에 둔다.
 *
 * hostId를 두는 근거는 11 CAT-01 처리 규칙과 응답 모델이다. 06-2 1절 내부 요소 열에
 * hostId가 없지만 그 열은 값 객체와 주요 스칼라만 적는 열이라 name도 빠져 있다.
 * 빠짐이지 없음이 아니다.
 *
 * version 필드를 두지 않는다. 계약 2-2절이 그 판단을 두 번째 바퀴의 선행 결정으로 미뤘고,
 * 미결 정책에 의존하는 구현을 확정하지 않는다는 N4가 그 이유다.
 *
 * 식별자를 값 객체가 아니라 문자열 열로 저장하는 이유는 JPA 매핑 제약이다. 식별자 속성에는
 * 변환기를 쓰지 못하고 임베더블 식별자는 단일 필드에 과하다. 값 객체는 경계에서 쓴다.
 */
@Entity
@Table(name = "property")
public class Property {

    @Id
    @Column(name = "id", nullable = false, length = 64)
    private String id;

    @Column(name = "host_id", nullable = false, length = 64)
    private String hostId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Embedded
    private Region region;

    @Embedded
    private Address address;

    @Column(name = "description", nullable = false, length = 2000)
    private String description;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Property() {
    }

    private Property(PropertyId id, HostId hostId, String name, Region region, Address address,
                     String description, Instant now) {
        this.id = id.value();
        this.hostId = hostId.value();
        this.name = name;
        this.region = region;
        this.address = address;
        this.description = description;
        this.createdAt = now;
        this.updatedAt = now;
    }

    /**
     * 설계 근거: 06-4 1-2 registerProperty. Pre 없음, Invariant 없음, Post는 Property 생성이다.
     * description은 11 CAT-01 필드표가 생략 시 빈 문자열로 적어서 null을 받지 않는다.
     */
    public static Property register(HostId hostId, String name, Region region, Address address,
                                    String description, Instant now) {
        return new Property(PropertyId.newId(), hostId, name, region, address, description, now);
    }

    public PropertyId id() {
        return PropertyId.of(id);
    }

    public HostId hostId() {
        return HostId.of(hostId);
    }

    public String name() {
        return name;
    }

    public Region region() {
        return region;
    }

    public Address address() {
        return address;
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
