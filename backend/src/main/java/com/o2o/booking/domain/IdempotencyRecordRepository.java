package com.o2o.booking.domain;

import java.util.Optional;

/**
 * 멱등 기록의 리포지토리. 설계 근거: 11 명세 멱등 규칙 1(범위)과 4(진행 중)와 7(해제).
 * 유일성은 DB 유니크가 맡는다(06-4 1-4). 같은 범위를 동시에 두 번 begin하면 save가 실패하고
 * 그 실패를 앱 서비스가 진행 중 판정으로 바꾼다.
 */
public interface IdempotencyRecordRepository {

    IdempotencyRecord save(IdempotencyRecord record);

    Optional<IdempotencyRecord> findByScope(IdempotencyScope scope);

    /** 규칙 7. 변경이 없는 거절 뒤 진행 중 임시 기록을 해제한다 */
    void delete(IdempotencyRecord record);
}
