package com.o2o.promotion.application;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.o2o.catalog.domain.RoomType;
import com.o2o.catalog.domain.RoomTypeNotFoundException;
import com.o2o.catalog.domain.RoomTypeRepository;
import com.o2o.promotion.domain.Condition;
import com.o2o.promotion.domain.OccupancyExceededException;
import com.o2o.promotion.domain.PricingResult;
import com.o2o.promotion.domain.PricingService;
import com.o2o.promotion.domain.Promotion;
import com.o2o.promotion.domain.PromotionChanges;
import com.o2o.promotion.domain.PromotionCreated;
import com.o2o.promotion.domain.PromotionNotFoundException;
import com.o2o.promotion.domain.PromotionRepository;
import com.o2o.promotion.domain.PromotionUpdated;
import com.o2o.promotion.domain.StayRange;
import com.o2o.shared.GuestCount;
import com.o2o.shared.PageQuery;
import com.o2o.shared.PageResult;
import com.o2o.shared.PromotionId;
import com.o2o.shared.RoomTypeId;
import com.o2o.shared.SeoulDate;

/**
 * 설계 근거: 06-2 6절 프로모션 CRC의 PromotionApplicationService 행. 등록과 수정의 트랜잭션
 * 경계를 열고 커밋 후 이벤트를 발행한다. 06-4 0절 커밋 후 발행 규칙.
 *
 * 종료(close)는 없다. 계약 7절 D-2 다가 enabled 토글만 두고 상태와 close와 I13을 이월했다.
 *
 * PROMO-05의 인원 검사가 여기 있는 이유는 06-4 1-2 예약 계약표가 인원 검증(A5)을 앱 서비스
 * Pre에 두기 때문이다. PricingService는 인원을 모른다(접점 표). RoomTypeRepository 읽기는
 * 06-1 R2(maxOccupancy 읽기)와 같은 성격이다.
 *
 * 시각을 Clock에서 받는 이유는 앞 묶음과 같다. eval-criteria-code.md의 테스트 격리와
 * 재현성 축이 시간 제어를 요구한다.
 */
@Service
@Transactional
public class PromotionApplicationService {

    private final PromotionRepository promotionRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final PricingService pricingService;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public PromotionApplicationService(PromotionRepository promotionRepository,
                                       RoomTypeRepository roomTypeRepository,
                                       PricingService pricingService,
                                       ApplicationEventPublisher eventPublisher,
                                       Clock clock) {
        this.promotionRepository = promotionRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.pricingService = pricingService;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    /** PROMO-01. 설계 근거: 11 PROMO-01, 06-4 1-2 create 행. Post가 PromotionCreated 발행 */
    public Promotion create(PromotionDraft draft) {
        Instant now = Instant.now(clock);
        Condition condition = Condition.of(draft.campaignStartDate(), draft.campaignEndDate(),
                draft.stayStartDate(), draft.stayEndDate(), draft.minNights(), draft.regionCodes());
        Promotion created = promotionRepository.save(
                Promotion.create(draft.name(), draft.discountRate(), condition, draft.enabled(), now));
        eventPublisher.publishEvent(PromotionCreated.of(created));
        return created;
    }

    /**
     * PROMO-02. 설계 근거: 11 PROMO-02, 06-4 1-2 update 행. Pre의 잠금은 findForUpdate이고
     * 버전 대조는 Promotion.update가 한다(V9). 없는 ID는 404다(11 공통 에러 표 RESOURCE_NOT_FOUND).
     */
    public Promotion update(PromotionId promotionId, long expectedVersion, PromotionChanges changes) {
        Instant now = Instant.now(clock);
        Promotion promotion = promotionRepository.findForUpdate(promotionId)
                .orElseThrow(() -> new PromotionNotFoundException(promotionId));
        promotion.update(expectedVersion, changes, now);
        Promotion saved = promotionRepository.save(promotion);
        eventPublisher.publishEvent(PromotionUpdated.of(saved));
        return saved;
    }

    /** PROMO-03. 설계 근거: 11 PROMO-03 */
    @Transactional(readOnly = true)
    public Promotion get(PromotionId promotionId) {
        return promotionRepository.findById(promotionId)
                .orElseThrow(() -> new PromotionNotFoundException(promotionId));
    }

    /** PROMO-04. 설계 근거: 11 PROMO-04. enabled가 null이면 전체 */
    @Transactional(readOnly = true)
    public PageResult<Promotion> list(Boolean enabled, PageQuery pageQuery) {
        return promotionRepository.findAll(enabled, pageQuery);
    }

    /**
     * PROMO-05. 설계 근거: 11 PROMO-05 처리 규칙 네 줄과 에러 표(RATE_NOT_CONFIGURED,
     * OCCUPANCY_EXCEEDED). 재고를 선점하지 않고 보지도 않는다. 셋째 줄이 재고 소진과 할인
     * 조건 충족을 별개로 적는다. 없는 객실 타입은 404다.
     */
    @Transactional(readOnly = true)
    public ApplicablePromotionsResult applicablePromotions(RoomTypeId roomTypeId, StayRange stay,
                                                           GuestCount guestCount) {
        LocalDate today = SeoulDate.today(clock);
        stay.requireNotBefore(today);
        RoomType roomType = roomTypeRepository.findById(roomTypeId)
                .orElseThrow(() -> new RoomTypeNotFoundException(roomTypeId));
        if (guestCount.value() > roomType.maxOccupancy()) {
            throw new OccupancyExceededException(roomType.maxOccupancy(), guestCount.value());
        }
        PricingResult result = pricingService.evaluate(roomTypeId, stay);
        return new ApplicablePromotionsResult(roomTypeId, stay, guestCount, Instant.now(clock),
                result.candidates(), result.selectedPromotionId());
    }
}
