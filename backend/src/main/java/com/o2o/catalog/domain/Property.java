package com.o2o.catalog.domain;

import java.time.Instant;

import com.o2o.shared.HostId;
import com.o2o.shared.PropertyId;
import com.o2o.shared.VersionConflictException;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 숙소 애그리거트 루트. 설계 근거: 06-2 1절 Property 행, 06-2 6절 카탈로그 CRC.
 *
 * CRC의 책임 두 줄에 대응한다. 이름과 주소와 지역을 알고, 숙소를 등록하고 서술 속성을
 * 수정하며 식별자를 바꾸지 않는다. 수정은 개정 2로 이번 바퀴에 들어와 6-2단계에서 붙었다.
 *
 * 지키는 불변식이 없다. 06-2 1절 지키는 불변식 열이 Property에 없음이라고 적는다.
 * 형식 검증도 하지 않는다. 06-4 1-4가 그 책임을 컨트롤러에 둔다.
 *
 * hostId를 두는 근거는 11 CAT-01 처리 규칙과 응답 모델이다. 06-2 1절 내부 요소 열에
 * hostId가 없지만 그 열은 값 객체와 주요 스칼라만 적는 열이라 name도 빠져 있다.
 * 빠짐이지 없음이 아니다.
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

    // 11 응답 모델 Property가 필수로 적는 필드다. 등록 시 0이고 수정마다 1씩 오른다.
    // 대조 방식은 2026-09-09 결정으로 낙관적 잠금이다. update가 그 대조를 한다
    @Column(name = "version", nullable = false)
    private long version;

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
        this.version = 0L;
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

    /**
     * CAT-02. 설계 근거: 06-4 1-2 updateProperty. Post가 서술 속성 변경이다.
     *
     * 버전 대조가 여기 있는 근거는 06-4 1-4다. 규칙 검증은 애그리거트가 한다. 그리고
     * 2026-09-09 사용자 결정으로 동시 수정 처리를 낙관적 잠금으로 확정했다. 저장할 때
     * 요청이 들고 온 숫자와 현재 숫자를 대조한다. 계약 2-2절이 그 결정의 정본이다.
     *
     * null은 유지를 뜻한다. 11 CAT-02 필드표가 생략하면 기존 값 유지라고 적는다.
     * 식별자와 소유자와 생성 시각은 바꾸지 않는다. 같은 절의 처리 규칙이다.
     */
    public void update(long expectedVersion, String name, Region region, Address address,
                       String description, Instant now) {
        if (this.version != expectedVersion) {
            throw new VersionConflictException(expectedVersion, this.version);
        }
        if (name != null) {
            this.name = name;
        }
        if (region != null) {
            this.region = region;
        }
        if (address != null) {
            this.address = address;
        }
        if (description != null) {
            this.description = description;
        }
        this.version = this.version + 1;
        this.updatedAt = now;
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

    public long version() {
        return version;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
