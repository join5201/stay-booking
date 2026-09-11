package com.o2o.booking.application;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.o2o.booking.domain.IdempotencyRecord;
import com.o2o.booking.domain.IdempotencyRecordRepository;
import com.o2o.booking.domain.IdempotencyScope;

/**
 * 멱등 기록의 트랜잭션 셋. 설계 근거: 11 멱등 규칙 4, 6, 7과 계약 7절 D-1.
 *
 * 진행 중 기록은 본 트랜잭션보다 먼저 따로 커밋해야 같은 순간 들어온 같은 키가 그것을 본다
 * (REQUIRES_NEW). 완료 전환은 도메인 변경과 같은 트랜잭션이어야 한다(MANDATORY. 규칙 6).
 * 해제는 본 트랜잭션이 이미 되돌아간 뒤라 다시 따로 연다(REQUIRES_NEW. 규칙 7).
 * 셋을 한 클래스에 두되 서로 부르지 않는다. 자기 호출은 프록시를 지나지 않아 전파가 안 걸린다.
 */
@Service
public class IdempotencyRecordService {

    /** begin의 결과. created가 참이면 이번 요청이 최초다 */
    public record Begun(IdempotencyRecord record, boolean created) {
    }

    private final IdempotencyRecordRepository repository;
    private final Clock clock;

    public IdempotencyRecordService(IdempotencyRecordRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /**
     * 기록이 있으면 그것을, 없으면 진행 중 기록을 새로 남겨 돌려준다. 같은 순간 둘이 여기를
     * 지나면 유니크가 한쪽의 커밋을 막고 그 예외는 호출자가 받아 다시 조회한다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Begun begin(IdempotencyScope scope, String bodyHash) {
        Optional<IdempotencyRecord> existing = repository.findByScope(scope);
        if (existing.isPresent()) {
            return new Begun(existing.get(), false);
        }
        IdempotencyRecord record = repository.save(
                IdempotencyRecord.begin(scope, bodyHash, Instant.now(clock)));
        return new Begun(record, true);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public Optional<IdempotencyRecord> find(IdempotencyScope scope) {
        return repository.findByScope(scope);
    }

    /** 규칙 6. 도메인 변경과 같은 트랜잭션 안에서만 부른다 */
    @Transactional(propagation = Propagation.MANDATORY)
    public void complete(IdempotencyScope scope, StoredResponse response) {
        IdempotencyRecord record = repository.findByScope(scope)
                .orElseThrow(() -> new IllegalStateException("진행 중 기록이 없다: " + scope));
        record.complete(response.status(), response.location(), response.body(),
                Instant.now(clock));
        repository.save(record);
    }

    /** 규칙 7. 변경이 없는 거절 뒤 진행 중 기록을 지운다. 완료 기록은 건드리지 않는다 */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void release(IdempotencyScope scope) {
        repository.findByScope(scope)
                .filter((record) -> !record.isCompleted())
                .ifPresent(repository::delete);
    }
}
