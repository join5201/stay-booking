package com.o2o.promotion.api;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.o2o.promotion.application.PromotionApplicationService;
import com.o2o.promotion.application.PromotionDraft;
import com.o2o.promotion.domain.Promotion;
import com.o2o.promotion.domain.PromotionChanges;
import com.o2o.promotion.domain.StayRange;
import com.o2o.shared.ActorResolver;
import com.o2o.shared.ActorRole;
import com.o2o.shared.ApiDate;
import com.o2o.shared.GuestCount;
import com.o2o.shared.PageQuery;
import com.o2o.shared.PageResponse;
import com.o2o.shared.PromotionId;
import com.o2o.shared.RoomTypeId;

import jakarta.validation.Valid;

/**
 * PROMO-01부터 05. 설계 근거: 11 프로모션 절.
 *
 * 경로와 상태 코드가 그 절 그대로다. 등록은 201에 Location, 나머지는 200이다. PROMO-01부터
 * 04는 인증 OPERATOR이고 PROMO-05는 불필요다(11 인증과 접근 제어 표). 경로 앞부분이 둘이라
 * (promotions와 room-types) 클래스 수준 RequestMapping을 두지 않고 메서드마다 전체 경로를 적는다.
 *
 * 이 층의 책임은 형식 검증과 번역뿐이다. 규칙은 도메인이, 컨텍스트를 넘는 선행조건은
 * 앱 서비스가 지킨다(06-4 1-4).
 */
@RestController
public class PromotionController {

    private final PromotionApplicationService promotionApplicationService;
    private final ActorResolver actorResolver;

    public PromotionController(PromotionApplicationService promotionApplicationService,
                               ActorResolver actorResolver) {
        this.promotionApplicationService = promotionApplicationService;
        this.actorResolver = actorResolver;
    }

    /** PROMO-01 프로모션 등록. 201 */
    @PostMapping("/api/v1/promotions")
    public ResponseEntity<PromotionResponse> create(
            @RequestHeader(value = "X-Dev-Actor-Id", required = false) String actorId,
            @Valid @RequestBody CreatePromotionRequest request) {
        actorResolver.require(actorId, ActorRole.OPERATOR);
        Promotion created = promotionApplicationService.create(new PromotionDraft(
                request.name(), request.discountRate(),
                ApiDate.parse("campaignStartDate", request.campaignStartDate()),
                ApiDate.parse("campaignEndDate", request.campaignEndDate()),
                request.stayStartDate() == null
                        ? null : ApiDate.parse("stayStartDate", request.stayStartDate()),
                request.stayEndDate() == null
                        ? null : ApiDate.parse("stayEndDate", request.stayEndDate()),
                request.minNights(), request.regionCodes(), request.enabledOrTrue()));
        return ResponseEntity
                .created(URI.create("/api/v1/promotions/" + created.id().value()))
                .body(PromotionResponse.from(created));
    }

    /** PROMO-02 프로모션 수정. 200. 버전 불일치는 409 VERSION_CONFLICT */
    @PatchMapping("/api/v1/promotions/{promotionId}")
    public PromotionResponse update(
            @RequestHeader(value = "X-Dev-Actor-Id", required = false) String actorId,
            @PathVariable String promotionId,
            @Valid @RequestBody UpdatePromotionRequest request) {
        actorResolver.require(actorId, ActorRole.OPERATOR);
        PromotionChanges changes = new PromotionChanges(
                request.name(), request.discountRate(),
                request.campaignStartDate() == null
                        ? null : ApiDate.parse("campaignStartDate", request.campaignStartDate()),
                request.campaignEndDate() == null
                        ? null : ApiDate.parse("campaignEndDate", request.campaignEndDate()),
                request.stayWindowChange(), request.minNights(), request.regionCodes(),
                request.enabled());
        return PromotionResponse.from(promotionApplicationService.update(
                PromotionId.of(promotionId), request.version(), changes));
    }

    /** PROMO-03 프로모션 상세 조회. 200이고 없으면 404 */
    @GetMapping("/api/v1/promotions/{promotionId}")
    public PromotionResponse get(
            @RequestHeader(value = "X-Dev-Actor-Id", required = false) String actorId,
            @PathVariable String promotionId) {
        actorResolver.require(actorId, ActorRole.OPERATOR);
        return PromotionResponse.from(promotionApplicationService.get(PromotionId.of(promotionId)));
    }

    /** PROMO-04 프로모션 관리 목록 조회. 200. enabled 생략은 전체 */
    @GetMapping("/api/v1/promotions")
    public PageResponse<PromotionResponse> list(
            @RequestHeader(value = "X-Dev-Actor-Id", required = false) String actorId,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        actorResolver.require(actorId, ActorRole.OPERATOR);
        return PageResponse.from(
                promotionApplicationService.list(enabled, PageQuery.of(page, size)),
                PromotionResponse::from);
    }

    /**
     * PROMO-05 적용 가능 프로모션 조회. 인증 불필요. 200.
     * 날짜 형식은 여기서 400 INVALID_DATE_RANGE, 순서와 30박과 오늘 이상은 도메인과 앱 서비스가 본다.
     * guestCount 범위(1부터 100)는 shared의 GuestCount가 400 INVALID_REQUEST로 낸다.
     */
    @GetMapping("/api/v1/room-types/{roomTypeId}/applicable-promotions")
    public ApplicablePromotionsResponse applicablePromotions(
            @PathVariable String roomTypeId,
            @RequestParam String checkIn,
            @RequestParam String checkOut,
            @RequestParam int guestCount) {
        StayRange stay = StayRange.of(ApiDate.parse("checkIn", checkIn),
                ApiDate.parse("checkOut", checkOut));
        return ApplicablePromotionsResponse.from(promotionApplicationService.applicablePromotions(
                RoomTypeId.of(roomTypeId), stay, GuestCount.of(guestCount)));
    }
}
