package com.o2o.search.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.o2o.promotion.domain.StayRange;
import com.o2o.search.application.SearchApplicationService;
import com.o2o.shared.ApiDate;
import com.o2o.shared.GuestCount;
import com.o2o.shared.PageQuery;
import com.o2o.shared.PageResponse;
import com.o2o.shared.RoomTypeId;

/**
 * SEARCH-01부터 03. 설계 근거: 11 검색 절. 셋 다 인증 불필요라 헤더를 읽지 않는다.
 *
 * 날짜 형식은 여기서 400 INVALID_DATE_RANGE(ApiDate), 순서와 30박과 오늘 이상은 StayRange와
 * 앱 서비스가 같은 코드로 낸다. guestCount와 page와 size의 범위는 shared의 값 객체가 400
 * INVALID_REQUEST로 낸다. regionCode 길이 1부터 32는 11 SEARCH-01 쿼리표라 여기서 본다.
 * 쿼리 인자의 검증을 애너테이션이 아니라 값 객체와 검사문으로 하는 이유는 공유 핸들러가
 * 고정되어 있어 메서드 검증 예외를 ErrorResponse로 바꿀 자리가 없기 때문이다.
 */
@RestController
public class SearchController {

    private static final int REGION_CODE_MAX = 32;

    private final SearchApplicationService searchApplicationService;

    public SearchController(SearchApplicationService searchApplicationService) {
        this.searchApplicationService = searchApplicationService;
    }

    /** SEARCH-01 숙소 검색. 200. 결과가 없으면 빈 items */
    @GetMapping("/api/v1/search/properties")
    public PageResponse<PropertySearchResultResponse> searchProperties(
            @RequestParam String regionCode,
            @RequestParam String checkIn,
            @RequestParam String checkOut,
            @RequestParam int guestCount,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return PageResponse.from(searchApplicationService.searchProperties(regionCode(regionCode),
                stay(checkIn, checkOut), GuestCount.of(guestCount), PageQuery.of(page, size)),
                PropertySearchResultResponse::from);
    }

    /** SEARCH-02 객실별 연박 가용성 조회. 가용성 부족도 200이다 */
    @GetMapping("/api/v1/room-types/{roomTypeId}/availability")
    public AvailabilityResponse availability(
            @PathVariable String roomTypeId,
            @RequestParam String checkIn,
            @RequestParam String checkOut,
            @RequestParam int guestCount) {
        return AvailabilityResponse.from(searchApplicationService.availability(
                RoomTypeId.of(roomTypeId), stay(checkIn, checkOut), GuestCount.of(guestCount)));
    }

    /** SEARCH-03 예상 숙박 금액 조회. 200. 요금 누락과 인원 초과는 409 */
    @GetMapping("/api/v1/room-types/{roomTypeId}/price-quote")
    public PriceQuoteResponse priceQuote(
            @PathVariable String roomTypeId,
            @RequestParam String checkIn,
            @RequestParam String checkOut,
            @RequestParam int guestCount) {
        return PriceQuoteResponse.from(searchApplicationService.priceQuote(
                RoomTypeId.of(roomTypeId), stay(checkIn, checkOut), GuestCount.of(guestCount)));
    }

    private static StayRange stay(String checkIn, String checkOut) {
        return StayRange.of(ApiDate.parse("checkIn", checkIn), ApiDate.parse("checkOut", checkOut));
    }

    /** 11 SEARCH-01 쿼리표의 regionCode 제약. 공백만이거나 32자를 넘으면 400 INVALID_REQUEST */
    private static String regionCode(String regionCode) {
        String trimmed = regionCode.trim();
        if (trimmed.isEmpty() || trimmed.length() > REGION_CODE_MAX) {
            throw new IllegalArgumentException("regionCode는 1자 이상 " + REGION_CODE_MAX
                    + "자 이하여야 한다: " + regionCode);
        }
        return trimmed;
    }
}
