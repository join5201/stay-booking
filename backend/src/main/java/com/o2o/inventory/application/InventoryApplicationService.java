package com.o2o.inventory.application;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.o2o.catalog.domain.Property;
import com.o2o.catalog.domain.PropertyRepository;
import com.o2o.catalog.domain.RoomType;
import com.o2o.catalog.domain.RoomTypeNotFoundException;
import com.o2o.catalog.domain.RoomTypeRepository;
import com.o2o.inventory.domain.DailyInventory;
import com.o2o.inventory.domain.DailyInventoryRepository;
import com.o2o.inventory.domain.DailyRate;
import com.o2o.inventory.domain.DailyRateRepository;
import com.o2o.inventory.domain.DuplicateInventoryException;
import com.o2o.inventory.domain.DuplicateRateException;
import com.o2o.inventory.domain.InvalidStayPeriodException;
import com.o2o.inventory.domain.InventoryAdjusted;
import com.o2o.inventory.domain.InventoryNotFoundException;
import com.o2o.inventory.domain.InventoryOpened;
import com.o2o.inventory.domain.PastStayDateException;
import com.o2o.inventory.domain.RateAdjusted;
import com.o2o.inventory.domain.RateNotFoundException;
import com.o2o.inventory.domain.RateRegistered;
import com.o2o.shared.HostId;
import com.o2o.shared.Money;
import com.o2o.shared.RoomTypeId;

/**
 * 설계 근거: 06-2 6절 재고와 요금 CRC의 InventoryApplicationService 책임 세 행.
 * 개설과 조정과 요금 등록과 조정의 트랜잭션 경계를 열고, OpenInventory에서 기간의 날짜 행
 * N개를 한 트랜잭션으로 만들며, 개설과 요금 등록 전에 roomTypeId가 카탈로그에 있는지 확인한다.
 *
 * 카탈로그의 리포지토리 둘을 읽기로 쓴다. RoomTypeRepository는 CRC가 협력자로 이름을 적어
 * 두었고 근거가 06-1 R1이다. PropertyRepository는 CRC에 없다. 11 인증과 접근 제어가 대상
 * 숙소의 소유자만 재고와 요금을 다룰 수 있다고 적는데, 소유자는 Property에만 있고 RoomType은
 * PropertyId만 갖기 때문이다(06-2 1절). CRC에 없는 협력자를 쓰는 것이라 여기 적어 둔다.
 *
 * 남의 자원에 404를 주는 근거는 11 인증과 접근 제어다. 403이 아니다. 403은 자원의 존재를
 * 알려 주기 때문이다. 검증 항목은 T02다.
 *
 * 시각을 Clock에서 받는 이유는 CatalogApplicationService와 같다. eval-criteria-code.md의
 * 테스트 격리와 재현성 축이 시간 제어를 요구한다.
 *
 * 이번 묶음이 만들지 않는 것: hold와 commit과 release. 근거는 계약 2-2절이다.
 * InventoryAllocationService도 같은 이유로 없다. 06-2 6절이 그 도메인 서비스의 책임을
 * 숙박 기간의 재고 N행 잠금과 그 셋으로 적는데 전부 예약 경로다.
 */
@Service
@Transactional
public class InventoryApplicationService {

    private final DailyInventoryRepository inventoryRepository;
    private final DailyRateRepository rateRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final PropertyRepository propertyRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public InventoryApplicationService(DailyInventoryRepository inventoryRepository,
                                       DailyRateRepository rateRepository,
                                       RoomTypeRepository roomTypeRepository,
                                       PropertyRepository propertyRepository,
                                       ApplicationEventPublisher eventPublisher,
                                       Clock clock) {
        this.inventoryRepository = inventoryRepository;
        this.rateRepository = rateRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.propertyRepository = propertyRepository;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    /**
     * INV-01. 설계 근거: 11 재고 INV-01, 06-4 1-2 openInventory.
     */
    public DailyInventory openInventory(HostId hostId, RoomTypeId roomTypeId, LocalDate stayDate,
                                        int totalCount) {
        requireOwnedRoomType(hostId, roomTypeId);
        Instant now = Instant.now(clock);
        requireNotPast(stayDate, today());

        // U2. DB 유니크가 최종 방어이고 이 확인은 409를 또렷하게 주기 위한 것이다.
        // 06-4 1-4가 유일성을 DB로 보내므로 이 검사가 없어도 무결성은 깨지지 않는다
        if (inventoryRepository.findByRoomTypeIdAndStayDate(roomTypeId, stayDate).isPresent()) {
            throw new DuplicateInventoryException(roomTypeId, stayDate);
        }

        DailyInventory opened = inventoryRepository.save(
                DailyInventory.open(roomTypeId, stayDate, totalCount, now));
        eventPublisher.publishEvent(InventoryOpened.of(opened));
        return opened;
    }

    /**
     * INV-02. 설계 근거: 11 재고 INV-02, 06-4 1-2 openInventory, T03.
     *
     * from은 포함하고 to는 제외한다. 11 명세 137행이 to는 제외한다고 적는다.
     *
     * 전부 아니면 전무를 코드로 만들지 않는다. 겹치는 날짜가 하나라도 있으면 예외를 던지고
     * 트랜잭션이 통째로 롤백한다. 부분 저장을 지우는 코드를 쓰면 그 코드 자체가 틀릴 수 있다.
     */
    public List<DailyInventory> openInventories(HostId hostId, RoomTypeId roomTypeId,
                                                LocalDate from, LocalDate to, int totalCount) {
        requireOwnedRoomType(hostId, roomTypeId);
        Instant now = Instant.now(clock);
        LocalDate today = today();
        requirePeriod(from, to, today);

        List<LocalDate> dates = new ArrayList<>();
        for (LocalDate d = from; d.isBefore(to); d = d.plusDays(1)) {
            dates.add(d);
        }

        List<LocalDate> existing = inventoryRepository.findExistingDates(roomTypeId, dates);
        if (!existing.isEmpty()) {
            throw new DuplicateInventoryException(roomTypeId, existing.get(0));
        }

        List<DailyInventory> toOpen = new ArrayList<>(dates.size());
        for (LocalDate d : dates) {
            toOpen.add(DailyInventory.open(roomTypeId, d, totalCount, now));
        }
        List<DailyInventory> opened = inventoryRepository.saveAll(toOpen);
        // 이벤트는 행 단위 사실이라 행마다 낸다. InventoryOpened의 주석에 근거가 있다
        for (DailyInventory inventory : opened) {
            eventPublisher.publishEvent(InventoryOpened.of(inventory));
        }
        return opened;
    }

    /**
     * INV-03. 설계 근거: 11 재고 INV-03, 06-4 1-2 adjust, T04와 T05.
     *
     * 잠그고 읽은 뒤에 버전을 대조하고 그 안에서 I1을 검사한다. 순서가 계약 7절 D-2다.
     * 잠금이 먼저인 이유는 잠금 전에 읽으면 검사와 저장 사이에 남이 끼어들 수 있어서다.
     */
    public DailyInventory adjustInventory(HostId hostId, RoomTypeId roomTypeId, LocalDate stayDate,
                                          long expectedVersion, int totalCount) {
        requireOwnedRoomType(hostId, roomTypeId);
        Instant now = Instant.now(clock);
        requireNotPast(stayDate, today());

        DailyInventory inventory = inventoryRepository.findForUpdate(roomTypeId, stayDate)
                .orElseThrow(() -> new InventoryNotFoundException(roomTypeId, stayDate));
        inventory.adjust(expectedVersion, totalCount, now);
        DailyInventory saved = inventoryRepository.save(inventory);
        eventPublisher.publishEvent(InventoryAdjusted.of(saved));
        return saved;
    }

    /**
     * RATE-01. 설계 근거: 11 요금 RATE-01, 06-4 1-2 registerRate.
     */
    public DailyRate registerRate(HostId hostId, RoomTypeId roomTypeId, LocalDate stayDate,
                                  long amount) {
        requireOwnedRoomType(hostId, roomTypeId);
        Instant now = Instant.now(clock);
        requireNotPast(stayDate, today());

        if (rateRepository.findByRoomTypeIdAndStayDate(roomTypeId, stayDate).isPresent()) {
            throw new DuplicateRateException(roomTypeId, stayDate);
        }

        DailyRate registered = rateRepository.save(
                DailyRate.register(roomTypeId, stayDate, Money.krw(amount), now));
        eventPublisher.publishEvent(RateRegistered.of(registered));
        return registered;
    }

    /**
     * RATE-02. 설계 근거: 11 요금 RATE-02, 06-4 1-2 adjustRate, T05.
     */
    public DailyRate adjustRate(HostId hostId, RoomTypeId roomTypeId, LocalDate stayDate,
                                long expectedVersion, long amount) {
        requireOwnedRoomType(hostId, roomTypeId);
        Instant now = Instant.now(clock);
        requireNotPast(stayDate, today());

        DailyRate dailyRate = rateRepository.findForUpdate(roomTypeId, stayDate)
                .orElseThrow(() -> new RateNotFoundException(roomTypeId, stayDate));
        dailyRate.adjust(expectedVersion, amount, now);
        DailyRate saved = rateRepository.save(dailyRate);
        eventPublisher.publishEvent(RateAdjusted.of(saved));
        return saved;
    }

    /**
     * 06-2 6절 CRC의 세 번째 책임 행. 개설과 요금 등록 전에 roomTypeId가 카탈로그에 있는지
     * 확인한다(06-1 R1). 소유자 확인은 11 인증과 접근 제어가 더한다.
     *
     * 없는 객실 타입과 남의 객실 타입이 같은 예외를 쓴다. 11이 남의 자원을 404로 주라고
     * 적어서 둘을 구분해 알려 주지 않는 것이 목적이다.
     */
    private void requireOwnedRoomType(HostId hostId, RoomTypeId roomTypeId) {
        RoomType roomType = roomTypeRepository.findById(roomTypeId)
                .orElseThrow(() -> new RoomTypeNotFoundException(roomTypeId));
        Property property = propertyRepository.findById(roomType.propertyId())
                .orElseThrow(() -> new RoomTypeNotFoundException(roomTypeId));
        if (!property.hostId().equals(hostId)) {
            throw new RoomTypeNotFoundException(roomTypeId);
        }
    }

    /** 서버의 오늘. 11 명세가 오늘 이상을 요구하고 그 오늘의 기준은 UTC다(11 공통 절) */
    private LocalDate today() {
        return LocalDate.ofInstant(Instant.now(clock), ZoneOffset.UTC);
    }

    private void requireNotPast(LocalDate stayDate, LocalDate today) {
        if (stayDate.isBefore(today)) {
            throw new PastStayDateException(stayDate, today);
        }
    }

    private void requirePeriod(LocalDate from, LocalDate to, LocalDate today) {
        if (!from.isBefore(to)) {
            throw new InvalidStayPeriodException("from은 to보다 앞서야 한다", from, to);
        }
        if (from.isBefore(today)) {
            throw new PastStayDateException(from, today);
        }
        long days = ChronoUnit.DAYS.between(from, to);
        if (days > InvalidStayPeriodException.MAX_DAYS) {
            throw new InvalidStayPeriodException(
                    "기간은 " + InvalidStayPeriodException.MAX_DAYS + "일을 넘을 수 없다", from, to);
        }
    }
}
