package com.o2o.search.application;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import com.o2o.promotion.domain.OccupancyExceededException;
import com.o2o.promotion.domain.PriceSnapshot;
import com.o2o.promotion.domain.PricingService;
import com.o2o.promotion.domain.StayRange;
import com.o2o.shared.GuestCount;
import com.o2o.shared.Money;
import com.o2o.shared.PageQuery;
import com.o2o.shared.PageResult;
import com.o2o.shared.RoomTypeId;
import com.o2o.shared.SeoulDate;

/**
 * SEARCH-01부터 03. 설계 근거: 11 검색 절 세 API의 처리 규칙, 06-1 R8(읽기 전용 읽기 모델),
 * 계약 task-S9-promotion-search 7절 D-1 나(기존 테이블 직접 조회).
 *
 * 세 컨텍스트의 리포지토리를 읽기로만 쓴다. 저장과 이벤트 발행이 없고 Hold를 만들지 않는다
 * (11 SEARCH-01 처리 규칙 셋째 줄, SEARCH-03 처리 규칙 둘째 줄). 가격은 PricingService에
 * 맡겨 PROMO-05와 예약이 같은 규칙을 쓰게 한다(11 내부 처리 여섯째 줄).
 *
 * 쪽 나눔을 메모리에서 하는 이유는 D-1 나다. 포함 조건이 재고와 요금과 인원을 다 본 뒤에야
 * 정해지므로 DB가 쪽을 자를 수 없다. 프로젝션이 생기면 그쪽 테이블이 자른다.
 *
 * 인원 검사가 여기 있는 이유는 PromotionApplicationService와 같다. PricingService는 인원을
 * 모른다. 검색 컨텍스트는 도메인이 없어 예외를 promotion 도메인의 것으로 쓴다. 같은 상황에 같은
 * 코드(409 OCCUPANCY_EXCEEDED, 409 RATE_NOT_CONFIGURED)가 나가야 하기 때문이다.
 */
@Service
@Transactional(readOnly = true)
public class SearchApplicationService {

    static final String OCCUPANCY_EXCEEDED = "OCCUPANCY_EXCEEDED";
    static final String INVENTORY_NOT_CONFIGURED = "INVENTORY_NOT_CONFIGURED";
    static final String INVENTORY_UNAVAILABLE = "INVENTORY_UNAVAILABLE";
    static final String RATE_NOT_CONFIGURED = "RATE_NOT_CONFIGURED";

    private final PropertyRepository propertyRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final DailyInventoryRepository inventoryRepository;
    private final DailyRateRepository rateRepository;
    private final PricingService pricingService;
    private final Clock clock;

    public SearchApplicationService(PropertyRepository propertyRepository,
                                    RoomTypeRepository roomTypeRepository,
                                    DailyInventoryRepository inventoryRepository,
                                    DailyRateRepository rateRepository,
                                    PricingService pricingService,
                                    Clock clock) {
        this.propertyRepository = propertyRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.inventoryRepository = inventoryRepository;
        this.rateRepository = rateRepository;
        this.pricingService = pricingService;
        this.clock = clock;
    }

    /**
     * SEARCH-01. 설계 근거: 11 SEARCH-01 처리 규칙. 인원 수용, 전 날짜 재고 존재, 전 날짜 가용
     * 수 1 이상, 전 날짜 요금 존재를 모두 만족하는 객실만 포함하고 그런 객실이 없는 숙소는
     * 뺀다. 결과가 없으면 빈 items다. 계약 8-1절 V15.
     */
    public PageResult<PropertySearchResult> searchProperties(String regionCode, StayRange stay,
                                                             GuestCount guestCount,
                                                             PageQuery pageQuery) {
        stay.requireNotBefore(SeoulDate.today(clock));
        List<PropertySearchResult> matched = new ArrayList<>();
        for (Property property : propertyRepository.findAllByRegionCode(regionCode)) {
            List<RoomSearchResult> rooms = new ArrayList<>();
            for (RoomType roomType : roomTypeRepository.findAllByPropertyId(property.id())) {
                RoomSearchResult room = qualify(roomType, regionCode, stay, guestCount);
                if (room != null) {
                    rooms.add(room);
                }
            }
            if (!rooms.isEmpty()) {
                long lowest = rooms.stream().mapToLong(RoomSearchResult::totalAmount).min().orElseThrow();
                matched.add(new PropertySearchResult(property, lowest, Money.KRW, rooms));
            }
        }
        return page(matched, pageQuery);
    }

    /** 네 조건 중 하나라도 어긋나면 null이다. 조건의 순서는 11 SEARCH-01 처리 규칙 첫 줄 그대로다 */
    private RoomSearchResult qualify(RoomType roomType, String regionCode, StayRange stay,
                                     GuestCount guestCount) {
        if (guestCount.value() > roomType.maxOccupancy()) {
            return null;
        }
        List<DailyInventory> inventories = inventoryRepository.findRange(
                roomType.id(), stay.checkIn(), stay.checkOut());
        if (inventories.size() != stay.nights()) {
            return null;
        }
        int minAvailable = inventories.stream().mapToInt(DailyInventory::availableCount).min().orElseThrow();
        if (minAvailable < 1) {
            return null;
        }
        List<DailyRate> rates = rateRepository.findRange(roomType.id(), stay.checkIn(), stay.checkOut());
        if (rates.size() != stay.nights()) {
            return null;
        }
        PriceSnapshot price = pricingService.evaluate(roomType.id(), regionCode, stay).snapshot();
        return new RoomSearchResult(roomType.id(), roomType.name(), roomType.maxOccupancy(),
                minAvailable, price.totalAmount());
    }

    /** 11 공통 목록 절의 Page 구조. 메모리 쪽 나눔이라 totalElements가 조건을 통과한 숙소 수다 */
    private static PageResult<PropertySearchResult> page(List<PropertySearchResult> all,
                                                         PageQuery pageQuery) {
        int from = Math.min(pageQuery.page() * pageQuery.size(), all.size());
        int to = Math.min(from + pageQuery.size(), all.size());
        int totalPages = (int) Math.ceil(all.size() / (double) pageQuery.size());
        return new PageResult<>(List.copyOf(all.subList(from, to)), pageQuery.page(),
                pageQuery.size(), all.size(), totalPages);
    }

    /**
     * SEARCH-02. 설계 근거: 11 SEARCH-02 처리 규칙 세 줄. 가용성 부족은 200과 available=false다.
     * days는 전 숙박 날짜이고 재고 레코드가 없는 날짜는 availableCount가 null이며
     * missingInventoryDates에도 든다. 요금 누락과 인원 초과도 reasons에 넣는다. 최상위
     * availableCount는 재고 기준 최소 수이고 누락이 있으면 0이다. 계약 8-1절 V13과 V14.
     */
    public AvailabilityResult availability(RoomTypeId roomTypeId, StayRange stay,
                                           GuestCount guestCount) {
        stay.requireNotBefore(SeoulDate.today(clock));
        RoomType roomType = roomTypeRepository.findById(roomTypeId)
                .orElseThrow(() -> new RoomTypeNotFoundException(roomTypeId));

        Map<LocalDate, Integer> availableByDate = new HashMap<>();
        for (DailyInventory inventory : inventoryRepository.findRange(roomTypeId, stay.checkIn(),
                stay.checkOut())) {
            availableByDate.put(inventory.stayDate(), inventory.availableCount());
        }
        List<AvailabilityResult.Day> days = new ArrayList<>();
        List<LocalDate> missingInventoryDates = new ArrayList<>();
        boolean unavailableDay = false;
        int minAvailable = Integer.MAX_VALUE;
        for (LocalDate date : stay.dates()) {
            Integer available = availableByDate.get(date);
            days.add(new AvailabilityResult.Day(date, available));
            if (available == null) {
                missingInventoryDates.add(date);
            } else {
                minAvailable = Math.min(minAvailable, available);
                if (available < 1) {
                    unavailableDay = true;
                }
            }
        }

        List<LocalDate> ratedDates = rateRepository.findRange(roomTypeId, stay.checkIn(),
                stay.checkOut()).stream().map(DailyRate::stayDate).toList();
        List<LocalDate> missingRateDates = stay.dates().stream()
                .filter(date -> !ratedDates.contains(date)).toList();

        // 순서는 11 Availability 표의 reasons 행에 적힌 순서다
        List<String> reasons = new ArrayList<>();
        if (guestCount.value() > roomType.maxOccupancy()) {
            reasons.add(OCCUPANCY_EXCEEDED);
        }
        if (!missingInventoryDates.isEmpty()) {
            reasons.add(INVENTORY_NOT_CONFIGURED);
        }
        if (unavailableDay) {
            reasons.add(INVENTORY_UNAVAILABLE);
        }
        if (!missingRateDates.isEmpty()) {
            reasons.add(RATE_NOT_CONFIGURED);
        }
        int availableCount = missingInventoryDates.isEmpty() ? minAvailable : 0;
        return new AvailabilityResult(roomTypeId, stay, guestCount, reasons.isEmpty(),
                availableCount, days, missingInventoryDates, missingRateDates, reasons);
    }

    /**
     * SEARCH-03. 설계 근거: 11 SEARCH-03 처리 규칙과 오류 표. 요금과 인원 조건을 확인하고
     * 날짜별 단가와 할인액과 총액을 돌려준다. 재고를 보장하거나 선점하지 않는다. 인원 초과는
     * 409 OCCUPANCY_EXCEEDED, 요금 누락은 PricingService의 RateNotConfigured가 409다. 계약 8-1절 V16.
     */
    public PriceQuoteResult priceQuote(RoomTypeId roomTypeId, StayRange stay, GuestCount guestCount) {
        stay.requireNotBefore(SeoulDate.today(clock));
        RoomType roomType = roomTypeRepository.findById(roomTypeId)
                .orElseThrow(() -> new RoomTypeNotFoundException(roomTypeId));
        if (guestCount.value() > roomType.maxOccupancy()) {
            throw new OccupancyExceededException(roomType.maxOccupancy(), guestCount.value());
        }
        PriceSnapshot price = pricingService.quote(roomTypeId, stay.checkIn(), stay.checkOut());
        return new PriceQuoteResult(roomTypeId, stay, guestCount, Instant.now(clock), price);
    }
}
