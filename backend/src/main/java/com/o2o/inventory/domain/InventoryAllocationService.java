package com.o2o.inventory.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.o2o.shared.RoomTypeId;

/**
 * 연박 재고 배분의 도메인 서비스. 설계 근거: 06-2 6절 재고와 요금 CRC의 InventoryAllocationService
 * 행. 숙박 기간의 재고 N행을 날짜 오름차순으로 잠그고 hold를 전 행에 적용하며, 한 행이라도
 * 거절되면 전체를 롤백한다(A1). 06-4 1-4가 원자성 조율을 도메인 서비스에 둔다.
 *
 * 잠금 순서의 근거는 08-3 결정 3(Booking, Payment, 재고 N행 날짜 오름차순)이다. 순서는
 * 리포지토리 질의가 정렬로 보장한다(task-S9-booking 7절 D-2 가). 행 수가 박수보다 적으면
 * 미개설이다. 없는 날짜의 판정을 잠금 조회 하나로 같이 하는 이유는 잠근 채 두 번 읽지 않기
 * 위해서다.
 *
 * 잠금과 적용이 두 메서드로 나뉜 이유는 계약 2절 검사 순서다. 잠근 뒤 요금과 가격을 대조하고
 * 그다음에 선점한다. PRICE_CHANGED면 예약도 Hold도 만들지 않는다(11 BOOK-01 처리 규칙).
 *
 * 롤백은 이 클래스가 하지 않는다. 예외를 던지면 호출자의 트랜잭션이 통째로 되돌린다.
 * 예약 앱 서비스가 그 트랜잭션을 연다(06-2 4절 생성 경계). 스프링을 모르는 순수 클래스라
 * 빈 등록은 예약 컨텍스트의 설정 클래스가 한다. 06-1 R4의 하류 쪽이 조립하는 것이다.
 *
 * commit과 release의 N행 적용은 2차가 같은 자리에 더한다. 1차 호출자는 예약 생성뿐이다.
 */
public class InventoryAllocationService {

    private final DailyInventoryRepository inventoryRepository;

    public InventoryAllocationService(DailyInventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    /**
     * checkIn 이상 checkOut 미만의 재고 행을 날짜 오름차순으로 잠그고 돌려준다. 빠진 날짜가
     * 있으면 InventoryNotOpened다. 잠금은 호출자의 트랜잭션이 끝날 때 풀린다.
     */
    public List<DailyInventory> lock(RoomTypeId roomTypeId, LocalDate checkIn, LocalDate checkOut) {
        List<DailyInventory> locked = inventoryRepository.findRangeForUpdate(
                roomTypeId, checkIn, checkOut);
        List<LocalDate> missing = missingDates(checkIn, checkOut, locked);
        if (!missing.isEmpty()) {
            throw new InventoryNotOpenedException(roomTypeId, missing);
        }
        return locked;
    }

    /**
     * HoldInventory. 잠긴 행 전부에 hold(n)을 적용한다. 한 행이라도 가용이 부족하면
     * InventoryShortage가 호출자의 트랜잭션을 깨고 나가므로 앞 날짜의 증가도 남지 않는다.
     */
    public List<DailyInventory> hold(List<DailyInventory> locked, int n, Instant now) {
        for (DailyInventory inventory : locked) {
            inventory.hold(n, now);
        }
        return inventoryRepository.saveAll(locked);
    }

    /** 잠금과 적용을 한 번에. 대조할 것이 없는 호출자용이다 */
    public List<DailyInventory> hold(RoomTypeId roomTypeId, LocalDate checkIn, LocalDate checkOut,
                                     int n, Instant now) {
        return hold(lock(roomTypeId, checkIn, checkOut), n, now);
    }

    private static List<LocalDate> missingDates(LocalDate from, LocalDate toExclusive,
                                                List<DailyInventory> found) {
        List<LocalDate> present = found.stream().map(DailyInventory::stayDate).toList();
        List<LocalDate> missing = new ArrayList<>();
        for (LocalDate d = from; d.isBefore(toExclusive); d = d.plusDays(1)) {
            if (!present.contains(d)) {
                missing.add(d);
            }
        }
        return missing;
    }
}
