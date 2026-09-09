package com.o2o.inventory.infrastructure;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.o2o.inventory.domain.DailyRate;

import jakarta.persistence.LockModeType;

/**
 * 설계 근거: DailyInventoryJpaRepository와 같다. 06-4 1-4, 06-4 0절 락 선언.
 */
public interface DailyRateJpaRepository extends JpaRepository<DailyRate, String> {

    @Query("select r from DailyRate r where r.roomTypeId = :roomTypeId and r.stayDate = :stayDate")
    Optional<DailyRate> findOne(@Param("roomTypeId") String roomTypeId,
                                @Param("stayDate") LocalDate stayDate);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from DailyRate r where r.roomTypeId = :roomTypeId and r.stayDate = :stayDate")
    Optional<DailyRate> findOneForUpdate(@Param("roomTypeId") String roomTypeId,
                                         @Param("stayDate") LocalDate stayDate);
}
