package com.o2o.inventory.infrastructure;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.o2o.inventory.domain.DailyInventory;
import com.o2o.inventory.domain.DailyInventoryRepository;
import com.o2o.shared.RoomTypeId;

/**
 * domain이 선언한 DailyInventoryRepository의 구현. 설계 근거: 06-2 6절 CRC 협력자, 06-4 1-4.
 *
 * 카탈로그의 JpaPropertyRepository와 같은 구조다. domain은 Spring Data를 모르고 이 클래스가
 * 그 사이를 잇는다. 평가 축의 레이어 역전이 그것을 요구한다.
 */
@Repository
public class JpaDailyInventoryRepository implements DailyInventoryRepository {

    private final DailyInventoryJpaRepository jpaRepository;

    public JpaDailyInventoryRepository(DailyInventoryJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public DailyInventory save(DailyInventory inventory) {
        return jpaRepository.save(inventory);
    }

    /** INV-02. 날짜 행 N개를 한 트랜잭션으로 만든다. 06-4 0절의 의도적 예외 */
    @Override
    public List<DailyInventory> saveAll(List<DailyInventory> inventories) {
        return jpaRepository.saveAll(inventories);
    }

    @Override
    public Optional<DailyInventory> findByRoomTypeIdAndStayDate(RoomTypeId roomTypeId,
                                                               LocalDate stayDate) {
        return jpaRepository.findOne(roomTypeId.value(), stayDate);
    }

    /** 06-4 1-2 adjust의 Pre 열이 요구하는 잠금. 방법은 PESSIMISTIC_WRITE다 */
    @Override
    public Optional<DailyInventory> findForUpdate(RoomTypeId roomTypeId, LocalDate stayDate) {
        return jpaRepository.findOneForUpdate(roomTypeId.value(), stayDate);
    }

    @Override
    public List<LocalDate> findExistingDates(RoomTypeId roomTypeId,
                                             Collection<LocalDate> stayDates) {
        if (stayDates.isEmpty()) {
            return List.of();
        }
        return jpaRepository.findExistingDates(roomTypeId.value(), stayDates);
    }
}
