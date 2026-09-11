package com.o2o.promotion.api;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
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
 * V9부터 V12와 PROMO-01부터 05의 상태 코드와 응답 모양. 설계 근거: 11 프로모션 절, 11 공통
 * 요청과 응답 규칙, 11 에러 응답 표, 11 응답 모델 Promotion과 ApplicablePromotions, T28.
 *
 * 서버를 진짜로 띄우고 HTTP로 부른다. 상태 코드와 Location과 응답 칸 수는 컨트롤러와
 * 핸들러와 직렬화 설정이 함께 만드는 것이라 서비스만 불러서는 확인되지 않는다.
 *
 * 서버가 실제 시계를 쓰므로 날짜는 서울 기준 오늘에서 센다(테스트 규칙 T4, T5). 이 클래스의
 * 행은 커밋되어 다음 테스트에 남는다. 그래서 적용 대상이 아닌 프로모션은 캠페인을 먼 미래에
 * 두고, 적용 대상은 테스트마다 새로 만든 지역 코드에만 건다. 전체 지역 프로모션을 오늘
 * 캠페인으로 남기면 다른 테스트의 후보 없음이 깨진다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PromotionApiTest {

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

    /** 적용 대상이 아닌 프로모션. 캠페인이 먼 미래라 어느 조회에도 후보가 되지 않는다 */
    private static String 먼_미래_본문(String regionCodesJson) {
        LocalDate start = today().plusDays(100);
        return "{\"name\":\"가을 할인\",\"discountRate\":10,\"campaignStartDate\":\"" + start
                + "\",\"campaignEndDate\":\"" + start.plusDays(30)
                + "\",\"minNights\":2,\"regionCodes\":" + regionCodesJson + "}";
    }

    /** 오늘 캠페인. 새로 만든 지역에만 건다 */
    private static String 오늘_본문(String region, int discountRate) {
        return "{\"name\":\"" + region + " " + discountRate + "\",\"discountRate\":" + discountRate
                + ",\"campaignStartDate\":\"" + today() + "\",\"campaignEndDate\":\""
                + today().plusDays(30) + "\",\"minNights\":1,\"regionCodes\":[\"" + region
                + "\"],\"enabled\":true}";
    }

    private JsonNode 등록(String body) throws Exception {
        HttpResponse<String> res = send("POST", "/api/v1/promotions", OPERATOR, body);
        assertEquals(201, res.statusCode(), res.body());
        return JSON.readTree(res.body());
    }

    private static String 새_지역() {
        return "T" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private String 객실_타입(String region, int maxOccupancy) throws Exception {
        HttpResponse<String> property = send("POST", "/api/v1/properties", HOST,
                "{\"name\":\"프로모션 테스트 스테이\",\"regionCode\":\"" + region
                        + "\",\"address\":\"서울특별시 중구 예시로 9\",\"description\":\"\"}");
        assertEquals(201, property.statusCode(), property.body());
        String propertyId = JSON.readTree(property.body()).get("id").asString();
        HttpResponse<String> roomType = send("POST",
                "/api/v1/properties/" + propertyId + "/room-types", HOST,
                "{\"name\":\"스탠다드\",\"maxOccupancy\":" + maxOccupancy + ",\"description\":\"\"}");
        assertEquals(201, roomType.statusCode(), roomType.body());
        return JSON.readTree(roomType.body()).get("id").asString();
    }

    private void 요금(String roomTypeId, LocalDate date, long amount) throws Exception {
        HttpResponse<String> res = send("POST", "/api/v1/room-types/" + roomTypeId + "/rates",
                HOST, "{\"date\":\"" + date + "\",\"amount\":" + amount + ",\"currency\":\"KRW\"}");
        assertEquals(201, res.statusCode(), res.body());
    }

    private String 적용_조회(String roomTypeId, LocalDate checkIn, LocalDate checkOut, int guests) {
        return "/api/v1/room-types/" + roomTypeId + "/applicable-promotions?checkIn=" + checkIn
                + "&checkOut=" + checkOut + "&guestCount=" + guests;
    }

    @Test
    void PROMO_01_등록하면_201과_Location과_열세_칸이다() throws Exception {
        LocalDate start = today().plusDays(100);
        HttpResponse<String> res = send("POST", "/api/v1/promotions", OPERATOR,
                "{\"name\":\"가을 할인\",\"discountRate\":10,\"campaignStartDate\":\"" + start
                        + "\",\"campaignEndDate\":\"" + start.plusDays(30)
                        + "\",\"stayStartDate\":\"" + start + "\",\"stayEndDate\":\""
                        + start.plusDays(60) + "\",\"minNights\":2,\"regionCodes\":[\"SEOUL\"]}");

        assertEquals(201, res.statusCode(), res.body());
        JsonNode body = JSON.readTree(res.body());
        assertEquals(13, body.size(), res.body());
        String id = body.get("id").asString();
        assertTrue(id.startsWith("promo_"), id);
        assertEquals("/api/v1/promotions/" + id, res.headers().firstValue("Location").orElse(""));
        assertEquals("가을 할인", body.get("name").asString());
        assertEquals(10, body.get("discountRate").asInt());
        assertEquals(start.toString(), body.get("campaignStartDate").asString());
        assertEquals(start.plusDays(30).toString(), body.get("campaignEndDate").asString());
        assertEquals(start.toString(), body.get("stayStartDate").asString());
        assertEquals(start.plusDays(60).toString(), body.get("stayEndDate").asString());
        assertEquals(2, body.get("minNights").asInt());
        assertEquals("SEOUL", body.get("regionCodes").get(0).asString());
        // enabled를 생략하면 true다. 11 PROMO-01 필드표
        assertTrue(body.get("enabled").asBoolean());
        assertEquals(0, body.get("version").asInt());
        assertTrue(body.get("createdAt").asString().matches(TIME_PATTERN), res.body());
        assertEquals(body.get("createdAt").asString(), body.get("updatedAt").asString());
    }

    @Test
    void PROMO_01_숙박_기간을_생략하면_두_칸이_null로_나간다() throws Exception {
        JsonNode body = 등록(먼_미래_본문("[]"));

        assertEquals(13, body.size(), body.toString());
        assertTrue(body.get("stayStartDate").isNull());
        assertTrue(body.get("stayEndDate").isNull());
        assertEquals(0, body.get("regionCodes").size());
    }

    @Test
    void PROMO_01_운영자가_아니면_403이고_헤더가_없으면_401이다() throws Exception {
        HttpResponse<String> host = send("POST", "/api/v1/promotions", HOST, 먼_미래_본문("[]"));
        HttpResponse<String> none = send("POST", "/api/v1/promotions", null, 먼_미래_본문("[]"));

        assertEquals(403, host.statusCode(), host.body());
        assertEquals("ACCESS_DENIED", JSON.readTree(host.body()).get("code").asString());
        assertEquals(401, none.statusCode(), none.body());
        assertEquals("ACTOR_REQUIRED", JSON.readTree(none.body()).get("code").asString());
    }

    @Test
    void PROMO_01_범위와_필수값_오류는_400_INVALID_REQUEST다() throws Exception {
        LocalDate start = today().plusDays(100);
        String 할인율_0 = "{\"name\":\"x\",\"discountRate\":0,\"campaignStartDate\":\"" + start
                + "\",\"campaignEndDate\":\"" + start.plusDays(30) + "\",\"minNights\":1,\"regionCodes\":[]}";
        String 숙박_기간_하나만 = "{\"name\":\"x\",\"discountRate\":10,\"campaignStartDate\":\"" + start
                + "\",\"campaignEndDate\":\"" + start.plusDays(30) + "\",\"stayStartDate\":\"" + start
                + "\",\"minNights\":1,\"regionCodes\":[]}";
        String 지역_중복 = "{\"name\":\"x\",\"discountRate\":10,\"campaignStartDate\":\"" + start
                + "\",\"campaignEndDate\":\"" + start.plusDays(30)
                + "\",\"minNights\":1,\"regionCodes\":[\"SEOUL\",\"SEOUL\"]}";
        String 모르는_필드 = "{\"name\":\"x\",\"discountRate\":10,\"campaignStartDate\":\"" + start
                + "\",\"campaignEndDate\":\"" + start.plusDays(30)
                + "\",\"minNights\":1,\"regionCodes\":[],\"couponCode\":\"A\"}";

        for (String body : new String[] {할인율_0, 숙박_기간_하나만, 지역_중복, 모르는_필드}) {
            HttpResponse<String> res = send("POST", "/api/v1/promotions", OPERATOR, body);
            assertEquals(400, res.statusCode(), res.body());
            assertEquals("INVALID_REQUEST", JSON.readTree(res.body()).get("code").asString(), body);
        }
    }

    @Test
    void PROMO_01_날짜_형식과_순서_오류는_400_INVALID_DATE_RANGE다() throws Exception {
        LocalDate start = today().plusDays(100);
        String 형식 = "{\"name\":\"x\",\"discountRate\":10,\"campaignStartDate\":\"2026/10/01\","
                + "\"campaignEndDate\":\"" + start.plusDays(30) + "\",\"minNights\":1,\"regionCodes\":[]}";
        String 순서 = "{\"name\":\"x\",\"discountRate\":10,\"campaignStartDate\":\"" + start.plusDays(30)
                + "\",\"campaignEndDate\":\"" + start + "\",\"minNights\":1,\"regionCodes\":[]}";
        String 같은날 = "{\"name\":\"x\",\"discountRate\":10,\"campaignStartDate\":\"" + start
                + "\",\"campaignEndDate\":\"" + start + "\",\"minNights\":1,\"regionCodes\":[]}";

        for (String body : new String[] {형식, 순서, 같은날}) {
            HttpResponse<String> res = send("POST", "/api/v1/promotions", OPERATOR, body);
            assertEquals(400, res.statusCode(), res.body());
            assertEquals("INVALID_DATE_RANGE", JSON.readTree(res.body()).get("code").asString(), body);
        }
    }

    @Test
    void V9_PROMO_02에서_지난_version이면_409이고_최근_변경이_남는다() throws Exception {
        String id = 등록(먼_미래_본문("[]")).get("id").asString();
        HttpResponse<String> first = send("PATCH", "/api/v1/promotions/" + id, OPERATOR,
                "{\"version\":0,\"enabled\":false}");
        assertEquals(200, first.statusCode(), first.body());
        assertEquals(1, JSON.readTree(first.body()).get("version").asInt());

        HttpResponse<String> stale = send("PATCH", "/api/v1/promotions/" + id, OPERATOR,
                "{\"version\":0,\"discountRate\":20}");

        assertEquals(409, stale.statusCode(), stale.body());
        assertEquals("VERSION_CONFLICT", JSON.readTree(stale.body()).get("code").asString());
        JsonNode current = JSON.readTree(send("GET", "/api/v1/promotions/" + id, OPERATOR, null).body());
        assertFalse(current.get("enabled").asBoolean());
        assertEquals(10, current.get("discountRate").asInt());
        assertEquals(1, current.get("version").asInt());
    }

    @Test
    void V10_PROMO_02가_생략한_필드를_유지하고_합친_최종_상태에서_날짜_순서를_본다() throws Exception {
        JsonNode created = 등록(먼_미래_본문("[\"SEOUL\"]"));
        String id = created.get("id").asString();
        LocalDate start = LocalDate.parse(created.get("campaignStartDate").asString());

        HttpResponse<String> 종료일만 = send("PATCH", "/api/v1/promotions/" + id, OPERATOR,
                "{\"version\":0,\"campaignEndDate\":\"" + start.minusDays(1) + "\"}");
        assertEquals(400, 종료일만.statusCode(), 종료일만.body());
        assertEquals("INVALID_DATE_RANGE", JSON.readTree(종료일만.body()).get("code").asString());

        HttpResponse<String> 둘다 = send("PATCH", "/api/v1/promotions/" + id, OPERATOR,
                "{\"version\":0,\"campaignStartDate\":\"" + start.minusDays(3)
                        + "\",\"campaignEndDate\":\"" + start.minusDays(1) + "\"}");
        assertEquals(200, 둘다.statusCode(), 둘다.body());
        JsonNode body = JSON.readTree(둘다.body());
        assertEquals(13, body.size(), 둘다.body());
        assertEquals(start.minusDays(3).toString(), body.get("campaignStartDate").asString());
        assertEquals(start.minusDays(1).toString(), body.get("campaignEndDate").asString());
        // 생략한 필드는 그대로다
        assertEquals("가을 할인", body.get("name").asString());
        assertEquals(10, body.get("discountRate").asInt());
        assertEquals(2, body.get("minNights").asInt());
        assertEquals("SEOUL", body.get("regionCodes").get(0).asString());
        assertEquals(1, body.get("version").asInt());
    }

    @Test
    void PROMO_02_숙박_기간은_둘_다_null로_해제하고_하나만_보내면_400이다() throws Exception {
        LocalDate start = today().plusDays(100);
        String id = 등록("{\"name\":\"가을 할인\",\"discountRate\":10,\"campaignStartDate\":\"" + start
                + "\",\"campaignEndDate\":\"" + start.plusDays(30) + "\",\"stayStartDate\":\"" + start
                + "\",\"stayEndDate\":\"" + start.plusDays(60)
                + "\",\"minNights\":2,\"regionCodes\":[]}").get("id").asString();

        HttpResponse<String> 해제 = send("PATCH", "/api/v1/promotions/" + id, OPERATOR,
                "{\"version\":0,\"stayStartDate\":null,\"stayEndDate\":null}");
        assertEquals(200, 해제.statusCode(), 해제.body());
        assertTrue(JSON.readTree(해제.body()).get("stayStartDate").isNull());
        assertTrue(JSON.readTree(해제.body()).get("stayEndDate").isNull());

        HttpResponse<String> 하나만 = send("PATCH", "/api/v1/promotions/" + id, OPERATOR,
                "{\"version\":1,\"stayStartDate\":\"" + start + "\"}");
        assertEquals(400, 하나만.statusCode(), 하나만.body());
        assertEquals("INVALID_REQUEST", JSON.readTree(하나만.body()).get("code").asString());

        HttpResponse<String> 교체 = send("PATCH", "/api/v1/promotions/" + id, OPERATOR,
                "{\"version\":1,\"stayStartDate\":\"" + start.plusDays(1) + "\",\"stayEndDate\":\""
                        + start.plusDays(10) + "\"}");
        assertEquals(200, 교체.statusCode(), 교체.body());
        assertEquals(start.plusDays(1).toString(),
                JSON.readTree(교체.body()).get("stayStartDate").asString());
        assertEquals(2, JSON.readTree(교체.body()).get("version").asInt());
    }

    @Test
    void PROMO_02_유지_필드에_명시적_null과_변경_없는_요청은_400이다() throws Exception {
        // 11 PROMO-02 필드표 아래 문장. null 허용 필드만 null로 해제할 수 있고 변경 필드가 하나 이상
        String id = 등록(먼_미래_본문("[]")).get("id").asString();

        HttpResponse<String> 이름_null = send("PATCH", "/api/v1/promotions/" + id, OPERATOR,
                "{\"version\":0,\"name\":null}");
        HttpResponse<String> 버전만 = send("PATCH", "/api/v1/promotions/" + id, OPERATOR,
                "{\"version\":0}");
        HttpResponse<String> 할인율_100 = send("PATCH", "/api/v1/promotions/" + id, OPERATOR,
                "{\"version\":0,\"discountRate\":100}");

        for (HttpResponse<String> res : List.of(이름_null, 버전만, 할인율_100)) {
            assertEquals(400, res.statusCode(), res.body());
            assertEquals("INVALID_REQUEST", JSON.readTree(res.body()).get("code").asString());
        }
        JsonNode current = JSON.readTree(send("GET", "/api/v1/promotions/" + id, OPERATOR, null).body());
        assertEquals(0, current.get("version").asInt());
    }

    @Test
    void PROMO_03_없는_ID는_404이고_있으면_열세_칸이다() throws Exception {
        String id = 등록(먼_미래_본문("[]")).get("id").asString();

        HttpResponse<String> found = send("GET", "/api/v1/promotions/" + id, OPERATOR, null);
        HttpResponse<String> missing = send("GET", "/api/v1/promotions/promo_none", OPERATOR, null);

        assertEquals(200, found.statusCode(), found.body());
        assertEquals(13, JSON.readTree(found.body()).size());
        assertEquals(id, JSON.readTree(found.body()).get("id").asString());
        assertEquals(404, missing.statusCode(), missing.body());
        assertEquals("RESOURCE_NOT_FOUND", JSON.readTree(missing.body()).get("code").asString());
    }

    @Test
    void PROMO_04_목록은_enabled로_거르고_쪽_구조_다섯_칸이다() throws Exception {
        String 사용 = 등록(먼_미래_본문("[]")).get("id").asString();
        String 미사용 = 등록(먼_미래_본문("[]")).get("id").asString();
        assertEquals(200, send("PATCH", "/api/v1/promotions/" + 미사용, OPERATOR,
                "{\"version\":0,\"enabled\":false}").statusCode());

        HttpResponse<String> 미사용만 = send("GET", "/api/v1/promotions?enabled=false&size=100",
                OPERATOR, null);
        HttpResponse<String> 전체 = send("GET", "/api/v1/promotions?size=100", OPERATOR, null);
        HttpResponse<String> 기본 = send("GET", "/api/v1/promotions", OPERATOR, null);

        assertEquals(200, 미사용만.statusCode(), 미사용만.body());
        JsonNode body = JSON.readTree(미사용만.body());
        assertEquals(5, body.size(), 미사용만.body());
        assertTrue(ids(body).contains(미사용));
        assertFalse(ids(body).contains(사용));
        assertTrue(ids(JSON.readTree(전체.body())).contains(사용));
        assertTrue(ids(JSON.readTree(전체.body())).contains(미사용));
        assertEquals(20, JSON.readTree(기본.body()).get("size").asInt());
        assertEquals(0, JSON.readTree(기본.body()).get("page").asInt());
    }

    @Test
    void PROMO_04_size가_101이면_400이고_100은_통과한다() throws Exception {
        HttpResponse<String> over = send("GET", "/api/v1/promotions?size=101", OPERATOR, null);
        HttpResponse<String> max = send("GET", "/api/v1/promotions?size=100", OPERATOR, null);

        assertEquals(400, over.statusCode(), over.body());
        assertEquals("INVALID_REQUEST", JSON.readTree(over.body()).get("code").asString());
        assertEquals(200, max.statusCode(), max.body());
    }

    @Test
    void PROMO_05_후보를_할인액_내림차순으로_내고_하나를_선택한다() throws Exception {
        String region = 새_지역();
        String roomTypeId = 객실_타입(region, 2);
        LocalDate checkIn = today().plusDays(5);
        요금(roomTypeId, checkIn, 100_000);
        요금(roomTypeId, checkIn.plusDays(1), 100_000);
        String 십 = 등록(오늘_본문(region, 10)).get("id").asString();
        String 이십 = 등록(오늘_본문(region, 20)).get("id").asString();

        HttpResponse<String> res = send("GET", 적용_조회(roomTypeId, checkIn, checkIn.plusDays(2), 2),
                null, null);

        assertEquals(200, res.statusCode(), res.body());
        JsonNode body = JSON.readTree(res.body());
        assertEquals(7, body.size(), res.body());
        assertEquals(roomTypeId, body.get("roomTypeId").asString());
        assertEquals(checkIn.toString(), body.get("checkIn").asString());
        assertEquals(checkIn.plusDays(2).toString(), body.get("checkOut").asString());
        assertEquals(2, body.get("guestCount").asInt());
        assertTrue(body.get("evaluatedAt").asString().matches(TIME_PATTERN), res.body());
        assertEquals(2, body.get("items").size());
        JsonNode first = body.get("items").get(0);
        assertEquals(5, first.size(), res.body());
        assertEquals(이십, first.get("id").asString());
        assertEquals(20, first.get("discountRate").asInt());
        assertEquals(40_000L, first.get("discountAmount").asLong());
        assertTrue(first.get("selected").asBoolean());
        assertEquals(십, body.get("items").get(1).get("id").asString());
        assertFalse(body.get("items").get(1).get("selected").asBoolean());
        assertEquals(이십, body.get("selectedPromotionId").asString());
    }

    @Test
    void V11_PROMO_05가_요금_없는_날짜에_409_RATE_NOT_CONFIGURED를_낸다() throws Exception {
        String region = 새_지역();
        String roomTypeId = 객실_타입(region, 2);
        LocalDate checkIn = today().plusDays(5);
        요금(roomTypeId, checkIn, 100_000);

        HttpResponse<String> res = send("GET", 적용_조회(roomTypeId, checkIn, checkIn.plusDays(2), 2),
                null, null);

        assertEquals(409, res.statusCode(), res.body());
        JsonNode body = JSON.readTree(res.body());
        assertEquals("RATE_NOT_CONFIGURED", body.get("code").asString());
        assertEquals(1, body.get("details").size(), res.body());
        assertTrue(body.get("details").get(0).get("reason").asString()
                .startsWith(checkIn.plusDays(1).toString()), res.body());
    }

    @Test
    void V12_PROMO_05가_인원_초과에_409를_내고_후보_없음은_빈_배열과_null이다() throws Exception {
        String region = 새_지역();
        String roomTypeId = 객실_타입(region, 2);
        LocalDate checkIn = today().plusDays(5);
        요금(roomTypeId, checkIn, 100_000);
        요금(roomTypeId, checkIn.plusDays(1), 100_000);

        HttpResponse<String> 초과 = send("GET", 적용_조회(roomTypeId, checkIn, checkIn.plusDays(2), 3),
                null, null);
        HttpResponse<String> 없음 = send("GET", 적용_조회(roomTypeId, checkIn, checkIn.plusDays(2), 2),
                null, null);

        assertEquals(409, 초과.statusCode(), 초과.body());
        assertEquals("OCCUPANCY_EXCEEDED", JSON.readTree(초과.body()).get("code").asString());
        assertEquals(200, 없음.statusCode(), 없음.body());
        JsonNode body = JSON.readTree(없음.body());
        assertEquals(7, body.size(), 없음.body());
        assertEquals(0, body.get("items").size());
        assertTrue(body.get("selectedPromotionId").isNull(), 없음.body());
    }

    @Test
    void PROMO_05_날짜_형식과_순서와_30박과_과거는_400_INVALID_DATE_RANGE다() throws Exception {
        String roomTypeId = 객실_타입(새_지역(), 2);
        LocalDate checkIn = today().plusDays(5);
        String[] paths = {
                "/api/v1/room-types/" + roomTypeId + "/applicable-promotions?checkIn=2026-13-01&checkOut="
                        + checkIn + "&guestCount=1",
                적용_조회(roomTypeId, checkIn, checkIn, 1),
                적용_조회(roomTypeId, checkIn, checkIn.plusDays(31), 1),
                적용_조회(roomTypeId, today().minusDays(1), today(), 1)};

        for (String path : paths) {
            HttpResponse<String> res = send("GET", path, null, null);
            assertEquals(400, res.statusCode(), path + " " + res.body());
            assertEquals("INVALID_DATE_RANGE", JSON.readTree(res.body()).get("code").asString(), path);
        }
    }

    @Test
    void PROMO_05_30박은_통과하고_guestCount_0은_400_INVALID_REQUEST다() throws Exception {
        String roomTypeId = 객실_타입(새_지역(), 2);
        LocalDate checkIn = today().plusDays(5);
        for (int i = 0; i < 30; i++) {
            요금(roomTypeId, checkIn.plusDays(i), 50_000);
        }

        HttpResponse<String> 삼십박 = send("GET", 적용_조회(roomTypeId, checkIn, checkIn.plusDays(30), 1),
                null, null);
        HttpResponse<String> 인원_0 = send("GET", 적용_조회(roomTypeId, checkIn, checkIn.plusDays(1), 0),
                null, null);

        assertEquals(200, 삼십박.statusCode(), 삼십박.body());
        assertEquals(400, 인원_0.statusCode(), 인원_0.body());
        assertEquals("INVALID_REQUEST", JSON.readTree(인원_0.body()).get("code").asString());
    }

    @Test
    void PROMO_05_없는_객실_타입은_404다() throws Exception {
        LocalDate checkIn = today().plusDays(5);

        HttpResponse<String> res = send("GET", 적용_조회("room_none", checkIn, checkIn.plusDays(1), 1),
                null, null);

        assertEquals(404, res.statusCode(), res.body());
        assertEquals("RESOURCE_NOT_FOUND", JSON.readTree(res.body()).get("code").asString());
    }

    private static List<String> ids(JsonNode page) {
        List<String> ids = new ArrayList<>();
        for (JsonNode item : page.get("items")) {
            ids.add(item.get("id").asString());
        }
        return ids;
    }
}
