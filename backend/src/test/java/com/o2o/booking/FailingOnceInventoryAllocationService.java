package com.o2o.booking;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.o2o.inventory.domain.DailyInventory;
import com.o2o.inventory.domain.DailyInventoryRepository;
import com.o2o.inventory.domain.InventoryAllocationService;
import com.o2o.shared.RoomTypeId;

/**
 * L11의 테스트 전용 훅. 무장하면 다음 releaseSold 한 번이 예외로 나간다. 취소 트랜잭션에서 환불
 * 뒤 재고 반환이 깨졌을 때 CANCELED도 환불도 남지 않는지를 보기 위해서다(11 BOOK-04 규칙의 부분
 * 결과 없음, 08-3 11-2). 무장하지 않으면 운영 서비스와 똑같이 동작한다. @Primary로 등록해 운영
 * 빈 대신 주입되고 운영 코드는 손대지 않는다.
 */
public class FailingOnceInventoryAllocationService extends InventoryAllocationService {

    private final AtomicBoolean failNextReleaseSold = new AtomicBoolean(false);

    public FailingOnceInventoryAllocationService(DailyInventoryRepository inventoryRepository) {
        super(inventoryRepository);
    }

    public void failNextReleaseSold() {
        failNextReleaseSold.set(true);
    }

    public void disarm() {
        failNextReleaseSold.set(false);
    }

    @Override
    public List<DailyInventory> releaseSold(RoomTypeId roomTypeId, LocalDate checkIn,
                                            LocalDate checkOut, int n, Instant now) {
        if (failNextReleaseSold.getAndSet(false)) {
            throw new TestHookException("테스트 훅. 판매분 반환에서 강제 실패");
        }
        return super.releaseSold(roomTypeId, checkIn, checkOut, n, now);
    }
}
