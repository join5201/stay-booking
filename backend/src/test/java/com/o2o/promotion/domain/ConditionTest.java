package com.o2o.promotion.domain;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * V1과 V3과 V4와 V5. 설계 근거: I8(06-2 3-1, 06-4 1-1), 11 PROMO-01 필드표, 06-2 6절 프로모션
 * CRC의 isApplicable 행, 11 내부 처리 가격과 프로모션 절.
 *
 * 스프링을 띄우지 않는다. 조건 판정은 값 객체가 스스로 하는 일이라 컨테이너도 DB도 필요 없다.
 * 오늘(at)을 인자로 받으므로 Clock도 없다. 날짜 상수는 서로의 상대 위치만 뜻한다.
 *
 * 실패 케이스와 통과 케이스를 짝으로 붙이고 경계는 양쪽을 본다(테스트 규칙 T1, T2).
 * 캠페인 끝 날짜 제외는 계약 6절이 06-2와 11 명세의 어긋남을 11 쪽으로 확정한 자리라 그 경계를
 * 따로 본다.
 */
class ConditionTest {

    private static final LocalDate START = LocalDate.parse("2026-10-01");
    private static final LocalDate END = LocalDate.parse("2026-11-01");
    private static final String SEOUL = "SEOUL";
    private static final String BUSAN = "BUSAN";

    @Test
    void 캠페인_시작일이_종료일보다_늦으면_거절한다() {
        // V1. I8
        assertThrows(InvalidPeriodException.class,
                () -> Condition.of(END, START, null, null, 1, List.of()));
    }

    @Test
    void 캠페인_시작일이_종료일과_같거나_앞서면_통과한다() {
        // V1의 짝. I8은 늦지 않다이지 앞선다가 아니다. 같은 날짜는 이 층에서 통과하고
        // 명세의 하루 이상 제약은 Promotion이 본다
        Condition 같은날 = Condition.of(START, START, null, null, 1, List.of());
        Condition 앞선날 = Condition.of(START, END, null, null, 1, List.of());

        assertEquals(START, 같은날.campaignEndDate());
        assertEquals(END, 앞선날.campaignEndDate());
    }

    @Test
    void 지역_코드가_중복되면_거절한다() {
        // V3
        assertThrows(InvalidRegionCodesException.class,
                () -> Condition.of(START, END, null, null, 1, List.of(SEOUL, SEOUL)));
    }

    @Test
    void 지역_코드가_101개면_거절하고_100개는_통과한다() {
        // V3의 상한 경계 양쪽. 11 PROMO-01 필드표 최대 100개
        List<String> 백개 = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            백개.add("R" + i);
        }
        List<String> 백한개 = new ArrayList<>(백개);
        백한개.add("R100");

        assertEquals(100, Condition.of(START, END, null, null, 1, 백개).regionCodes().size());
        assertThrows(InvalidRegionCodesException.class,
                () -> Condition.of(START, END, null, null, 1, 백한개));
    }

    @Test
    void 빈_지역_목록은_전체_지역으로_통과한다() {
        // V3의 짝. 11 PROMO-01 필드표의 빈 배열은 전체 지역
        Condition condition = Condition.of(START, END, null, null, 1, List.of());
        StayRange stay = StayRange.of(START.plusDays(5), START.plusDays(7));

        assertTrue(condition.regionCodes().isEmpty());
        assertTrue(condition.matches(SEOUL, stay, START));
        assertTrue(condition.matches(BUSAN, stay, START));
    }

    @Test
    void 숙박_기간_두_날짜_중_하나만_오면_거절한다() {
        // V4. 11 PROMO-01 필드표 stayStartDate 행
        assertThrows(IncompleteStayWindowException.class,
                () -> Condition.of(START, END, START, null, 1, List.of()));
        assertThrows(IncompleteStayWindowException.class,
                () -> Condition.of(START, END, null, END, 1, List.of()));
    }

    @Test
    void 숙박_기간_두_날짜가_둘_다_없으면_제한_없음으로_통과한다() {
        // V4의 짝
        Condition condition = Condition.of(START, END, null, null, 1, List.of());

        assertNull(condition.stayWindow());
    }

    @Test
    void 숙박_기간_두_날짜가_둘_다_오면_통과한다() {
        Condition condition = Condition.of(START, END, START, END.plusMonths(1), 1, List.of());

        assertEquals(START, condition.stayWindow().start());
        assertEquals(END.plusMonths(1), condition.stayWindow().end());
    }

    @Test
    void 지역과_박수와_캠페인_기간을_모두_만족할_때만_참이다() {
        // V5. 06-2 6절 CRC isApplicable 행. 셋 중 하나라도 어긋나면 거짓이다
        Condition condition = Condition.of(START, END, null, null, 2, List.of(SEOUL));
        StayRange 이박 = StayRange.of(START.plusDays(5), START.plusDays(7));
        StayRange 일박 = StayRange.of(START.plusDays(5), START.plusDays(6));

        assertTrue(condition.matches(SEOUL, 이박, START.plusDays(3)));
        assertFalse(condition.matches(BUSAN, 이박, START.plusDays(3)));
        assertFalse(condition.matches(SEOUL, 일박, START.plusDays(3)));
        assertFalse(condition.matches(SEOUL, 이박, START.minusDays(1)));
    }

    @Test
    void 캠페인_기간은_시작_포함_끝_제외다() {
        // 계약 6절 캠페인 기간 양끝 행. 11 명세를 따른다. 경계 양쪽을 본다
        Condition condition = Condition.of(START, END, null, null, 1, List.of());
        StayRange stay = StayRange.of(END.plusDays(10), END.plusDays(12));

        assertTrue(condition.matches(SEOUL, stay, START));
        assertTrue(condition.matches(SEOUL, stay, END.minusDays(1)));
        assertFalse(condition.matches(SEOUL, stay, END));
    }

    @Test
    void 최소_박수는_같으면_통과하고_하나_모자라면_거짓이다() {
        // V5의 경계. 11 내부 처리 둘째 줄의 최소 박수 조건
        Condition condition = Condition.of(START, END, null, null, 3, List.of());

        assertTrue(condition.matches(SEOUL, StayRange.of(START, START.plusDays(3)), START));
        assertFalse(condition.matches(SEOUL, StayRange.of(START, START.plusDays(2)), START));
    }

    @Test
    void 숙박_기간이_있으면_전체_숙박_구간이_그_안에_들어야_한다() {
        // 11 내부 처리 둘째 줄. 설정한 숙박 기간 안에 전체 숙박 구간이 들어와야 한다
        LocalDate windowStart = START.plusDays(10);
        LocalDate windowEnd = START.plusDays(20);
        Condition condition = Condition.of(START, END, windowStart, windowEnd, 1, List.of());

        assertTrue(condition.matches(SEOUL, StayRange.of(windowStart, windowEnd), START));
        assertFalse(condition.matches(SEOUL,
                StayRange.of(windowStart.minusDays(1), windowEnd), START));
        assertFalse(condition.matches(SEOUL,
                StayRange.of(windowStart, windowEnd.plusDays(1)), START));
    }

    @Test
    void 합칠_때_생략한_필드는_유지하고_숙박_기간은_해제할_수_있다() {
        // 11 PROMO-02 처리 규칙. 생략은 유지, null 허용 필드만 null로 해제
        Condition condition = Condition.of(START, END, START, END, 2, List.of(SEOUL));

        Condition 유지 = condition.merge(new PromotionChanges(null, null, null, null,
                StayWindowChange.keep(), null, null, null));
        Condition 해제 = condition.merge(new PromotionChanges(null, null, null, null,
                StayWindowChange.clear(), 3, List.of(BUSAN), null));

        assertEquals(START, 유지.stayWindow().start());
        assertEquals(2, 유지.minNights());
        assertNull(해제.stayWindow());
        assertEquals(3, 해제.minNights());
        assertEquals(List.of(BUSAN), 해제.regionCodes());
        assertEquals(START, 해제.campaignStartDate());
    }
}
