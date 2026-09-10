package com.o2o.inventory.api;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 8-1절 V10과 V11, 그리고 V9의 조회 반쪽. INV-04와 INV-05와 RATE-03과 RATE-04.
 * 설계 근거: 11 재고 절과 요금 절의 조회 넷, 11 응답 모델 InventoryRange와 RateRange, T07.
 *
 * 쓰기 API 테스트와 클래스를 가른 이유는 확인하는 규칙이 다르기 때문이다. 쓰기는 거절 조건이
 * 중심이고 조회는 없는 날짜를 어떻게 답하느냐가 중심이다. 카탈로그에서 등록과 목록을 두
 * 클래스로 가른 것과 같은 결이다.
 *
 * 없는 날짜를 0으로 채워 내보내지 않는 것이 이 클래스의 핵심이다. 재고 0과 재고 미등록은
 * 다르다. 앞은 판매할 방이 없는 날이고 뒤는 판매 계획을 세우지 않은 날이다. T07이 숙박
 * 기간 중 한 날짜에 재고나 요금이 없을 때를 따로 보는 이유가 그것이다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class InventoryQueryApiTest {

    private static final String HOST = "host_001";
    private static final String OTHER_HOST = "host_002";
    private static final String GUEST = "guest_001";
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).build();

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static LocalDate today() {
        return LocalDate.now(ZoneOffset.UTC);
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

    private String roomTypeOf(String host) throws Exception {
        String propertyBody = """
                {"name":"조회 테스트 스테이","regionCode":"SEOUL","address":"서울특별시 중구 예시로 7","description":""}
                """;
        HttpResponse<String> property = send("POST", "/api/v1/properties", host, propertyBody);
        assertEquals(201, property.statusCode(), property.body());
        String propertyId = JSON.readTree(property.body()).get("id").asString();

        String roomTypeBody = """
                {"name":"스탠다드","maxOccupancy":2,"description":""}
                """;
        HttpResponse<String> roomType = send(
                "POST", "/api/v1/properties/" + propertyId + "/room-types", host, roomTypeBody);
        assertEquals(201, roomType.statusCode(), roomType.body());
        return JSON.readTree(roomType.body()).get("id").asString();
    }

    private void 재고를_등록한다(String roomTypeId, LocalDate date, int total) throws Exception {
        HttpResponse<String> res = send("POST",
                "/api/v1/room-types/" + roomTypeId + "/inventories", HOST,
                "{\"date\":\"" + date + "\",\"totalCount\":" + total + "}");
        assertEquals(201, res.statusCode(), res.body());
    }

    private void 요금을_등록한다(String roomTypeId, LocalDate date, long amount) throws Exception {
        HttpResponse<String> res = send("POST", "/api/v1/room-types/" + roomTypeId + "/rates",
                HOST, "{\"date\":\"" + date + "\",\"amount\":" + amount + ",\"currency\":\"KRW\"}");
        assertEquals(201, res.statusCode(), res.body());
    }

    /**
     * 과거 날짜 재고 행을 직접 놓는다. 쓰기 API가 과거 날짜를 막으므로 만들 길이 없다.
     * 조회는 과거를 허용하는데 그 허용을 확인하려면 과거 행이 있어야 한다. 상태를 DB에
     * 직접 놓는 근거는 InventoryApplicationServiceTest 머리 주석과 같다.
     */
    private void 과거_재고를_직접_놓는다(String roomTypeId, LocalDate date, int total) {
        Timestamp now = Timestamp.from(Instant.now());
        jdbcTemplate.update(
                "insert into daily_inventory (id, room_type_id, stay_date, total_count, "
                        + "sold_count, held_count, version, created_at, updated_at) "
                        + "values (?, ?, ?, ?, 0, 0, 0, ?, ?)",
                "inv_" + UUID.randomUUID().toString().replace("-", ""),
                roomTypeId, Date.valueOf(date), total, now, now);
    }

    @Test
    void INV_04_기간을_조회하면_200이고_날짜_오름차순이다() throws Exception {
        String roomTypeId = roomTypeOf(HOST);
        LocalDate from = today().plusDays(10);
        // 일부러 뒤 날짜를 먼저 넣는다. 정렬이 등록 순서가 아니라 날짜 순서여야 한다
        재고를_등록한다(roomTypeId, from.plusDays(2), 7);
        재고를_등록한다(roomTypeId, from, 5);
        재고를_등록한다(roomTypeId, from.plusDays(1), 6);

        HttpResponse<String> res = send("GET", "/api/v1/room-types/" + roomTypeId
                + "/inventories?from=" + from + "&to=" + from.plusDays(3), HOST, null);

        assertEquals(200, res.statusCode(), res.body());
        JsonNode body = JSON.readTree(res.body());
        assertEquals(5, body.size(), res.body());
        assertEquals(3, body.get("items").size());
        assertEquals(from.toString(), body.get("items").get(0).get("date").asString());
        assertEquals(from.plusDays(1).toString(), body.get("items").get(1).get("date").asString());
        assertEquals(from.plusDays(2).toString(), body.get("items").get(2).get("date").asString());
        // 다 있으면 빈 배열이다. V10의 통과 쪽 짝이다
        assertEquals(0, body.get("missingDates").size());
    }

    @Test
    void V10_없는_날짜는_items가_아니라_missingDates에_담긴다() throws Exception {
        // 11 INV-04의 처리 규칙. 없는 날짜는 items에 만들지 않고 missingDates에 적는다
        String roomTypeId = roomTypeOf(HOST);
        LocalDate from = today().plusDays(10);
        재고를_등록한다(roomTypeId, from, 5);
        재고를_등록한다(roomTypeId, from.plusDays(2), 7);

        HttpResponse<String> res = send("GET", "/api/v1/room-types/" + roomTypeId
                + "/inventories?from=" + from + "&to=" + from.plusDays(4), HOST, null);

        assertEquals(200, res.statusCode(), res.body());
        JsonNode body = JSON.readTree(res.body());
        // 있는 것 둘, 없는 것 둘. 없는 것을 0으로 채우면 items가 넷이 된다
        assertEquals(2, body.get("items").size(), res.body());
        assertEquals(2, body.get("missingDates").size(), res.body());
        assertEquals(from.plusDays(1).toString(), body.get("missingDates").get(0).asString());
        assertEquals(from.plusDays(3).toString(), body.get("missingDates").get(1).asString());
    }

    @Test
    void V10_요금도_없는_날짜가_missingDates에_담긴다() throws Exception {
        String roomTypeId = roomTypeOf(HOST);
        LocalDate from = today().plusDays(10);
        요금을_등록한다(roomTypeId, from.plusDays(1), 100_000);

        HttpResponse<String> res = send("GET", "/api/v1/room-types/" + roomTypeId
                + "/rates?from=" + from + "&to=" + from.plusDays(3), HOST, null);

        assertEquals(200, res.statusCode(), res.body());
        JsonNode body = JSON.readTree(res.body());
        assertEquals(1, body.get("items").size(), res.body());
        assertEquals(100_000L, body.get("items").get(0).get("amount").asLong());
        assertEquals("KRW", body.get("items").get(0).get("currency").asString());
        assertEquals(2, body.get("missingDates").size(), res.body());
    }

    @Test
    void INV_04_아무것도_없으면_items가_비고_전부_missingDates다() throws Exception {
        // 빈 결과가 404가 아니라 200이다. 기간 조회는 자원이 기간이지 날짜가 아니다
        String roomTypeId = roomTypeOf(HOST);
        LocalDate from = today().plusDays(10);

        HttpResponse<String> res = send("GET", "/api/v1/room-types/" + roomTypeId
                + "/inventories?from=" + from + "&to=" + from.plusDays(3), HOST, null);

        assertEquals(200, res.statusCode(), res.body());
        JsonNode body = JSON.readTree(res.body());
        assertEquals(0, body.get("items").size());
        assertEquals(3, body.get("missingDates").size());
    }

    @Test
    void INV_05_날짜_단건을_조회하면_200이고_일곱_필드다() throws Exception {
        String roomTypeId = roomTypeOf(HOST);
        LocalDate date = today().plusDays(10);
        재고를_등록한다(roomTypeId, date, 5);

        HttpResponse<String> res = send("GET",
                "/api/v1/room-types/" + roomTypeId + "/inventories/" + date, HOST, null);

        assertEquals(200, res.statusCode(), res.body());
        JsonNode body = JSON.readTree(res.body());
        assertEquals(7, body.size(), res.body());
        assertEquals(5, body.get("totalCount").asInt());
        assertEquals(5, body.get("availableCount").asInt());
        assertEquals(date.toString(), body.get("date").asString());
    }

    @Test
    void V11_없는_날짜_단건_조회는_404다() throws Exception {
        // 11 INV-05의 처리 규칙. 기간 조회의 missingDates와 다른 답이다
        String roomTypeId = roomTypeOf(HOST);

        HttpResponse<String> res = send("GET", "/api/v1/room-types/" + roomTypeId
                + "/inventories/" + today().plusDays(10), HOST, null);

        assertEquals(404, res.statusCode(), res.body());
        assertTrue(res.body().contains("RESOURCE_NOT_FOUND"), res.body());
    }

    @Test
    void RATE_04_요금_단건은_200이고_없으면_404다() throws Exception {
        String roomTypeId = roomTypeOf(HOST);
        LocalDate date = today().plusDays(10);
        요금을_등록한다(roomTypeId, date, 100_000);

        HttpResponse<String> found = send("GET",
                "/api/v1/room-types/" + roomTypeId + "/rates/" + date, HOST, null);
        assertEquals(200, found.statusCode(), found.body());
        JsonNode body = JSON.readTree(found.body());
        assertEquals(5, body.size(), found.body());
        assertEquals(100_000L, body.get("amount").asLong());

        HttpResponse<String> missing = send("GET",
                "/api/v1/room-types/" + roomTypeId + "/rates/" + date.plusDays(1), HOST, null);
        assertEquals(404, missing.statusCode(), missing.body());
        assertTrue(missing.body().contains("RESOURCE_NOT_FOUND"), missing.body());
    }

    @Test
    void V9_과거_기간_조회는_허용된다() throws Exception {
        // V9의 나머지 반쪽. 등록과 수정은 400이었고 조회는 400이 아니어야 한다.
        // 11 INV-04와 INV-05의 처리 규칙이 과거 조회를 허용한다고 적는다
        String roomTypeId = roomTypeOf(HOST);
        LocalDate past = today().minusDays(5);
        과거_재고를_직접_놓는다(roomTypeId, past, 4);

        HttpResponse<String> range = send("GET", "/api/v1/room-types/" + roomTypeId
                + "/inventories?from=" + past + "&to=" + past.plusDays(2), HOST, null);
        assertEquals(200, range.statusCode(), range.body());
        JsonNode body = JSON.readTree(range.body());
        assertEquals(1, body.get("items").size(), range.body());
        assertEquals(4, body.get("items").get(0).get("totalCount").asInt());

        HttpResponse<String> single = send("GET",
                "/api/v1/room-types/" + roomTypeId + "/inventories/" + past, HOST, null);
        assertEquals(200, single.statusCode(), single.body());
    }

    @Test
    void 조회도_366일_상한에_걸린다() throws Exception {
        // 과거 조건만 빠지고 순서와 상한은 쓰기와 같다
        String roomTypeId = roomTypeOf(HOST);
        LocalDate from = today();

        HttpResponse<String> tooLong = send("GET", "/api/v1/room-types/" + roomTypeId
                + "/inventories?from=" + from + "&to=" + from.plusDays(367), HOST, null);
        assertEquals(400, tooLong.statusCode(), tooLong.body());
        assertTrue(tooLong.body().contains("INVALID_DATE_RANGE"), tooLong.body());

        HttpResponse<String> exact = send("GET", "/api/v1/room-types/" + roomTypeId
                + "/inventories?from=" + from + "&to=" + from.plusDays(366), HOST, null);
        assertEquals(200, exact.statusCode(), exact.body());
        assertEquals(366, JSON.readTree(exact.body()).get("missingDates").size());
    }

    @Test
    void 조회에서_from이_to보다_뒤면_400이다() throws Exception {
        String roomTypeId = roomTypeOf(HOST);
        LocalDate from = today().plusDays(10);

        HttpResponse<String> res = send("GET", "/api/v1/room-types/" + roomTypeId
                + "/inventories?from=" + from + "&to=" + from, HOST, null);

        assertEquals(400, res.statusCode(), res.body());
        assertTrue(res.body().contains("INVALID_DATE_RANGE"), res.body());
    }

    @Test
    void 남의_객실_타입은_조회도_404다() throws Exception {
        // T02. 조회 넷도 대상 숙소의 소유자만 볼 수 있다. 쓰기와 같은 규칙이다
        String otherRoomTypeId = roomTypeOf(OTHER_HOST);
        LocalDate from = today().plusDays(10);

        HttpResponse<String> range = send("GET", "/api/v1/room-types/" + otherRoomTypeId
                + "/inventories?from=" + from + "&to=" + from.plusDays(3), HOST, null);
        assertEquals(404, range.statusCode(), range.body());

        HttpResponse<String> single = send("GET", "/api/v1/room-types/" + otherRoomTypeId
                + "/rates/" + from, HOST, null);
        assertEquals(404, single.statusCode(), single.body());
    }

    @Test
    void 조회도_행위자가_없으면_401이고_GUEST면_403이다() throws Exception {
        String roomTypeId = roomTypeOf(HOST);
        String path = "/api/v1/room-types/" + roomTypeId + "/inventories/" + today().plusDays(10);

        HttpResponse<String> anonymous = send("GET", path, null, null);
        assertEquals(401, anonymous.statusCode(), anonymous.body());
        assertTrue(anonymous.body().contains("ACTOR_REQUIRED"), anonymous.body());

        HttpResponse<String> guest = send("GET", path, GUEST, null);
        assertEquals(403, guest.statusCode(), guest.body());
        assertTrue(guest.body().contains("ACCESS_DENIED"), guest.body());
    }
}
