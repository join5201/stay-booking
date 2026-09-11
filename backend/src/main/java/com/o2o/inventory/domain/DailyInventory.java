package com.o2o.inventory.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.o2o.shared.RoomTypeId;
import com.o2o.shared.VersionConflictException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * 날짜별 재고 애그리거트 루트. 설계 근거: 06-2 1절 DailyInventory 행, 06-2 6절 재고와 요금 CRC.
 *
 * CRC의 책임 세 줄 전부에 대응한다. 객실 타입과 날짜와 수량 셋을 알고, 모든 수량 변경에서
 * I1과 I1a를 검사하며, 가용성을 계산해 답하되 저장하지 않는다. hold와 commit과 release는
 * 예약 묶음(task-S9-booking 2-1절)이 붙였다. 앞 묶음이 미뤄 둔 자리다(task-S9-inventory-rate
 * 2-2절). 1차에서 호출 경로가 있는 것은 hold뿐이고 commit과 releaseHeld와 releaseSold의
 * 호출자는 확정과 만료와 취소라 2차다.
 *
 * 지키는 불변식은 둘이다(06-2 3-1, 06-4 1-1).
 * I1 재고 총량. totalCount는 soldCount + heldCount 이상이다.
 * I1a 수량 하한. soldCount와 heldCount는 음수가 될 수 없다.
 *
 * 식별자가 대리키인 근거는 06-2 1절 식별자 열이다. 대리키에 유니크(roomTypeId, stayDate)를
 * 건다. 자연키를 기본키로 삼지 않는 이유는 그 열이 그렇게 적어서다. 유니크를 DB에 두는
 * 근거는 06-4 1-4다. 유일성과 무결성은 DB가 맡는다.
 */
@Entity
@Table(name = "daily_inventory",
        uniqueConstraints = @UniqueConstraint(name = "uk_daily_inventory_room_date",
                columnNames = {"room_type_id", "stay_date"}))
public class DailyInventory {

    private static final String PREFIX = "inv_";

    @Id
    @Column(name = "id", nullable = false, length = 64)
    private String id;

    @Column(name = "room_type_id", nullable = false, length = 64)
    private String roomTypeId;

    @Column(name = "stay_date", nullable = false)
    private LocalDate stayDate;

    @Column(name = "total_count", nullable = false)
    private int totalCount;

    @Column(name = "sold_count", nullable = false)
    private int soldCount;

    @Column(name = "held_count", nullable = false)
    private int heldCount;

    // 11 응답 모델 DailyInventory가 version을 필수로 적고 INV-03 요청이 이 값을 대조한다.
    // 잠금과 함께 쓰는 근거는 계약 7절 D-2다. 잠금은 같은 순간의 두 요청을 줄 세우고
    // 이 값은 오래된 화면을 보고 보낸 요청을 거절한다. 막는 것이 서로 다르다
    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected DailyInventory() {
    }

    private DailyInventory(RoomTypeId roomTypeId, LocalDate stayDate, int totalCount, Instant now) {
        this.id = PREFIX + UUID.randomUUID().toString().replace("-", "");
        this.roomTypeId = roomTypeId.value();
        this.stayDate = stayDate;
        this.totalCount = totalCount;
        this.soldCount = 0;
        this.heldCount = 0;
        this.version = 0L;
        this.createdAt = now;
        this.updatedAt = now;
    }

    /**
     * INV-01과 INV-02. 설계 근거: 06-4 1-2 openInventory. Post가 날짜 행 생성과 판매와 선점 0을 적는다.
     *
     * roomType 존재 확인과 중복 없음(U2)은 여기가 아니라 앱 서비스와 DB가 맡는다. 같은 표의
     * Pre가 존재 확인에 서비스라고 괄호로 적고, 06-4 1-4가 유일성을 DB로 보낸다.
     *
     * 계약표 Pre의 totalCount 양수는 [가설]로 적혀 있고 11 명세 139행이 totalCount 0 등록을
     * 허용한다고 적는다. 계약 6절이 명세를 따르기로 확정해서 0을 막지 않는다. 음수만 막는다.
     */
    public static DailyInventory open(RoomTypeId roomTypeId, LocalDate stayDate, int totalCount,
                                      Instant now) {
        validateCounts(totalCount, 0, 0);
        return new DailyInventory(roomTypeId, stayDate, totalCount, now);
    }

    /**
     * INV-03. 설계 근거: 06-4 1-2 adjust. Pre가 잠금과 바꾼 totalCount가 soldCount + heldCount
     * 이상임을 적고, 위반 시 예외를 InventoryBelowOccupied로 적는다.
     *
     * 잠금은 이 메서드가 아니라 앱 서비스가 리포지토리로 건다. 애그리거트는 잠긴 뒤에 불린다.
     * 버전 대조를 잠금 안에서 하는 근거는 계약 7절 D-2다.
     */
    public void adjust(long expectedVersion, int newTotalCount, Instant now) {
        if (this.version != expectedVersion) {
            throw new VersionConflictException(expectedVersion, this.version);
        }
        validateCounts(newTotalCount, this.soldCount, this.heldCount);
        this.totalCount = newTotalCount;
        this.version = this.version + 1;
        this.updatedAt = now;
    }

    /**
     * I1과 I1a를 한자리에서 검사한다. 06-2 6절 CRC가 모든 수량 변경에서 둘 다 검사하라고 적어서
     * 수량을 바꾸는 모든 경로가 이 메서드를 지난다. adjust와 hold와 commit과 releaseHeld와
     * releaseSold 다섯이 전부 여기를 지난다.
     */
    private static void validateCounts(int totalCount, int soldCount, int heldCount) {
        if (soldCount < 0 || heldCount < 0) {
            throw new InventoryCountBelowZeroException(soldCount, heldCount);
        }
        if (totalCount < soldCount + heldCount) {
            throw new InventoryBelowOccupiedException(totalCount, soldCount, heldCount);
        }
    }

    /**
     * HoldInventory. 설계 근거: 06-4 1-2 hold(n). Pre가 오름차순 잠금과 행 존재와 가용 수량
     * n 이상을 적고, Post가 heldCount 증가와 InventoryHeld 발행을 적는다. 위반 예외는
     * InventoryShortage다. 잠금과 존재 확인은 InventoryAllocationService가 리포지토리로 하고
     * 이 메서드는 잠긴 뒤에 불린다. 연박 원자성(A1)의 롤백은 그 서비스와 트랜잭션이 맡는다.
     *
     * n은 08-3 결정 11의 11-5로 1이지만 계약표가 hold(n)이라 인자로 받는다. 검증 항목은
     * 계약 8-1절 K4와 T08과 T09다.
     */
    public void hold(int n, Instant now) {
        requirePositive(n);
        if (availableCount() < n) {
            throw new InventoryShortageException(roomTypeId(), stayDate, availableCount(), n);
        }
        validateCounts(this.totalCount, this.soldCount, this.heldCount + n);
        this.heldCount = this.heldCount + n;
        this.updatedAt = now;
    }

    /**
     * CommitInventory. 설계 근거: 06-4 1-2 commit(n). Pre가 잠금과 heldCount가 n 이상을 적고
     * Post가 선점을 판매로 이동한다고 적는다. 위반 예외는 InsufficientHold다.
     * 1차에는 호출자가 없다. 호출자는 확정(2차)이다. 검증 항목은 계약 8-1절 K5다.
     */
    public void commit(int n, Instant now) {
        requirePositive(n);
        if (this.heldCount < n) {
            throw new InsufficientHoldException(roomTypeId(), stayDate, this.heldCount, n);
        }
        validateCounts(this.totalCount, this.soldCount + n, this.heldCount - n);
        this.heldCount = this.heldCount - n;
        this.soldCount = this.soldCount + n;
        this.updatedAt = now;
    }

    /**
     * 선점 반환. 설계 근거: 06-4 1-2 releaseHeld(n). Pre가 잠금과 원천이 HELD와 heldCount가
     * n 이상을 적는다. 위반 예외는 InsufficientHold다. 호출자는 만료(2차)다. K5.
     */
    public void releaseHeld(int n, Instant now) {
        requirePositive(n);
        if (this.heldCount < n) {
            throw new InsufficientHoldException(roomTypeId(), stayDate, this.heldCount, n);
        }
        validateCounts(this.totalCount, this.soldCount, this.heldCount - n);
        this.heldCount = this.heldCount - n;
        this.updatedAt = now;
    }

    /**
     * 판매분 반환. 설계 근거: 06-4 1-2 releaseSold(n). Pre가 잠금과 원천이 SOLD와 soldCount가
     * n 이상을 적는다. 위반 예외는 InsufficientSold다. 호출자는 취소(2차)다. K5.
     */
    public void releaseSold(int n, Instant now) {
        requirePositive(n);
        if (this.soldCount < n) {
            throw new InsufficientSoldException(roomTypeId(), stayDate, this.soldCount, n);
        }
        validateCounts(this.totalCount, this.soldCount - n, this.heldCount);
        this.soldCount = this.soldCount - n;
        this.updatedAt = now;
    }

    private static void requirePositive(int n) {
        if (n <= 0) {
            throw new IllegalArgumentException("수량은 1 이상이어야 한다: " + n);
        }
    }

    /**
     * 설계 근거: 06-2 6절 CRC의 세 번째 책임 행과 05-3 38행. 계산 결과이고 저장하지 않는다.
     * 그래서 필드가 아니라 메서드다.
     */
    public int availableCount() {
        return totalCount - soldCount - heldCount;
    }

    public String id() {
        return id;
    }

    public RoomTypeId roomTypeId() {
        return RoomTypeId.of(roomTypeId);
    }

    public LocalDate stayDate() {
        return stayDate;
    }

    public int totalCount() {
        return totalCount;
    }

    public int soldCount() {
        return soldCount;
    }

    public int heldCount() {
        return heldCount;
    }

    public long version() {
        return version;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
