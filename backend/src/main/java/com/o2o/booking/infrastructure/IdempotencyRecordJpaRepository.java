package com.o2o.booking.infrastructure;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.o2o.booking.domain.IdempotencyRecord;

/** 설계 근거: 11 멱등 규칙 1의 범위 네 칸이 유니크이고 조회 키다 */
public interface IdempotencyRecordJpaRepository extends JpaRepository<IdempotencyRecord, String> {

    @Query("select r from IdempotencyRecord r where r.actorId = :actorId and r.method = :method "
            + "and r.path = :path and r.idempotencyKey = :key")
    Optional<IdempotencyRecord> findByScope(@Param("actorId") String actorId,
                                            @Param("method") String method,
                                            @Param("path") String path,
                                            @Param("key") String key);
}
