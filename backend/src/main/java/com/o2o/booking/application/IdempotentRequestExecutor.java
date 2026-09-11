package com.o2o.booking.application;

import java.util.function.Supplier;

import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.o2o.booking.domain.IdempotencyKeyReusedException;
import com.o2o.booking.domain.IdempotencyRecord;
import com.o2o.booking.domain.IdempotencyScope;
import com.o2o.booking.domain.RequestInProgressException;

/**
 * 멱등 실행기. 설계 근거: 11 명세 멱등 처리 절의 아홉 규칙, 06-4 0절 멱등 반환, 계약 2절 검사
 * 순서 4와 5와 13, 계약 7절 D-1.
 *
 * 순서. 기록을 조회하거나 진행 중으로 남긴다(별도 커밋). 완료 기록이면 body를 대조해 최초
 * 응답을 되돌리거나(규칙 3) 거절한다(규칙 2). 진행 중이면 거절한다(규칙 4). 최초면 본
 * 트랜잭션을 열어 작업을 돌리고 그 안에서 기록을 완료로 바꾼다(규칙 6). 작업이 거절되면 본
 * 트랜잭션이 되돌아가고 진행 중 기록을 따로 지운다(규칙 7).
 *
 * 작업은 공급자로 받는다. api 층이 앱 서비스를 부르고 응답 JSON을 만드는 람다를 넘긴다.
 * 이 클래스는 그 안을 모르므로 2차의 결제 요청과 취소도 같은 실행기를 쓴다.
 *
 * 해제가 실패하면(DB 장애) 진행 중 기록이 남고 로컬 v1은 자동 만료가 없어(규칙 8) 그 키는
 * 다시 쓸 수 없다. 새 키로 새 요청을 보내는 것이 명세의 처리다(규칙 5).
 */
@Component
public class IdempotentRequestExecutor {

    private final IdempotencyRecordService records;
    private final TransactionTemplate transactionTemplate;

    public IdempotentRequestExecutor(IdempotencyRecordService records,
                                     PlatformTransactionManager transactionManager) {
        this.records = records;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public IdempotentResult execute(IdempotencyScope scope, String bodyHash,
                                    Supplier<StoredResponse> work) {
        IdempotencyRecordService.Begun begun;
        try {
            begun = records.begin(scope, bodyHash);
        } catch (DataAccessException raced) {
            // 같은 순간 같은 키가 먼저 커밋됐다. 유니크가 이쪽을 막은 것이니 그쪽 기록으로 판정한다
            IdempotencyRecord winner = records.find(scope).orElseThrow(() -> raced);
            begun = new IdempotencyRecordService.Begun(winner, false);
        }
        if (!begun.created()) {
            IdempotencyRecord existing = begun.record();
            if (!existing.isCompleted()) {
                throw new RequestInProgressException(scope);
            }
            if (!existing.sameBody(bodyHash)) {
                throw new IdempotencyKeyReusedException(scope);
            }
            return new IdempotentResult(new StoredResponse(existing.responseStatus(),
                    existing.responseLocation(), existing.responseBody()), true);
        }
        try {
            StoredResponse response = transactionTemplate.execute((status) -> {
                StoredResponse produced = work.get();
                records.complete(scope, produced);
                return produced;
            });
            return new IdempotentResult(response, false);
        } catch (RuntimeException rejected) {
            records.release(scope);
            throw rejected;
        }
    }
}
