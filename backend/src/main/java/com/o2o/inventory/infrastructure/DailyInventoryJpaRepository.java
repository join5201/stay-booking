package com.o2o.inventory.infrastructure;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.o2o.inventory.domain.DailyInventory;

import jakarta.persistence.LockModeType;

/**
 * 설계 근거: 06-4 1-4의 유일성과 무결성은 DB. 카탈로그의 같은 자리와 성격이 같다.
 *
 * PESSIMISTIC_WRITE가 여기 있는 근거는 06-4 0절의 락 선언과 계약 7절 D-2다. 0절이
 * 비관적 락으로 확정한다고 적는다. 잠금은 DB가 거는 것이라 인프라 층에 온다. 도메인
 * 리포지토리 인터페이스는 findForUpdate라는 이름으로 의도만 말하고 방법은 모른다.
 */
public interface DailyInventoryJpaRepository extends JpaRepository<DailyInventory, String> {

    @Query("select i from DailyInventory i where i.roomTypeId = :roomTypeId and i.stayDate = :stayDate")
    Optional<DailyInventory> findOne(@Param("roomTypeId") String roomTypeId,
                                     @Param("stayDate") LocalDate stayDate);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from DailyInventory i where i.roomTypeId = :roomTypeId and i.stayDate = :stayDate")
    Optional<DailyInventory> findOneForUpdate(@Param("roomTypeId") String roomTypeId,
                                              @Param("stayDate") LocalDate stayDate);

    @Query("select i.stayDate from DailyInventory i "
            + "where i.roomTypeId = :roomTypeId and i.stayDate in :stayDates order by i.stayDate")
    List<LocalDate> findExistingDates(@Param("roomTypeId") String roomTypeId,
                                      @Param("stayDates") Collection<LocalDate> stayDates);

    // INV-04. to가 크거나 같음이 아니라 작음인 것이 명세의 끝 날짜 제외다
    @Query("select i from DailyInventory i where i.roomTypeId = :roomTypeId "
            + "and i.stayDate >= :from and i.stayDate < :to order by i.stayDate")
    List<DailyInventory> findRange(@Param("roomTypeId") String roomTypeId,
                                   @Param("from") LocalDate from,
                                   @Param("to") LocalDate toExclusive);
}
