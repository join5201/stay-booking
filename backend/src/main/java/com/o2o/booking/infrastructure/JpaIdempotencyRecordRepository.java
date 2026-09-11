package com.o2o.booking.infrastructure;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.o2o.booking.domain.IdempotencyRecord;
import com.o2o.booking.domain.IdempotencyRecordRepository;
import com.o2o.booking.domain.IdempotencyScope;

/** domain이 선언한 IdempotencyRecordRepository의 구현. 계약 7절 D-1 가 */
@Repository
public class JpaIdempotencyRecordRepository implements IdempotencyRecordRepository {

    private final IdempotencyRecordJpaRepository jpaRepository;

    public JpaIdempotencyRecordRepository(IdempotencyRecordJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public IdempotencyRecord save(IdempotencyRecord record) {
        return jpaRepository.save(record);
    }

    @Override
    public Optional<IdempotencyRecord> findByScope(IdempotencyScope scope) {
        return jpaRepository.findByScope(scope.actorId(), scope.method(), scope.path(),
                scope.key().value());
    }

    @Override
    public void delete(IdempotencyRecord record) {
        jpaRepository.delete(record);
    }
}
