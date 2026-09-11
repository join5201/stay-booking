package com.o2o.promotion.domain;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.o2o.catalog.domain.Property;
import com.o2o.catalog.domain.PropertyNotFoundException;
import com.o2o.catalog.domain.PropertyRepository;
import com.o2o.catalog.domain.RoomType;
import com.o2o.catalog.domain.RoomTypeNotFoundException;
import com.o2o.catalog.domain.RoomTypeRepository;
import com.o2o.inventory.domain.DailyRate;
import com.o2o.inventory.domain.DailyRateRepository;
import com.o2o.shared.RoomTypeId;
import com.o2o.shared.SeoulDate;

/**
 * 가격 계산 도메인 서비스. 설계 근거: 06-2 6절 PricingService 행, 06-4 118행 PricingService
 * 계약 행, 계약 7절 D-3 가(이번 묶음이 만들고 예약 묶음이 재사용한다).
 *
 * CRC 그대로다. 숙박 기간의 날짜별 단가와 적용 가능한 프로모션을 읽어 DailyPrice 행 N개와
 * 할인 배분을 계산해 돌려준다. 협력자는 DailyRateRepository 읽기와 PromotionRepository
 * 읽기이고, 지역 판정을 위해 카탈로그를 읽는 것은 06-1 R3(Region 값 읽기, Conformist)이다.
 *
 * 06-4 118행의 Pre가 숙박 기간 전 날짜에 요금 존재(A6)이고 위반 시 예외가 RateNotFound다.
 * 이 묶음에서는 그 예외가 RateNotConfigured이고 빠진 날짜 목록을 싣는다(11 PROMO-05와
 * SEARCH-03의 409 RATE_NOT_CONFIGURED, 접점 표). 인원 검사는 여기 없다. 06-4 1-2 예약
 * 계약표가 인원 검증(A5)을 앱 서비스 Pre에 두고, 접점 표도 호출자 몫으로 적는다.
 *
 * 프로모션 선정 시점은 06-4 118행이 예약 생성 시점이라 적는다. 여기서는 Clock의 지금이고
 * 날짜는 서울 기준이다(11 공통 35행, SeoulDate). 스프링 어노테이션이 없는 이유는 도메인
 * 층 규칙이고, 빈 등록은 infrastructure의 PricingServiceConfiguration이 한다.
 */
public final class PricingService {

    private final RoomTypeRepository roomTypeRepository;
    private final PropertyRepository propertyRepository;
    private final DailyRateRepository dailyRateRepository;
    private final PromotionRepository promotionRepository;
    private final Clock clock;

    public PricingService(RoomTypeRepository roomTypeRepository,
                          PropertyRepository propertyRepository,
                          DailyRateRepository dailyRateRepository,
                          PromotionRepository promotionRepository,
                          Clock clock) {
        this.roomTypeRepository = Objects.requireNonNull(roomTypeRepository);
        this.propertyRepository = Objects.requireNonNull(propertyRepository);
        this.dailyRateRepository = Objects.requireNonNull(dailyRateRepository);
        this.promotionRepository = Objects.requireNonNull(promotionRepository);
        this.clock = Objects.requireNonNull(clock);
    }

    /**
     * 다른 세션과의 접점 표에 적은 시그니처 그대로다. 예약 묶음이 부른다. 숙박 구간 검사는
     * StayRange가 하고(checkOut이 checkIn보다 뒤, 최대 30박), 오늘 이후인지는 호출자가 본다.
     */
    public PriceSnapshot quote(RoomTypeId roomTypeId, LocalDate checkIn, LocalDate checkOut) {
        return evaluate(roomTypeId, StayRange.of(checkIn, checkOut)).snapshot();
    }

    /** PROMO-05와 SEARCH-03. 스냅샷과 후보 전부를 한 번의 계산으로 돌려준다 */
    public PricingResult evaluate(RoomTypeId roomTypeId, StayRange stay) {
        RoomType roomType = roomTypeRepository.findById(roomTypeId)
                .orElseThrow(() -> new RoomTypeNotFoundException(roomTypeId));
        Property property = propertyRepository.findById(roomType.propertyId())
                .orElseThrow(() -> new PropertyNotFoundException(roomType.propertyId()));
        return evaluate(roomTypeId, property.region().code(), stay);
    }

    /** 검색이 객실마다 카탈로그를 다시 읽지 않도록 지역 코드를 받는 경로다(06-1 R8 읽기 모델) */
    public PricingResult evaluate(RoomTypeId roomTypeId, String regionCode, StayRange stay) {
        List<LocalDate> dates = stay.dates();
        List<DailyRate> rates = dailyRateRepository.findRange(roomTypeId, stay.checkIn(),
                stay.checkOut());
        List<Long> baseAmounts = baseAmounts(roomTypeId, dates, rates);

        LocalDate today = SeoulDate.today(clock);
        List<Promotion> applicable = new ArrayList<>();
        for (Promotion promotion : promotionRepository.findEnabledOn(today)) {
            if (promotion.isApplicable(regionCode, stay, today)) {
                applicable.add(promotion);
            }
        }
        return PriceCalculation.calculate(dates, baseAmounts, applicable);
    }

    /** A6. 빠진 날짜가 하나라도 있으면 목록째 실어 던진다. 11 SEARCH-03 처리 규칙 */
    private static List<Long> baseAmounts(RoomTypeId roomTypeId, List<LocalDate> dates,
                                          List<DailyRate> ratesAscending) {
        List<Long> amounts = new ArrayList<>(dates.size());
        List<LocalDate> missing = new ArrayList<>();
        int cursor = 0;
        for (LocalDate date : dates) {
            if (cursor < ratesAscending.size() && ratesAscending.get(cursor).stayDate().equals(date)) {
                amounts.add(ratesAscending.get(cursor).rate().amount());
                cursor++;
            } else {
                missing.add(date);
            }
        }
        if (!missing.isEmpty()) {
            throw new RateNotConfiguredException(roomTypeId, missing);
        }
        return amounts;
    }
}
