package com.o2o.search.api;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import com.o2o.shared.SeoulDate;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * V13부터 V16과 SEARCH-01부터 03의 상태 코드와 응답 모양. 설계 근거: 11 검색 절, 11 에러 응답 표,
 * 11 응답 모델 PropertySearchResult와 RoomSearchResult와 Availability와 PriceQuote, T28.
 *
 * 서버를 진짜로 띄우고 HTTP로 부른다. 검색은 카탈로그와 재고와 요금과 프로모션 넷의 상태를
 * 함께 읽으므로 씨앗도 넷의 API로 심는다(테스트 규칙 T6). 지역 코드는 테스트마다 새로 만든다.
 * 이 클래스의 행은 커밋되어 다음 테스트에 남고, 검색이 지역 단위라 같은 지역을 쓰면 다른
 * 테스트의 숙소가 결과에 섞이기 때문이다.
 *
 * 서버가 실제 시계를 쓰므로 날짜는 서울 기준 오늘에서 센다(테스트 규칙 T4, T5).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SearchApiTest {

    private static final String OPERATOR = "operator_001";
    private static final String HOST = "host_001";
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String TIME_PATTERN = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z";

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).build();

    @LocalServerPort
    private int port;

    /** 서버와 같은 오늘. 서버가 서울 날짜로 판정하므로 여기도 서울 날짜다(11 명세 35행) */
    private static LocalDate today() {
        return LocalDate.now(SeoulDate.ZONE);
    }

    /** 체크인은 오늘에서 열흘 뒤다. 오늘 경계는 PROMO-05 테스트가 이미 본다 */
    private static LocalDate checkIn() {
        return today().plusDays(10);
    }

    private HttpResponse<String> send(String method, String path, String actorId, String body)
            throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + path))
                .timeout(Duration.ofSeconds(30));
        if (actorId != null) {
            b.header("X-Dev-Actor-Id", actorId);
        }
        if (body == null) {
            b.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            b.header("Content-Type", "application/json");
            b.method(method, HttpRequest.BodyPublishers.ofString(body));
        }
        return client.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> get(String path) throws Exception {
        return send("GET", path, null, null);
    }

    private static String 새_지역() {
        return "T" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private String 숙소(String region) throws Exception {
        HttpResponse<String> res = send("POST", "/api/v1/properties", HOST,
                "{\"name\":\"검색 테스트 스테이\",\"regionCode\":\"" + region
                        + "\",\"address\":\"서울특별시 중구 예시로 9\",\"description\":\"\"}");
        assertEquals(201, res.statusCode(), res.body());
        return JSON.readTree(res.body()).get("id").asString();
    }

    private String 객실_타입(String propertyId, String name, int maxOccupancy) throws Exception {
        HttpResponse<String> res = send("POST", "/api/v1/properties/" + propertyId + "/room-types",
                HOST, "{\"name\":\"" + name + "\",\"maxOccupancy\":" + maxOccupancy
                        + ",\"description\":\"\"}");
        assertEquals(201, res.statusCode(), res.body());
        return JSON.readTree(res.body()).get("id").asString();
    }

    private void 요금(String roomTypeId, LocalDate date, long amount) throws Exception {
        HttpResponse<String> res = send("POST", "/api/v1/room-types/" + roomTypeId + "/rates",
                HOST, "{\"date\":\"" + date + "\",\"amount\":" + amount + ",\"currency\":\"KRW\"}");
        assertEquals(201, res.statusCode(), res.body());
    }

    private void 재고(String roomTypeId, LocalDate date, int totalCount) throws Exception {
        HttpResponse<String> res = send("POST", "/api/v1/room-types/" + roomTypeId + "/inventories",
                HOST, "{\"date\":\"" + date + "\",\"totalCount\":" + totalCount + "}");
        assertEquals(201, res.statusCode(), res.body());
    }

    /** nights 밤 전부에 요금과 재고를 심는다 */
    private void 요금과_재고(String roomTypeId, int nights, long amount, int totalCount)
            throws Exception {
        for (int i = 0; i < nights; i++) {
            요금(roomTypeId, checkIn().plusDays(i), amount);
            재고(roomTypeId, checkIn().plusDays(i), totalCount);
        }
    }

    /** 오늘 캠페인 프로모션. 새로 만든 지역에만 건다 */
    private String 프로모션(String region, int discountRate) throws Exception {
        HttpResponse<String> res = send("POST", "/api/v1/promotions", OPERATOR,
                "{\"name\":\"" + region + " " + discountRate + "\",\"discountRate\":" + discountRate
                        + ",\"campaignStartDate\":\"" + today() + "\",\"campaignEndDate\":\""
                        + today().plusDays(30) + "\",\"minNights\":1,\"regionCodes\":[\"" + region
                        + "\"],\"enabled\":true}");
        assertEquals(201, res.statusCode(), res.body());
        return JSON.readTree(res.body()).get("id").asString();
    }

    private static String 검색(String region, int nights, int guests) {
        return "/api/v1/search/properties?regionCode=" + region + "&checkIn=" + checkIn()
                + "&checkOut=" + checkIn().plusDays(nights) + "&guestCount=" + guests;
    }

    private static String 가용성(String roomTypeId, int nights, int guests) {
        return "/api/v1/room-types/" + roomTypeId + "/availability?checkIn=" + checkIn()
                + "&checkOut=" + checkIn().plusDays(nights) + "&guestCount=" + guests;
    }

    private static String 견적(String roomTypeId, int nights, int guests) {
        return "/api/v1/room-types/" + roomTypeId + "/price-quote?checkIn=" + checkIn()
                + "&checkOut=" + checkIn().plusDays(nights) + "&guestCount=" + guests;
    }

    // SEARCH-02

    @Test
    void SEARCH_02_전_날짜_가용이면_200과_available_true와_열_칸이다() throws Exception {
        String roomTypeId = 객실_타입(숙소(새_지역()), "스탠다드", 2);
        요금(roomTypeId, checkIn(), 100_000);
        요금(roomTypeId, checkIn().plusDays(1), 100_000);
        재고(roomTypeId, checkIn(), 3);
        재고(roomTypeId, checkIn().plusDays(1), 5);

        HttpResponse<String> res = get(가용성(roomTypeId, 2, 2));

        assertEquals(200, res.statusCode(), res.body());
        JsonNode body = JSON.readTree(res.body());
        assertEquals(10, body.size(), res.body());
        assertEquals(roomTypeId, body.get("roomTypeId").asString());
        assertEquals(checkIn().toString(), body.get("checkIn").asString());
        assertEquals(checkIn().plusDays(2).toString(), body.get("checkOut").asString());
        assertEquals(2, body.get("guestCount").asInt());
        assertTrue(body.get("available").asBoolean(), res.body());
        assertEquals(3, body.get("availableCount").asInt());
        assertEquals(2, body.get("days").size());
        assertEquals(checkIn().toString(), body.get("days").get(0).get("date").asString());
        assertEquals(3, body.get("days").get(0).get("availableCount").asInt());
        assertEquals(5, body.get("days").get(1).get("availableCount").asInt());
        assertEquals(0, body.get("missingInventoryDates").size());
        assertEquals(0, body.get("missingRateDates").size());
        assertEquals(0, body.get("reasons").size());
    }

    /** V13 */
    @Test
    void SEARCH_02_가용_수_0인_날이_있으면_200과_available_false와_INVENTORY_UNAVAILABLE이다()
            throws Exception {
        String roomTypeId = 객실_타입(숙소(새_지역()), "스탠다드", 2);
        요금(roomTypeId, checkIn(), 100_000);
        요금(roomTypeId, checkIn().plusDays(1), 100_000);
        재고(roomTypeId, checkIn(), 2);
        재고(roomTypeId, checkIn().plusDays(1), 0);

        HttpResponse<String> res = get(가용성(roomTypeId, 2, 2));

        assertEquals(200, res.statusCode(), res.body());
        JsonNode body = JSON.readTree(res.body());
        assertFalse(body.get("available").asBoolean(), res.body());
        assertEquals(0, body.get("availableCount").asInt());
        assertEquals(0, body.get("days").get(1).get("availableCount").asInt());
        assertEquals(1, body.get("reasons").size());
        assertEquals("INVENTORY_UNAVAILABLE", body.get("reasons").get(0).asString());
        assertEquals(0, body.get("missingInventoryDates").size());
    }

    /** V14 */
    @Test
    void SEARCH_02_재고와_요금이_없는_날은_missing에_들고_days에_null이다() throws Exception {
        String roomTypeId = 객실_타입(숙소(새_지역()), "스탠다드", 2);
        LocalDate day2 = checkIn().plusDays(1);
        LocalDate day3 = checkIn().plusDays(2);
        요금(roomTypeId, checkIn(), 100_000);
        재고(roomTypeId, checkIn(), 4);
        재고(roomTypeId, day3, 4);

        HttpResponse<String> res = get(가용성(roomTypeId, 3, 2));

        assertEquals(200, res.statusCode(), res.body());
        JsonNode body = JSON.readTree(res.body());
        assertFalse(body.get("available").asBoolean(), res.body());
        assertEquals(0, body.get("availableCount").asInt());
        assertEquals(3, body.get("days").size());
        assertEquals(4, body.get("days").get(0).get("availableCount").asInt());
        assertTrue(body.get("days").get(1).get("availableCount").isNull(), res.body());
        assertEquals(day2.toString(), body.get("days").get(1).get("date").asString());
        assertEquals(4, body.get("days").get(2).get("availableCount").asInt());
        assertEquals(1, body.get("missingInventoryDates").size());
        assertEquals(day2.toString(), body.get("missingInventoryDates").get(0).asString());
        assertEquals(2, body.get("missingRateDates").size());
        assertEquals(day2.toString(), body.get("missingRateDates").get(0).asString());
        assertEquals(day3.toString(), body.get("missingRateDates").get(1).asString());
        assertEquals(2, body.get("reasons").size());
        assertEquals("INVENTORY_NOT_CONFIGURED", body.get("reasons").get(0).asString());
        assertEquals("RATE_NOT_CONFIGURED", body.get("reasons").get(1).asString());
    }

    @Test
    void SEARCH_02_인원_초과는_200과_reasons_OCCUPANCY_EXCEEDED이다() throws Exception {
        String roomTypeId = 객실_타입(숙소(새_지역()), "스탠다드", 2);
        요금과_재고(roomTypeId, 1, 100_000, 3);

        HttpResponse<String> res = get(가용성(roomTypeId, 1, 3));

        assertEquals(200, res.statusCode(), res.body());
        JsonNode body = JSON.readTree(res.body());
        assertFalse(body.get("available").asBoolean(), res.body());
        assertEquals(3, body.get("availableCount").asInt());
        assertEquals(1, body.get("reasons").size());
        assertEquals("OCCUPANCY_EXCEEDED", body.get("reasons").get(0).asString());
    }

    /** V13 실패 짝 */
    @Test
    void SEARCH_02_날짜_형식_오류는_400_INVALID_DATE_RANGE이다() throws Exception {
        String roomTypeId = 객실_타입(숙소(새_지역()), "스탠다드", 2);

        HttpResponse<String> res = get("/api/v1/room-types/" + roomTypeId
                + "/availability?checkIn=2026/10/01&checkOut=" + checkIn().plusDays(1)
                + "&guestCount=2");

        assertEquals(400, res.statusCode(), res.body());
        assertEquals("INVALID_DATE_RANGE", JSON.readTree(res.body()).get("code").asString());
    }

    @Test
    void SEARCH_02_checkOut이_checkIn_이하이면_400_INVALID_DATE_RANGE이다() throws Exception {
        String roomTypeId = 객실_타입(숙소(새_지역()), "스탠다드", 2);

        HttpResponse<String> res = get("/api/v1/room-types/" + roomTypeId
                + "/availability?checkIn=" + checkIn() + "&checkOut=" + checkIn()
                + "&guestCount=2");

        assertEquals(400, res.statusCode(), res.body());
        assertEquals("INVALID_DATE_RANGE", JSON.readTree(res.body()).get("code").asString());
    }

    @Test
    void SEARCH_02_없는_객실은_404_RESOURCE_NOT_FOUND이다() throws Exception {
        HttpResponse<String> res = get(가용성("room_없음", 1, 2));

        assertEquals(404, res.statusCode(), res.body());
        assertEquals("RESOURCE_NOT_FOUND", JSON.readTree(res.body()).get("code").asString());
    }

    // SEARCH-01

    /** V15 */
    @Test
    void SEARCH_01_네_조건을_다_만족하는_객실만_들고_그런_객실이_없는_숙소는_빠진다() throws Exception {
        String region = 새_지역();
        String propertyA = 숙소(region);
        String a1 = 객실_타입(propertyA, "수용 2 정상", 2);
        요금과_재고(a1, 2, 100_000, 3);
        String a2 = 객실_타입(propertyA, "수용 1 인원 초과", 1);
        요금과_재고(a2, 2, 50_000, 3);
        String propertyB = 숙소(region);
        String b1 = 객실_타입(propertyB, "재고 0", 2);
        요금(b1, checkIn(), 50_000);
        요금(b1, checkIn().plusDays(1), 50_000);
        재고(b1, checkIn(), 3);
        재고(b1, checkIn().plusDays(1), 0);
        String b2 = 객실_타입(propertyB, "요금 누락", 2);
        재고(b2, checkIn(), 3);
        재고(b2, checkIn().plusDays(1), 3);
        요금(b2, checkIn(), 50_000);
        String b3 = 객실_타입(propertyB, "재고 누락", 2);
        요금(b3, checkIn(), 50_000);
        요금(b3, checkIn().plusDays(1), 50_000);
        재고(b3, checkIn(), 3);
        숙소(region);

        HttpResponse<String> res = get(검색(region, 2, 2));

        assertEquals(200, res.statusCode(), res.body());
        JsonNode body = JSON.readTree(res.body());
        assertEquals(5, body.size(), res.body());
        assertEquals(1, body.get("totalElements").asLong());
        assertEquals(1, body.get("items").size());
        JsonNode item = body.get("items").get(0);
        assertEquals(4, item.size(), res.body());
        assertEquals(9, item.get("property").size(), res.body());
        assertEquals(propertyA, item.get("property").get("id").asString());
        assertEquals(region, item.get("property").get("regionCode").asString());
        assertEquals("KRW", item.get("currency").asString());
        assertEquals(200_000, item.get("lowestTotalAmount").asLong());
        assertEquals(1, item.get("availableRoomTypes").size());
        JsonNode room = item.get("availableRoomTypes").get(0);
        assertEquals(5, room.size(), res.body());
        assertEquals(a1, room.get("roomTypeId").asString());
        assertEquals("수용 2 정상", room.get("name").asString());
        assertEquals(2, room.get("maxOccupancy").asInt());
        assertEquals(3, room.get("availableCount").asInt());
        assertEquals(200_000, room.get("totalAmount").asLong());
    }

    @Test
    void SEARCH_01_결과가_없으면_200과_빈_items다() throws Exception {
        HttpResponse<String> res = get(검색(새_지역(), 1, 2));

        assertEquals(200, res.statusCode(), res.body());
        JsonNode body = JSON.readTree(res.body());
        assertEquals(0, body.get("items").size());
        assertEquals(0, body.get("totalElements").asLong());
        assertEquals(0, body.get("totalPages").asInt());
        assertEquals(0, body.get("page").asInt());
        assertEquals(20, body.get("size").asInt());
    }

    /** 11 SEARCH-01 내부 처리 여섯째 줄. 검색 금액은 PROMO-05와 같은 PricingService가 낸다 */
    @Test
    void SEARCH_01_lowestTotalAmount는_객실_중_최소이고_프로모션_할인이_반영된다() throws Exception {
        String region = 새_지역();
        String propertyId = 숙소(region);
        String expensive = 객실_타입(propertyId, "디럭스", 2);
        요금과_재고(expensive, 2, 100_000, 1);
        String cheap = 객실_타입(propertyId, "스탠다드", 2);
        요금과_재고(cheap, 2, 80_000, 1);
        프로모션(region, 10);

        HttpResponse<String> res = get(검색(region, 2, 2));

        assertEquals(200, res.statusCode(), res.body());
        JsonNode item = JSON.readTree(res.body()).get("items").get(0);
        assertEquals(144_000, item.get("lowestTotalAmount").asLong());
        assertEquals(2, item.get("availableRoomTypes").size());
        // 객실 순서는 id 오름차순이고 id는 무작위라 순서 대신 합으로 본다. 180000 + 144000
        long sum = 0;
        for (JsonNode room : item.get("availableRoomTypes")) {
            sum += room.get("totalAmount").asLong();
        }
        assertEquals(324_000, sum, res.body());
    }

    @Test
    void SEARCH_01_쪽은_size대로_자르고_totalPages를_센다() throws Exception {
        String region = 새_지역();
        for (int i = 0; i < 3; i++) {
            String roomTypeId = 객실_타입(숙소(region), "스탠다드", 2);
            요금과_재고(roomTypeId, 1, 70_000, 2);
        }

        HttpResponse<String> first = get(검색(region, 1, 2) + "&page=0&size=2");
        HttpResponse<String> second = get(검색(region, 1, 2) + "&page=1&size=2");

        assertEquals(200, first.statusCode(), first.body());
        JsonNode firstBody = JSON.readTree(first.body());
        assertEquals(2, firstBody.get("items").size());
        assertEquals(3, firstBody.get("totalElements").asLong());
        assertEquals(2, firstBody.get("totalPages").asInt());
        assertEquals(2, firstBody.get("size").asInt());
        JsonNode secondBody = JSON.readTree(second.body());
        assertEquals(1, secondBody.get("items").size());
        assertEquals(1, secondBody.get("page").asInt());
    }

    /** 11 SEARCH-01 쿼리표 regionCode 1부터 32자. 경계 짝 */
    @Test
    void SEARCH_01_regionCode_32자는_200이고_33자는_400_INVALID_REQUEST이다() throws Exception {
        String region32 = ("T" + UUID.randomUUID().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", "")).substring(0, 32);
        String roomTypeId = 객실_타입(숙소(region32), "스탠다드", 2);
        요금과_재고(roomTypeId, 1, 70_000, 2);

        HttpResponse<String> ok = get(검색(region32, 1, 2));
        HttpResponse<String> tooLong = get(검색(region32 + "X", 1, 2));

        assertEquals(200, ok.statusCode(), ok.body());
        assertEquals(1, JSON.readTree(ok.body()).get("items").size());
        assertEquals(400, tooLong.statusCode(), tooLong.body());
        assertEquals("INVALID_REQUEST", JSON.readTree(tooLong.body()).get("code").asString());
    }

    @Test
    void SEARCH_01_guestCount_0과_size_101은_400_INVALID_REQUEST이다() throws Exception {
        HttpResponse<String> guests = get(검색(새_지역(), 1, 0));
        HttpResponse<String> size = get(검색(새_지역(), 1, 2) + "&size=101");

        assertEquals(400, guests.statusCode(), guests.body());
        assertEquals("INVALID_REQUEST", JSON.readTree(guests.body()).get("code").asString());
        assertEquals(400, size.statusCode(), size.body());
        assertEquals("INVALID_REQUEST", JSON.readTree(size.body()).get("code").asString());
    }

    @Test
    void SEARCH_01_31박은_400_INVALID_DATE_RANGE이고_30박은_200이다() throws Exception {
        HttpResponse<String> over = get(검색(새_지역(), 31, 2));
        HttpResponse<String> max = get(검색(새_지역(), 30, 2));

        assertEquals(400, over.statusCode(), over.body());
        assertEquals("INVALID_DATE_RANGE", JSON.readTree(over.body()).get("code").asString());
        assertEquals(200, max.statusCode(), max.body());
    }

    // SEARCH-03

    /** V16 */
    @Test
    void SEARCH_03_견적은_200과_일곱_칸이고_날짜별_finalAmount_합이_totalAmount다() throws Exception {
        String region = 새_지역();
        String roomTypeId = 객실_타입(숙소(region), "스탠다드", 2);
        요금(roomTypeId, checkIn(), 33_333);
        요금(roomTypeId, checkIn().plusDays(1), 33_333);
        요금(roomTypeId, checkIn().plusDays(2), 33_334);
        String promotionId = 프로모션(region, 10);

        HttpResponse<String> res = get(견적(roomTypeId, 3, 2));

        assertEquals(200, res.statusCode(), res.body());
        JsonNode body = JSON.readTree(res.body());
        assertEquals(7, body.size(), res.body());
        assertEquals(roomTypeId, body.get("roomTypeId").asString());
        assertEquals(checkIn().toString(), body.get("checkIn").asString());
        assertEquals(checkIn().plusDays(3).toString(), body.get("checkOut").asString());
        assertEquals(2, body.get("guestCount").asInt());
        assertEquals(3, body.get("nights").asInt());
        assertTrue(body.get("estimatedAt").asString().matches(TIME_PATTERN), res.body());
        JsonNode price = body.get("price");
        assertEquals(6, price.size(), res.body());
        assertEquals("KRW", price.get("currency").asString());
        assertEquals(100_000, price.get("baseTotalAmount").asLong());
        assertEquals(10_000, price.get("discountTotalAmount").asLong());
        assertEquals(90_000, price.get("totalAmount").asLong());
        assertEquals(promotionId, price.get("appliedPromotion").get("id").asString());
        assertEquals(10, price.get("appliedPromotion").get("discountRate").asInt());
        JsonNode days = price.get("days");
        assertEquals(3, days.size());
        long finalSum = 0;
        long baseSum = 0;
        long discountSum = 0;
        for (JsonNode day : days) {
            assertEquals(4, day.size(), res.body());
            baseSum += day.get("baseAmount").asLong();
            discountSum += day.get("discountAmount").asLong();
            finalSum += day.get("finalAmount").asLong();
            assertEquals(day.get("baseAmount").asLong() - day.get("discountAmount").asLong(),
                    day.get("finalAmount").asLong());
        }
        assertEquals(price.get("baseTotalAmount").asLong(), baseSum);
        assertEquals(price.get("discountTotalAmount").asLong(), discountSum);
        assertEquals(price.get("totalAmount").asLong(), finalSum);
        assertEquals(checkIn().toString(), days.get(0).get("date").asString());
        assertEquals(checkIn().plusDays(2).toString(), days.get(2).get("date").asString());
    }

    @Test
    void SEARCH_03_프로모션이_없으면_할인_0과_appliedPromotion_null이다() throws Exception {
        String roomTypeId = 객실_타입(숙소(새_지역()), "스탠다드", 2);
        요금(roomTypeId, checkIn(), 100_000);
        요금(roomTypeId, checkIn().plusDays(1), 120_000);

        HttpResponse<String> res = get(견적(roomTypeId, 2, 2));

        assertEquals(200, res.statusCode(), res.body());
        JsonNode price = JSON.readTree(res.body()).get("price");
        assertEquals(220_000, price.get("baseTotalAmount").asLong());
        assertEquals(0, price.get("discountTotalAmount").asLong());
        assertEquals(220_000, price.get("totalAmount").asLong());
        assertTrue(price.get("appliedPromotion").isNull(), res.body());
        assertEquals(0, price.get("days").get(1).get("discountAmount").asLong());
        assertEquals(120_000, price.get("days").get(1).get("finalAmount").asLong());
    }

    /** 11 SEARCH-03 처리 규칙 둘째 줄. 재고를 보장하지 않으므로 재고가 없어도 견적은 난다 */
    @Test
    void SEARCH_03_재고가_없어도_요금만_있으면_200이다() throws Exception {
        String roomTypeId = 객실_타입(숙소(새_지역()), "스탠다드", 2);
        요금(roomTypeId, checkIn(), 100_000);

        HttpResponse<String> res = get(견적(roomTypeId, 1, 2));

        assertEquals(200, res.statusCode(), res.body());
        assertEquals(100_000, JSON.readTree(res.body()).get("price").get("totalAmount").asLong());
    }

    /** V16 실패 짝 */
    @Test
    void SEARCH_03_요금이_없는_날이_있으면_409_RATE_NOT_CONFIGURED이다() throws Exception {
        String roomTypeId = 객실_타입(숙소(새_지역()), "스탠다드", 2);
        요금(roomTypeId, checkIn(), 100_000);

        HttpResponse<String> res = get(견적(roomTypeId, 2, 2));

        assertEquals(409, res.statusCode(), res.body());
        JsonNode body = JSON.readTree(res.body());
        assertEquals("RATE_NOT_CONFIGURED", body.get("code").asString());
        assertEquals(1, body.get("details").size());
        assertTrue(body.get("details").get(0).get("reason").asString()
                .contains(checkIn().plusDays(1).toString()), res.body());
    }

    @Test
    void SEARCH_03_인원_초과는_409_OCCUPANCY_EXCEEDED이다() throws Exception {
        String roomTypeId = 객실_타입(숙소(새_지역()), "스탠다드", 2);
        요금(roomTypeId, checkIn(), 100_000);

        HttpResponse<String> res = get(견적(roomTypeId, 1, 3));

        assertEquals(409, res.statusCode(), res.body());
        assertEquals("OCCUPANCY_EXCEEDED", JSON.readTree(res.body()).get("code").asString());
    }

    @Test
    void SEARCH_03_없는_객실은_404_RESOURCE_NOT_FOUND이다() throws Exception {
        HttpResponse<String> res = get(견적("room_없음", 1, 2));

        assertEquals(404, res.statusCode(), res.body());
        assertEquals("RESOURCE_NOT_FOUND", JSON.readTree(res.body()).get("code").asString());
    }

    @Test
    void SEARCH_03_과거_체크인은_400_INVALID_DATE_RANGE이다() throws Exception {
        String roomTypeId = 객실_타입(숙소(새_지역()), "스탠다드", 2);

        HttpResponse<String> res = get("/api/v1/room-types/" + roomTypeId
                + "/price-quote?checkIn=" + today().minusDays(1) + "&checkOut=" + today()
                + "&guestCount=2");

        assertEquals(400, res.statusCode(), res.body());
        assertEquals("INVALID_DATE_RANGE", JSON.readTree(res.body()).get("code").asString());
    }
}
