package com.o2o.promotion.api;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;

import com.o2o.promotion.domain.StayWindow;
import com.o2o.promotion.domain.StayWindowChange;
import com.o2o.shared.ApiDate;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import tools.jackson.databind.JsonNode;

/**
 * PROMO-02 요청 body. 설계 근거: 11 PROMO-02 필드표와 그 아래 문장. version만 필수이고 생략한
 * 필드는 유지하며 null 허용 필드만 null로 해제할 수 있다. version 외 변경 필드가 한 개 이상.
 *
 * 세 가지 상태를 가른다. 생략, 명시적 null, 값. 유지 필드는 명시적 null이 오면 거부해야 하고
 * (null 허용 필드만 해제 가능) 그래서 JsonSetter(nulls = FAIL)이 붙는다. 명시적 null이 오면
 * 본문을 못 읽은 것으로 400 INVALID_REQUEST가 나간다. stayStartDate와 stayEndDate는 해제가
 * 되는 필드라 JsonNode로 받는다. 생략은 null, 명시적 null은 NullNode, 값은 문자열 노드다.
 *
 * 다른 요청과 달리 레코드가 아니라 클래스다. tools.jackson.databind 3.1.5에서 레코드는 생성자로
 * 채워지는데 PropertyValueBuffer._findMissing이 생략된 생성자 인자에 NullValueProvider의
 * getAbsentValue를 부르고 NullsFailProvider는 그것을 getNullValue로 넘겨 생략까지 실패시킨다
 * (javap으로 확인, F15). 필드 주입이면 생략된 필드는 세터가 불리지 않아 FAIL이 명시적 null에만
 * 걸린다. JsonNode의 생략과 null 구분은 JsonNodeDeserializer의 getAbsentValue가 null,
 * getNullValue가 NullNode인 것으로 확인했다.
 *
 * 형식 검증이므로 컨트롤러 층이다(06-4 1-4). 합친 최종 상태의 날짜 순서는 도메인이 본다(V10).
 */
public class UpdatePromotionRequest {

    @JsonSetter("version")
    @NotNull @Min(0)
    private Long version;

    @JsonSetter(value = "name", nulls = Nulls.FAIL)
    @Size(min = 1, max = 100)
    private String name;

    @JsonSetter(value = "discountRate", nulls = Nulls.FAIL)
    @Min(1) @Max(99)
    private Integer discountRate;

    @JsonSetter(value = "campaignStartDate", nulls = Nulls.FAIL)
    private String campaignStartDate;

    @JsonSetter(value = "campaignEndDate", nulls = Nulls.FAIL)
    private String campaignEndDate;

    @JsonSetter("stayStartDate")
    private JsonNode stayStartDate;

    @JsonSetter("stayEndDate")
    private JsonNode stayEndDate;

    @JsonSetter(value = "minNights", nulls = Nulls.FAIL)
    @Min(1) @Max(30)
    private Integer minNights;

    @JsonSetter(value = "regionCodes", nulls = Nulls.FAIL)
    @Size(max = 100)
    private List<@NotBlank @Size(max = 32) String> regionCodes;

    @JsonSetter(value = "enabled", nulls = Nulls.FAIL)
    private Boolean enabled;

    @AssertTrue(message = "version 외에 바꿀 필드가 한 개 이상 필요하다")
    public boolean isAnyFieldPresent() {
        return name != null || discountRate != null || campaignStartDate != null
                || campaignEndDate != null || stayStartDate != null || stayEndDate != null
                || minNights != null || regionCodes != null || enabled != null;
    }

    /** 11 PROMO-02 처리 규칙 첫 줄. 숙박 기간을 바꾸거나 해제할 때 두 날짜를 함께 보낸다 */
    @AssertTrue(message = "stayStartDate와 stayEndDate는 함께 보내야 한다")
    public boolean isStayWindowPaired() {
        boolean startAbsent = stayStartDate == null;
        boolean endAbsent = stayEndDate == null;
        if (startAbsent != endAbsent) {
            return false;
        }
        if (startAbsent) {
            return true;
        }
        return stayStartDate.isNull() == stayEndDate.isNull();
    }

    /** 생략은 유지, 둘 다 null은 해제, 둘 다 값이면 교체. 형식 오류는 INVALID_DATE_RANGE */
    public StayWindowChange stayWindowChange() {
        if (stayStartDate == null) {
            return StayWindowChange.keep();
        }
        if (stayStartDate.isNull()) {
            return StayWindowChange.clear();
        }
        return StayWindowChange.replace(StayWindow.of(
                ApiDate.parse("stayStartDate", dateText(stayStartDate)),
                ApiDate.parse("stayEndDate", dateText(stayEndDate))));
    }

    /** 문자열이 아닌 노드는 그 JSON 표기 그대로 형식 오류가 된다 */
    private static String dateText(JsonNode node) {
        return node.isString() ? node.stringValue() : node.toString();
    }

    public Long version() {
        return version;
    }

    public String name() {
        return name;
    }

    public Integer discountRate() {
        return discountRate;
    }

    public String campaignStartDate() {
        return campaignStartDate;
    }

    public String campaignEndDate() {
        return campaignEndDate;
    }

    public Integer minNights() {
        return minNights;
    }

    public List<String> regionCodes() {
        return regionCodes;
    }

    public Boolean enabled() {
        return enabled;
    }
}
