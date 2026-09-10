package com.o2o.inventory.infrastructure;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.o2o.inventory.domain.DailyRate;
import com.o2o.inventory.domain.DailyRateRepository;
import com.o2o.shared.RoomTypeId;

/**
 * domain이 선언한 DailyRateRepository의 구현. 설계 근거: 06-2 6절 CRC 협력자, 06-4 1-4.
 */
@Repository
public class JpaDailyRateRepository implements DailyRateRepository {

    private final DailyRateJpaRepository jpaRepository;

    public JpaDailyRateRepository(DailyRateJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public DailyRate save(DailyRate dailyRate) {
        return jpaRepository.save(dailyRate);
    }

    @Override
    public Optional<DailyRate> findByRoomTypeIdAndStayDate(RoomTypeId roomTypeId,
                                                          LocalDate stayDate) {
        return jpaRepository.findOne(roomTypeId.value(), stayDate);
    }

    @Override
    public Optional<DailyRate> findForUpdate(RoomTypeId roomTypeId, LocalDate stayDate) {
        return jpaRepository.findOneForUpdate(roomTypeId.value(), stayDate);
    }

    @Override
    public List<DailyRate> findRange(RoomTypeId roomTypeId, LocalDate from, LocalDate toExclusive) {
        return jpaRepository.findRange(roomTypeId.value(), from, toExclusive);
    }
}
