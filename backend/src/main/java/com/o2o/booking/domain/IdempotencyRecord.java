package com.o2o.booking.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * 멱등 기록. 설계 근거: 11 명세 멱등 처리 절의 아홉 규칙, 06-4 0절 멱등 반환 선언, 계약 7절 D-1.
 *
 * 범위(행위자, 메서드, 경로, 키)마다 하나다. 유니크가 같은 순간 들어온 같은 키 둘 중 하나를
 * 걸러 낸다(규칙 4). 상태는 둘이다. 진행 중은 본 트랜잭션보다 먼저 커밋되고, 완료는 도메인
 * 변경과 같은 트랜잭션에서 저장된다(규칙 6). 완료 기록은 처음 저장한 상태 코드와 Location과
 * body를 그대로 갖고 재전송에 되돌려 준다(규칙 3). body 해시는 키 순서와 무관한 정규형의
 * 해시다(규칙 2). 완료 기록은 자동 만료되지 않는다(규칙 8).
 *
 * 예약 컨텍스트 안에 두는 근거는 계약 7절 D-1 가다. 2차의 결제 요청과 취소가 같은 기록을 쓴다.
 */
@Entity
@Table(name = "idempotency_record",
        uniqueConstraints = @UniqueConstraint(name = "uk_idempotency_scope",
                columnNames = {"actor_id", "http_method", "request_path", "idempotency_key"}))
public class IdempotencyRecord {

    private static final String PREFIX = "idem_";

    public enum State {
        IN_PROGRESS,
        COMPLETED
    }

    @Id
    @Column(name = "id", nullable = false, length = 64)
    private String id;

    @Column(name = "actor_id", nullable = false, length = 64)
    private String actorId;

    @Column(name = "http_method", nullable = false, length = 16)
    private String method;

    @Column(name = "request_path", nullable = false, length = 200)
    private String path;

    @Column(name = "idempotency_key", nullable = false, length = 128)
    private String idempotencyKey;

    @Column(name = "body_hash", nullable = false, length = 64)
    private String bodyHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 16)
    private State state;

    @Column(name = "response_status")
    private Integer responseStatus;

    @Column(name = "response_location", length = 300)
    private String responseLocation;

    // length가 없으면 MySQL 방언이 기본 길이 255에 맞춰 tinytext를 고른다. 30박 응답이 3KB를
    // 넘어 K13이 500으로 잡았다. 65535는 text 열이고 2차의 결제 시도 셋을 더해도 10KB 안이다
    @Lob
    @Column(name = "response_body", length = 65535)
    private String responseBody;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected IdempotencyRecord() {
    }

    private IdempotencyRecord(IdempotencyScope scope, String bodyHash, Instant now) {
        this.id = PREFIX + UUID.randomUUID().toString().replace("-", "");
        this.actorId = scope.actorId();
        this.method = scope.method();
        this.path = scope.path();
        this.idempotencyKey = scope.key().value();
        this.bodyHash = bodyHash;
        this.state = State.IN_PROGRESS;
        this.createdAt = now;
    }

    /** 규칙 4. 최초 요청이 처리 중임을 남긴다. 본 트랜잭션보다 먼저 커밋된다 */
    public static IdempotencyRecord begin(IdempotencyScope scope, String bodyHash, Instant now) {
        Objects.requireNonNull(scope, "scope는 null일 수 없다");
        Objects.requireNonNull(bodyHash, "bodyHash는 null일 수 없다");
        if (bodyHash.isBlank()) {
            throw new IllegalArgumentException("bodyHash는 비어 있을 수 없다");
        }
        return new IdempotencyRecord(scope, bodyHash, now);
    }

    /**
     * 규칙 3과 6. 처음 저장한 성공 응답을 남기고 완료로 바꾼다. 도메인 변경과 같은 트랜잭션에서
     * 불려야 한다. 이미 완료된 기록을 다시 완료로 바꾸지 않는다.
     */
    public void complete(int status, String location, String body, Instant now) {
        if (this.state == State.COMPLETED) {
            throw new IllegalStateException("이미 완료된 멱등 기록이다: " + id);
        }
        Objects.requireNonNull(body, "body는 null일 수 없다");
        this.state = State.COMPLETED;
        this.responseStatus = status;
        this.responseLocation = location;
        this.responseBody = body;
        this.completedAt = now;
    }

    public boolean isCompleted() {
        return state == State.COMPLETED;
    }

    /** 규칙 2. 같은 범위와 키에서 body가 같은가 */
    public boolean sameBody(String bodyHash) {
        return this.bodyHash.equals(bodyHash);
    }

    public String id() {
        return id;
    }

    public IdempotencyScope scope() {
        return new IdempotencyScope(actorId, method, path, IdempotencyKey.of(idempotencyKey));
    }

    public State state() {
        return state;
    }

    public Integer responseStatus() {
        return responseStatus;
    }

    public String responseLocation() {
        return responseLocation;
    }

    public String responseBody() {
        return responseBody;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant completedAt() {
        return completedAt;
    }
}
