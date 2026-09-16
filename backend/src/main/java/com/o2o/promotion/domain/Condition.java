package com.o2o.promotion.domain;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;

/**
 * 적용 조건 값 객체. 설계 근거: 06-2 1절 Promotion 내부 요소 Condition VO(minNights, Region,
 * campaignPeriod), 06-2 6절 프로모션 CRC의 Condition 책임 행, 05-3 4절 적용 조건 행.
 *
 * CRC의 책임 행 그대로다. 최소 숙박일과 지역과 캠페인 기간을 알고 생성 시 I8을 검사한다.
 * 11 명세가 더한 숙박 기간(StayWindow)도 여기 든다. 조건이 한 자리에 있어야 isApplicable이
 * 한 자리를 본다.
 *
 * 캠페인 기간은 시작 포함, 끝 제외다. 06-2 6절은 양끝 포함이라 적고 11 명세는 끝 날짜 제외라
 * 적는다. 둘이 어긋나고 계약 6절이 11을 따르기로 확정했다. 그 결정이 matches의 부등호다.
 *
 * I8은 06-2 3-1의 문장 그대로 시작이 종료보다 늦지 않다는 것이다(같으면 통과, 계약 8-1절 V1).
 * 11 명세의 끝 날짜가 시작보다 뒤여야 한다는 제약은 이보다 한 값 더 엄격하고 Promotion이
 * requireNonEmptyPeriods로 본다. 불변식과 명세 제약을 가른 자리다.
 *
 * regionCodes가 빈 목록이면 전체 지역이다. 11 PROMO-01 필드표. 등록된 지역 코드인지는 보지
 * 않는다. 지역 fixture가 아직 없고 카탈로그의 Region도 같은 이유로 안 본다.
 *
 * Embeddable 안에 ElementCollection을 두는 근거는 JPA 명세가 임베더블에 기본 타입 컬렉션을
 * 허용한다는 것이다. 지역 코드 목록은 조건의 일부라 조건 밖으로 빼지 않는다. EAGER인 이유는
 * 목록이 최대 100개로 작고 응답이 항상 이 목록을 내보내기 때문이다. 순서 열을 두는 이유는
 * 등록한 순서대로 되돌려 주기 위해서다. 순서 열이 없으면 읽을 때 순서가 흔들린다.
 */
@Embeddable
public class Condition {

    static final int MAX_REGION_CODES = 100;

    @Column(name = "campaign_start_date", nullable = false)
    private LocalDate campaignStartDate;

    @Column(name = "campaign_end_date", nullable = false)
    private LocalDate campaignEndDate;

    @Column(name = "stay_start_date")
    private LocalDate stayStartDate;

    @Column(name = "stay_end_date")
    private LocalDate stayEndDate;

    @Column(name = "min_nights", nullable = false)
    private int minNights;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "promotion_region_code",
            joinColumns = @JoinColumn(name = "promotion_id"))
    @OrderColumn(name = "position")
    @Column(name = "region_code", nullable = false, length = 32)
    private List<String> regionCodes;

    protected Condition() {
    }

    private Condition(LocalDate campaignStartDate, LocalDate campaignEndDate, StayWindow stayWindow,
                      int minNights, List<String> regionCodes) {
        this.campaignStartDate = campaignStartDate;
        this.campaignEndDate = campaignEndDate;
        this.stayStartDate = stayWindow == null ? null : stayWindow.start();
        this.stayEndDate = stayWindow == null ? null : stayWindow.end();
        this.minNights = minNights;
        this.regionCodes = new ArrayList<>(regionCodes);
    }

    /**
     * 생성. 06-2 6절 CRC의 생성 시 I8 검사가 여기다. 06-4 1-2 create의 Invariant 열 캠페인 순서.
     *
     * 숙박 기간은 두 날짜를 함께 받는다. 하나만 오면 IncompleteStayWindow다(11 PROMO-01 필드표,
     * V4). 둘 다 null이면 제한 없음이다.
     */
    public static Condition of(LocalDate campaignStartDate, LocalDate campaignEndDate,
                               LocalDate stayStartDate, LocalDate stayEndDate,
                               int minNights, List<String> regionCodes) {
        return of(campaignStartDate, campaignEndDate,
                stayWindowOf(stayStartDate, stayEndDate), minNights, regionCodes);
    }

    public static Condition of(LocalDate campaignStartDate, LocalDate campaignEndDate,
                               StayWindow stayWindow, int minNights, List<String> regionCodes) {
        Objects.requireNonNull(campaignStartDate, "캠페인 시작일은 null일 수 없다");
        Objects.requireNonNull(campaignEndDate, "캠페인 종료일은 null일 수 없다");
        if (campaignStartDate.isAfter(campaignEndDate)) {
            throw new InvalidPeriodException("캠페인 시작일이 종료일보다 늦다(I8): "
                    + campaignStartDate + " > " + campaignEndDate);
        }
        return new Condition(campaignStartDate, campaignEndDate, stayWindow, minNights,
                validRegionCodes(regionCodes));
    }

    /** 11 PROMO-01 필드표의 stayStartDate 행. 함께 설정하거나 둘 다 null */
    static StayWindow stayWindowOf(LocalDate stayStartDate, LocalDate stayEndDate) {
        if (stayStartDate == null && stayEndDate == null) {
            return null;
        }
        if (stayStartDate == null || stayEndDate == null) {
            throw new IncompleteStayWindowException();
        }
        return StayWindow.of(stayStartDate, stayEndDate);
    }

    /** 11 PROMO-01 필드표의 regionCodes 행. 최대 100개, 중복 금지. V3 */
    private static List<String> validRegionCodes(List<String> regionCodes) {
        Objects.requireNonNull(regionCodes, "regionCodes는 null일 수 없다. 전체 지역은 빈 목록이다");
        if (regionCodes.size() > MAX_REGION_CODES) {
            throw new InvalidRegionCodesException(
                    "지역 코드는 " + MAX_REGION_CODES + "개를 넘을 수 없다: " + regionCodes.size());
        }
        Set<String> seen = new HashSet<>();
        for (String code : regionCodes) {
            Objects.requireNonNull(code, "지역 코드는 null일 수 없다");
            if (!seen.add(code)) {
                throw new InvalidRegionCodesException("지역 코드가 중복됐다: " + code);
            }
        }
        return List.copyOf(regionCodes);
    }

    /**
     * PROMO-02. 생략한 필드를 유지하고 합친 최종 상태로 새 조건을 만든다. 11 PROMO-02 처리 규칙
     * 두 번째 줄. 합친 상태에서 I8을 다시 보는 것은 of가 한다. 계약 8-1절 V10.
     */
    public Condition merge(PromotionChanges changes) {
        StayWindow window = switch (changes.stayWindow()) {
            case StayWindowChange.Keep k -> stayWindow();
            case StayWindowChange.Clear c -> null;
            case StayWindowChange.Replace r -> r.window();
        };
        return of(changes.campaignStartDate() != null ? changes.campaignStartDate() : campaignStartDate,
                changes.campaignEndDate() != null ? changes.campaignEndDate() : campaignEndDate,
                window,
                changes.minNights() != null ? changes.minNights() : minNights,
                changes.regionCodes() != null ? changes.regionCodes() : regionCodes);
    }

    /**
     * 11 PROMO-01과 PROMO-02 필드표의 끝 날짜는 시작보다 뒤 제약. 시작 포함 끝 제외 구간에서
     * 시작과 끝이 같으면 하루도 없는 기간이다. I8보다 한 값 엄격하고 그래서 of가 아니라
     * Promotion이 부른다. 근거는 클래스 주석에 있다.
     */
    void requireNonEmptyPeriods() {
        if (campaignStartDate.equals(campaignEndDate)) {
            throw new InvalidPeriodException("캠페인 종료일은 시작일보다 뒤여야 한다: " + campaignEndDate);
        }
        StayWindow window = stayWindow();
        if (window != null && window.isEmpty()) {
            throw new InvalidPeriodException("숙박 기간 종료일은 시작일보다 뒤여야 한다: " + window.end());
        }
    }

    /**
     * 06-2 6절 CRC isApplicable의 조건 부분. 06-4 1-2 isApplicable 행의 Post. 지역과 최소
     * 숙박일이 맞고 at이 캠페인 기간 안일 때 참이다. 11 내부 처리 가격과 프로모션 절이 오늘이
     * campaignStartDate 이상 campaignEndDate 미만이고 숙박 구간이 숙박 기간 안이어야 한다고 더한다.
     * 계약 8-1절 V5. 상태(enabled)는 Promotion이 본다.
     */
    public boolean matches(String regionCode, StayRange stay, LocalDate at) {
        boolean inCampaign = !at.isBefore(campaignStartDate) && at.isBefore(campaignEndDate);
        boolean regionOk = regionCodes.isEmpty() || regionCodes.contains(regionCode);
        boolean nightsOk = stay.nights() >= minNights;
        StayWindow window = stayWindow();
        boolean windowOk = window == null || window.covers(stay);
        return inCampaign && regionOk && nightsOk && windowOk;
    }

    public LocalDate campaignStartDate() {
        return campaignStartDate;
    }

    public LocalDate campaignEndDate() {
        return campaignEndDate;
    }

    /** 숙박 기간 제한이 없으면 null이다. 11 응답 모델 Promotion의 두 필드가 그때 null이다 */
    public StayWindow stayWindow() {
        return stayStartDate == null ? null : StayWindow.of(stayStartDate, stayEndDate);
    }

    public int minNights() {
        return minNights;
    }

    /** 등록한 순서 그대로다. 빈 목록이 전체 지역이다 */
    public List<String> regionCodes() {
        return List.copyOf(regionCodes);
    }
}
