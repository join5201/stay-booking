package com.o2o.inventory.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.o2o.shared.RoomTypeId;

/**
 * 설계 근거: 06-2 6절 재고와 요금 CRC의 협력자 리포지토리, 06-2 1절 DailyRate 애그리거트 루트.
 *
 * DailyInventoryRepository와 달리 saveAll과 findExistingDates가 없다. 요금에는 일괄 등록
 * API가 없기 때문이다. 11 요금 절에 bulk 경로가 없다. 대칭을 위해 안 쓰는 메서드를 만들지 않는다.
 */
public interface DailyRateRepository {

    DailyRate save(DailyRate dailyRate);

    Optional<DailyRate> findByRoomTypeIdAndStayDate(RoomTypeId roomTypeId, LocalDate stayDate);

    /** 06-4 1-2 adjustRate. 계약표는 잠금을 적지 않으나 계약 7절 D-2가 수정 경로에 잠금을 요구한다 */
    Optional<DailyRate> findForUpdate(RoomTypeId roomTypeId, LocalDate stayDate);

    /** RATE-03. 재고의 findRange와 같은 규칙이다. from 포함, to 제외, 날짜 오름차순 */
    List<DailyRate> findRange(RoomTypeId roomTypeId, LocalDate from, LocalDate toExclusive);
}
