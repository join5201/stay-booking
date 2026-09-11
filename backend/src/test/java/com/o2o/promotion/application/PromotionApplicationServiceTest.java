package com.o2o.promotion.application;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.annotation.Transactional;

import com.o2o.catalog.application.CatalogApplicationService;
import com.o2o.catalog.domain.Property;
import com.o2o.catalog.domain.RoomType;
import com.o2o.catalog.domain.RoomTypeNotFoundException;
import com.o2o.inventory.application.InventoryApplicationService;
import com.o2o.promotion.domain.InvalidPeriodException;
import com.o2o.promotion.domain.InvalidStayRangeException;
import com.o2o.promotion.domain.OccupancyExceededException;
import com.o2o.promotion.domain.Promotion;
import com.o2o.promotion.domain.PromotionChanges;
import com.o2o.promotion.domain.PromotionNotFoundException;
import com.o2o.promotion.domain.RateNotConfiguredException;
import com.o2o.promotion.domain.StayRange;
import com.o2o.promotion.domain.StayWindowChange;
import com.o2o.shared.GuestCount;
import com.o2o.shared.HostId;
import com.o2o.shared.PageQuery;
import com.o2o.shared.PageResult;
import com.o2o.shared.PromotionId;
import com.o2o.shared.RoomTypeId;
import com.o2o.shared.VersionConflictException;

import jakarta.persistence.EntityManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * V5의 DB 몫과 V9와 V10의 응용 몫과 V11과 V12의 응용 몫. 설계 근거: 계약 8-1절, 06-4 1-2
 * 프로모션 계약표, 11 PROMO-01부터 05, 11 내부 처리 가격과 프로모션 절.
 *
 * 실제 MySQL에 붙는다. 캠페인 기간 필터(findEnabledOn)는 JPQL이고 임베더블 속성을 타는 조건이라
 * 진짜 DB에서만 확인된다(테스트 규칙 T3). 트랜잭션 롤백으로 데이터를 정리한다.
 *
 * 시각을 고정한다(T5). 고정 시각 UTC 20시는 서울 날짜로 다음 날이라 오늘이 서울 기준으로
 * 잘리는지도 같이 본다(11 공통 35행). 날짜는 전부 그 오늘에서 센다(T4).
 *
 * 지역 코드를 테스트마다 새로 만든다. 후보 없음 케이스가 무엇에 의존하는지를 코드에
 * 드러내기 위해서다. 롤백이 다른 테스트의 행을 막지만 읽는 사람은 그것을 매번 떠올리지 않는다.
 */
@SpringBootTest
@Transactional
class PromotionApplicationServiceTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-10-01T20:00:00Z");
    private static final LocalDate SEOUL_TODAY = LocalDate.parse("2026-10-02");
    private static final HostId HOST = HostId.of("host_001");
    private static final LocalDate CHECK_IN = SEOUL_TODAY.plusDays(5);
    private static final LocalDate CHECK_OUT = SEOUL_TODAY.plusDays(7);

    @TestConfiguration
    static class FixedClockConfiguration {

        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
        }
    }

    @Autowired
    private PromotionApplicationService promotionApplicationService;

    @Autowired
    private CatalogApplicationService catalogApplicationService;

    @Autowired
    private InventoryApplicationService inventoryApplicationService;

    @Autowired
    private EntityManager entityManager;

    @Test
    void 등록하면_조회로_같은_값을_읽는다() {
        // PROMO-01과 PROMO-03. 11 응답 모델 Promotion 열세 칸의 원천
        Promotion created = promotionApplicationService.create(new PromotionDraft(
                "가을 할인", 10, SEOUL_TODAY, SEOUL_TODAY.plusDays(30), null, null, 2,
                List.of("SEOUL", "BUSAN"), true));
        entityManager.flush();
        entityManager.clear();

        Promotion found = promotionApplicationService.get(created.id());

        assertEquals("가을 할인", found.name());
        assertEquals(10, found.discountRate());
        assertEquals(SEOUL_TODAY, found.condition().campaignStartDate());
        assertEquals(SEOUL_TODAY.plusDays(30), found.condition().campaignEndDate());
        assertNull(found.condition().stayWindow());
        assertEquals(2, found.condition().minNights());
        assertEquals(List.of("SEOUL", "BUSAN"), found.condition().regionCodes());
        assertTrue(found.enabled());
        assertEquals(0L, found.version());
        assertEquals(FIXED_NOW, found.createdAt());
        assertEquals(FIXED_NOW, found.updatedAt());
    }

    @Test
    void 없는_ID를_조회하면_거절한다() {
        assertThrows(PromotionNotFoundException.class,
                () -> promotionApplicationService.get(PromotionId.newId()));
    }

    @Test
    void 지난_버전으로_수정하면_거절하고_최근_변경이_남는다() {
        // V9의 응용 몫. 11 명세 1226행. 두 번째 요청이 첫 번째 요청 전 버전을 들고 온다
        Promotion created = 전체지역_프로모션(10);
        promotionApplicationService.update(created.id(), 0L, 변경(null, false));

        assertThrows(VersionConflictException.class,
                () -> promotionApplicationService.update(created.id(), 0L, 변경(20, null)));

        entityManager.clear();
        Promotion 현재 = promotionApplicationService.get(created.id());
        assertFalse(현재.enabled());
        assertEquals(10, 현재.discountRate());
        assertEquals(1L, 현재.version());
    }

    @Test
    void 생략한_필드를_유지하고_합친_최종_상태에서_날짜_순서를_본다() {
        // V10의 응용 몫. 11 명세 1252행. 종료일만 보내도 기존 시작일과 합쳐서 본다
        Promotion created = 전체지역_프로모션(10);
        PromotionChanges 종료일만 = new PromotionChanges(null, null, null,
                SEOUL_TODAY.minusDays(1), StayWindowChange.keep(), null, null, null);

        assertThrows(InvalidPeriodException.class,
                () -> promotionApplicationService.update(created.id(), 0L, 종료일만));

        PromotionChanges 시작일과_종료일 = new PromotionChanges(null, null,
                SEOUL_TODAY.minusDays(3), SEOUL_TODAY.minusDays(1), StayWindowChange.keep(),
                null, null, null);
        Promotion updated = promotionApplicationService.update(created.id(), 0L, 시작일과_종료일);

        assertEquals(SEOUL_TODAY.minusDays(3), updated.condition().campaignStartDate());
        assertEquals(10, updated.discountRate());
        assertEquals(1L, updated.version());
    }

    @Test
    void 없는_ID를_수정하면_거절한다() {
        assertThrows(PromotionNotFoundException.class,
                () -> promotionApplicationService.update(PromotionId.newId(), 0L, 변경(20, null)));
    }

    @Test
    void 목록은_enabled로_거르고_생략하면_전체다() {
        // PROMO-04. 11 PROMO-04 쿼리표 enabled 행
        Promotion 사용 = 전체지역_프로모션(10);
        Promotion 미사용 = promotionApplicationService.create(new PromotionDraft(
                "미사용", 10, SEOUL_TODAY, SEOUL_TODAY.plusDays(30), null, null, 1, List.of(),
                false));

        PageResult<Promotion> 사용만 = promotionApplicationService.list(true, PageQuery.of(0, 100));
        PageResult<Promotion> 미사용만 = promotionApplicationService.list(false, PageQuery.of(0, 100));
        PageResult<Promotion> 전체 = promotionApplicationService.list(null, PageQuery.of(0, 100));

        assertTrue(ids(사용만).contains(사용.id()));
        assertFalse(ids(사용만).contains(미사용.id()));
        assertTrue(ids(미사용만).contains(미사용.id()));
        assertFalse(ids(미사용만).contains(사용.id()));
        assertTrue(ids(전체).contains(사용.id()));
        assertTrue(ids(전체).contains(미사용.id()));
        assertEquals(0, 전체.page());
        assertEquals(100, 전체.size());
    }

    @Test
    void 조건이_맞는_프로모션을_후보로_내고_할인액이_큰_하나를_선택한다() {
        // PROMO-05. 11 PROMO-05 처리 규칙 첫째와 둘째 줄
        String region = 새_지역();
        RoomTypeId roomTypeId = 객실_타입(region, 2);
        요금(roomTypeId, CHECK_IN, 100_000);
        요금(roomTypeId, CHECK_IN.plusDays(1), 100_000);
        Promotion 십 = 지역_프로모션(region, 10);
        Promotion 이십 = 지역_프로모션(region, 20);

        ApplicablePromotionsResult result = promotionApplicationService.applicablePromotions(
                roomTypeId, StayRange.of(CHECK_IN, CHECK_OUT), GuestCount.of(2));

        assertEquals(2, result.items().size());
        assertEquals(이십.id(), result.selectedPromotionId());
        assertEquals(이십.id(), result.items().get(0).id());
        assertEquals(40_000L, result.items().get(0).discountAmount());
        assertTrue(result.items().get(0).selected());
        assertEquals(십.id(), result.items().get(1).id());
        assertEquals(20_000L, result.items().get(1).discountAmount());
        assertFalse(result.items().get(1).selected());
        assertEquals(FIXED_NOW, result.evaluatedAt());
        assertEquals(2, result.guestCount().value());
    }

    @Test
    void 인원이_최대_인원을_넘으면_거절하고_같으면_통과한다() {
        // V12의 응용 몫. 11 명세 1392행. 경계 양쪽
        String region = 새_지역();
        RoomTypeId roomTypeId = 객실_타입(region, 2);
        요금(roomTypeId, CHECK_IN, 100_000);
        요금(roomTypeId, CHECK_IN.plusDays(1), 100_000);
        StayRange stay = StayRange.of(CHECK_IN, CHECK_OUT);

        assertThrows(OccupancyExceededException.class,
                () -> promotionApplicationService.applicablePromotions(
                        roomTypeId, stay, GuestCount.of(3)));
        assertEquals(2, promotionApplicationService.applicablePromotions(
                roomTypeId, stay, GuestCount.of(2)).guestCount().value());
    }

    @Test
    void 요금_없는_날짜가_있으면_그_날짜_목록과_함께_거절한다() {
        // V11의 응용 몫. 11 명세 1391행. 둘째 밤의 요금이 없다
        String region = 새_지역();
        RoomTypeId roomTypeId = 객실_타입(region, 2);
        요금(roomTypeId, CHECK_IN, 100_000);

        RateNotConfiguredException e = assertThrows(RateNotConfiguredException.class,
                () -> promotionApplicationService.applicablePromotions(
                        roomTypeId, StayRange.of(CHECK_IN, CHECK_OUT), GuestCount.of(2)));

        assertEquals(List.of(CHECK_IN.plusDays(1)), e.missingDates());
    }

    @Test
    void 후보가_없으면_빈_목록과_null이다() {
        // V12의 응용 몫. 11 명세 1402행. 이 지역을 대상으로 한 프로모션이 없다
        String region = 새_지역();
        RoomTypeId roomTypeId = 객실_타입(region, 2);
        요금(roomTypeId, CHECK_IN, 100_000);
        요금(roomTypeId, CHECK_IN.plusDays(1), 100_000);
        지역_프로모션(새_지역(), 10);

        ApplicablePromotionsResult result = promotionApplicationService.applicablePromotions(
                roomTypeId, StayRange.of(CHECK_IN, CHECK_OUT), GuestCount.of(2));

        assertTrue(result.items().isEmpty());
        assertNull(result.selectedPromotionId());
    }

    @Test
    void 체크인이_오늘보다_앞이면_거절하고_오늘이면_통과한다() {
        // 11 PROMO-05 쿼리표 checkIn 행. 오늘은 서울 기준이라 UTC 날짜인 10월 1일은 과거다
        String region = 새_지역();
        RoomTypeId roomTypeId = 객실_타입(region, 2);
        요금(roomTypeId, SEOUL_TODAY, 100_000);

        assertThrows(InvalidStayRangeException.class,
                () -> promotionApplicationService.applicablePromotions(roomTypeId,
                        StayRange.of(SEOUL_TODAY.minusDays(1), SEOUL_TODAY), GuestCount.of(1)));
        assertEquals(SEOUL_TODAY, promotionApplicationService.applicablePromotions(roomTypeId,
                StayRange.of(SEOUL_TODAY, SEOUL_TODAY.plusDays(1)), GuestCount.of(1))
                .stay().checkIn());
    }

    @Test
    void 캠페인_종료일이_오늘이면_후보에서_빠지고_시작일이_오늘이면_든다() {
        // V5의 DB 몫. 11 내부 처리 첫 줄. findEnabledOn의 부등호를 진짜 DB에서 본다
        String region = 새_지역();
        RoomTypeId roomTypeId = 객실_타입(region, 2);
        요금(roomTypeId, CHECK_IN, 100_000);
        요금(roomTypeId, CHECK_IN.plusDays(1), 100_000);
        Promotion 어제끝 = promotionApplicationService.create(new PromotionDraft(
                "어제 끝", 10, SEOUL_TODAY.minusDays(10), SEOUL_TODAY, null, null, 1,
                List.of(region), true));
        Promotion 오늘시작 = promotionApplicationService.create(new PromotionDraft(
                "오늘 시작", 10, SEOUL_TODAY, SEOUL_TODAY.plusDays(1), null, null, 1,
                List.of(region), true));
        Promotion 미사용 = promotionApplicationService.create(new PromotionDraft(
                "미사용", 30, SEOUL_TODAY, SEOUL_TODAY.plusDays(1), null, null, 1,
                List.of(region), false));

        ApplicablePromotionsResult result = promotionApplicationService.applicablePromotions(
                roomTypeId, StayRange.of(CHECK_IN, CHECK_OUT), GuestCount.of(2));

        assertEquals(1, result.items().size());
        assertEquals(오늘시작.id(), result.items().get(0).id());
        assertFalse(result.items().stream().anyMatch(i -> i.id().equals(어제끝.id())));
        assertFalse(result.items().stream().anyMatch(i -> i.id().equals(미사용.id())));
    }

    @Test
    void 없는_객실_타입이면_거절한다() {
        assertThrows(RoomTypeNotFoundException.class,
                () -> promotionApplicationService.applicablePromotions(RoomTypeId.newId(),
                        StayRange.of(CHECK_IN, CHECK_OUT), GuestCount.of(1)));
    }

    private Promotion 전체지역_프로모션(int discountRate) {
        return promotionApplicationService.create(new PromotionDraft(
                "전체 " + discountRate, discountRate, SEOUL_TODAY, SEOUL_TODAY.plusDays(30),
                null, null, 1, List.of(), true));
    }

    private Promotion 지역_프로모션(String region, int discountRate) {
        return promotionApplicationService.create(new PromotionDraft(
                region + " " + discountRate, discountRate, SEOUL_TODAY, SEOUL_TODAY.plusDays(30),
                null, null, 1, List.of(region), true));
    }

    private RoomTypeId 객실_타입(String region, int maxOccupancy) {
        Property property = catalogApplicationService.registerProperty(
                HOST, "테스트 스테이", region, "서울특별시 중구 예시로 1", "");
        RoomType roomType = catalogApplicationService.registerRoomType(
                HOST, property.id(), "스탠다드", maxOccupancy, "");
        return roomType.id();
    }

    private void 요금(RoomTypeId roomTypeId, LocalDate stayDate, long amount) {
        inventoryApplicationService.registerRate(HOST, roomTypeId, stayDate, amount);
    }

    private static String 새_지역() {
        return "T" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private static PromotionChanges 변경(Integer discountRate, Boolean enabled) {
        return new PromotionChanges(null, discountRate, null, null, StayWindowChange.keep(),
                null, null, enabled);
    }

    private static List<PromotionId> ids(PageResult<Promotion> page) {
        return page.items().stream().map(Promotion::id).toList();
    }
}
