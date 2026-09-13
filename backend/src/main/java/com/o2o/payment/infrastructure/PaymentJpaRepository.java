package com.o2o.payment.infrastructure;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.o2o.payment.domain.AttemptKind;
import com.o2o.payment.domain.MockMode;
import com.o2o.payment.domain.Payment;
import com.o2o.payment.domain.PaymentAttemptStatus;

import jakarta.persistence.LockModeType;

/**
 * 설계 근거: 06-4 1-4의 유일성과 무결성은 DB. 재고의 같은 자리와 성격이 같다.
 *
 * PESSIMISTIC_WRITE가 여기 있는 근거는 06-4 0절의 락 선언과 06-2 5절 Payment 행이다. 시도 추가와
 * 콜백 기록은 루트를 잠근 뒤 한다. 잠금은 DB가 거는 것이라 인프라 층에 온다. 도메인 리포지토리
 * 인터페이스는 ForUpdate라는 이름으로 의도만 말하고 방법은 모른다.
 */
public interface PaymentJpaRepository extends JpaRepository<Payment, String> {

    @Query("select p from Payment p where p.bookingId = :bookingId")
    Optional<Payment> findOneByBookingId(@Param("bookingId") String bookingId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.bookingId = :bookingId")
    Optional<Payment> findOneByBookingIdForUpdate(@Param("bookingId") String bookingId);

    // 시도 ID로 루트를 찾아 잠근다. 시도가 루트를 통해서만 접근되므로(06-2 6절 CRC) 시도 표를
    // 직접 잠그지 않고 루트 행을 잠근다. MySQL은 조인한 시도 행도 함께 잠그지만 직렬화의 기준은
    // 루트 한 행이다(08-3 결정 3)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p join p.attempts a where a.id = :attemptId")
    Optional<Payment> findOneByAttemptIdForUpdate(@Param("attemptId") String attemptId);

    // T26. 재시작 재개의 후보. 잠그지 않는다. 요청 순서대로 재개한다
    @Query("select a.id from Payment p join p.attempts a "
            + "where a.kind = :kind and a.status = :status and a.mockMode in :modes "
            + "order by a.requestedAt, a.id")
    List<String> findAttemptIds(@Param("kind") AttemptKind kind,
                                @Param("status") PaymentAttemptStatus status,
                                @Param("modes") Collection<MockMode> modes);
}
