package com.o2o.inventory.domain;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.o2o.shared.RoomTypeId;

/**
 * 설계 근거: 06-2 6절 재고와 요금 CRC의 협력자 리포지토리, 06-2 1절이 DailyInventory를
 * 애그리거트 루트로 적는다. 루트가 아닌 것에는 리포지토리를 두지 않는다.
 *
 * findForUpdate가 따로 있는 근거는 계약 7절 D-2와 06-4 1-2 adjust의 Pre 열이다. 그 열이
 * 잠금을 선행조건으로 적는다. 조회용 findBy와 이름을 가르는 이유는 잠금이 붙는 조회를
 * 부르는 쪽이 알고 골라야 하기 때문이다. 이름이 같으면 조회가 몰래 잠근다.
 */
public interface DailyInventoryRepository {

    DailyInventory save(DailyInventory inventory);

    /**
     * INV-02. 기간 재고 일괄 등록이 쓴다. 06-4 0절이 재고 개설의 여러 행 생성을
     * 한 트랜잭션 한 애그리거트 규칙의 의도적 예외 2건 중 하나로 적는다.
     */
    List<DailyInventory> saveAll(List<DailyInventory> inventories);

    Optional<DailyInventory> findByRoomTypeIdAndStayDate(RoomTypeId roomTypeId, LocalDate stayDate);

    /** 06-4 1-2 adjust의 Pre 열이 요구하는 잠금. 행을 잠그고 읽는다 */
    Optional<DailyInventory> findForUpdate(RoomTypeId roomTypeId, LocalDate stayDate);

    /**
     * INV-02의 중복 없음(U2) 선행조건. 겹치는 날짜를 돌려준다. 개수가 아니라 날짜를 돌려주는
     * 이유는 11 INV-02가 전부 실패시키되 어느 날짜가 겹쳤는지 알려야 하기 때문이다.
     */
    List<LocalDate> findExistingDates(RoomTypeId roomTypeId, Collection<LocalDate> stayDates);

    /**
     * INV-04. from을 포함하고 to를 제외하며 날짜 오름차순이다. 11 응답 모델 InventoryRange의
     * items 열이 date 오름차순이고 존재하는 날짜만 포함한다고 적는다. 정렬을 부르는 쪽이
     * 아니라 여기서 하는 이유는 그것이 질의의 일이기 때문이다. 읽어 와서 정렬하면 행이
     * 많아질 때 값을 두 번 옮긴다.
     */
    List<DailyInventory> findRange(RoomTypeId roomTypeId, LocalDate from, LocalDate toExclusive);
}
