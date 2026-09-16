package com.o2o.booking.api;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;

import com.o2o.shared.SeoulDate;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * K24와 K25. 설계 근거: task-S9-booking 8-1절, 11 BOOK-02(쿼리 표, 처리 규칙 본인 예약만과
 * createdAt 내림차순 동률이면 ID 내림차순), 11 BOOK-03(소유자만, serverNow), 11 공통 목록 규칙
 * (page 0 이상 기본 0, size 1부터 100 기본 20, 범위 밖 400), 11 인증과 접근 제어(남의 자원은 404),
 * T01의 예약 조회 구간, T02.
 *
 * BookingApiTest와 같은 방식이다. 서버가 실제 시계로 돌므로 날짜는 서울의 오늘에서 세고
 * 데이터를 되돌리지 않는다. 같은 손님의 예약이 앞 테스트에서 쌓이므로 목록 검사는 이 테스트가
 * 만든 id를 담고 있는지와 전체 정렬이 맞는지로 본다.
 *
 * 동률 정렬은 HTTP로 만들 수 없다. 두 요청의 createdAt이 마이크로초까지 같을 일이 없어서다.
 * 그래서 createdAt을 DB에서 같은 값으로 맞춘다(testing.md T6의 방식). 프로덕션에 없는 경로가
 * 아니라 같은 순간 두 요청이 커밋된 상태를 미리 놓는 것이다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BookingQueryApiTest {

    private static final String HOST = "host_001";
    private static final String GUEST = "guest_001";
    private static final String OTHER_GUEST = "guest_002";
    private static final String BOOKINGS = "/api/v1/bookings";
    private static final long RATE = 100_000L;
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).build();

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static LocalDate today() {
        return LocalDate.now(SeoulDate.ZONE);
    }

    private HttpResponse<String> send(String method, String path, String actorId,
                                      String idempotencyKey, String body) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + path))
                .timeout(Duration.ofSeconds(60));
        if (actorId != null) {
            b.header("X-Dev-Actor-Id", actorId);
        }
        if (idempotencyKey != null) {
            b.header("Idempotency-Key", idempotencyKey);
        }
        if (body == null) {
            b.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            b.header("Content-Type", "application/json");
            b.method(method, HttpRequest.BodyPublishers.ofString(body));
        }
        return client.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> get(String path, String actorId) throws Exception {
        return send("GET", path, actorId, null, null);
    }

    /** 최대 인원 2인 객실 타입 하나를 만들고 from부터 nights 밤을 재고 total과 요금 RATE로 연다 */
    private String roomTypeWith(LocalDate from, int nights, int total) throws Exception {
        HttpResponse<String> property = send("POST", "/api/v1/properties", HOST, null, """
                {"name":"예약 조회 스테이","regionCode":"SEOUL","address":"서울특별시 중구 예시로 6","description":""}
                """);
        assertEquals(201, property.statusCode(), property.body());
        String propertyId = JSON.readTree(property.body()).get("id").asString();
        HttpResponse<String> roomType = send("POST",
                "/api/v1/properties/" + propertyId + "/room-types", HOST, null,
                "{\"name\":\"스탠다드\",\"maxOccupancy\":2,\"description\":\"\"}");
        assertEquals(201, roomType.statusCode(), roomType.body());
        String roomTypeId = JSON.readTree(roomType.body()).get("id").asString();
        for (int i = 0; i < nights; i++) {
            LocalDate date = from.plusDays(i);
            assertEquals(201, send("POST", "/api/v1/room-types/" + roomTypeId + "/inventories", HOST,
                    null, "{\"date\":\"" + date + "\",\"totalCount\":" + total + "}").statusCode());
            assertEquals(201, send("POST", "/api/v1/room-types/" + roomTypeId + "/rates", HOST, null,
                    "{\"date\":\"" + date + "\",\"amount\":" + RATE + ",\"currency\":\"KRW\"}")
                    .statusCode());
        }
        return roomTypeId;
    }

    /** BOOK-01로 1박 예약 하나를 만들고 id를 돌려준다 */
    private String book(String guest, String roomTypeId, LocalDate checkIn) throws Exception {
        HttpResponse<String> res = send("POST", BOOKINGS, guest, "key-" + UUID.randomUUID(),
                "{\"roomTypeId\":\"" + roomTypeId + "\",\"checkIn\":\"" + checkIn
                        + "\",\"checkOut\":\"" + checkIn.plusDays(1)
                        + "\",\"guestCount\":1,\"expectedTotalAmount\":" + RATE
                        + ",\"currency\":\"KRW\"}");
        assertEquals(201, res.statusCode(), res.body());
        return JSON.readTree(res.body()).get("id").asString();
    }

    private static List<String> ids(JsonNode items) {
        List<String> ids = new ArrayList<>();
        items.forEach((item) -> ids.add(item.get("id").asString()));
        return ids;
    }

    private static void assertError(HttpResponse<String> res, int status, String code) {
        assertEquals(status, res.statusCode(), res.body());
        assertEquals(code, JSON.readTree(res.body()).get("code").asString(), res.body());
    }

    // ---------- K24 ----------

    @Test
    void K24_본인_예약만_createdAt_내림차순으로_낸다() throws Exception {
        LocalDate checkIn = today().plusDays(10);
        String roomTypeId = roomTypeWith(checkIn, 2, 3);
        String first = book(GUEST, roomTypeId, checkIn);
        String second = book(GUEST, roomTypeId, checkIn.plusDays(1));
        String others = book(OTHER_GUEST, roomTypeId, checkIn);

        HttpResponse<String> res = get(BOOKINGS + "?size=100", GUEST);

        assertEquals(200, res.statusCode(), res.body());
        JsonNode json = JSON.readTree(res.body());
        assertEquals(5, json.size(), res.body());
        List<String> ids = ids(json.get("items"));
        assertTrue(ids.contains(first) && ids.contains(second), res.body());
        assertFalse(ids.contains(others), "남의 예약이 섞였다");
        json.get("items").forEach((item) -> assertEquals(GUEST, item.get("guestId").asString()));
        // 나중 것이 먼저다. 목록 전체가 createdAt 내림차순이다
        assertTrue(ids.indexOf(second) < ids.indexOf(first), ids.toString());
        Instant previous = null;
        for (JsonNode item : json.get("items")) {
            Instant createdAt = Instant.parse(item.get("createdAt").asString());
            assertTrue(previous == null || !createdAt.isAfter(previous), "createdAt 내림차순이 아니다");
            previous = createdAt;
        }
        assertEquals(20, json.get("items").get(0).size(), "항목도 Booking 스무 필드다");
    }

    @Test
    void K24_createdAt이_같으면_id_내림차순이다() throws Exception {
        LocalDate checkIn = today().plusDays(10);
        String roomTypeId = roomTypeWith(checkIn, 2, 1);
        String a = book(GUEST, roomTypeId, checkIn);
        String b = book(GUEST, roomTypeId, checkIn.plusDays(1));
        // 같은 순간 커밋된 둘. 나중 것의 created_at을 문자열 그대로 읽어 앞 것에도 넣는다.
        // 자바 시각을 넣으면 드라이버의 시간대 변환이 끼어 저장된 값과 어긋날 수 있다
        String later = jdbcTemplate.queryForObject(
                "select created_at from booking where id = ?", String.class, b);
        jdbcTemplate.update("update booking set created_at = ? where id in (?, ?)", later, a, b);
        String larger = a.compareTo(b) > 0 ? a : b;
        String smaller = a.compareTo(b) > 0 ? b : a;

        List<String> ids = ids(JSON.readTree(get(BOOKINGS + "?size=100", GUEST).body()).get("items"));

        assertEquals(larger, ids.get(0), ids.toString());
        assertEquals(smaller, ids.get(1), ids.toString());
    }

    @Test
    void K24_status로_거르고_허용_밖_값은_400이다() throws Exception {
        LocalDate checkIn = today().plusDays(10);
        String roomTypeId = roomTypeWith(checkIn, 1, 1);
        String id = book(GUEST, roomTypeId, checkIn);

        JsonNode held = JSON.readTree(get(BOOKINGS + "?status=HELD&size=100", GUEST).body());
        assertTrue(ids(held.get("items")).contains(id));
        held.get("items").forEach((item) -> assertEquals("HELD", item.get("status").asString()));

        // 필터가 실제로 거른다는 짝. 1차는 CONFIRMED가 0건인 것으로 봤지만 2차의 결제 API 테스트가
        // 같은 손님의 CONFIRMED를 같은 DB에 남기므로 이 HELD가 그 목록에 없고 항목이 전부 CONFIRMED인
        // 것으로 본다(2026-09-13, task-S9-booking-lifecycle 6단계)
        JsonNode confirmed = JSON.readTree(get(BOOKINGS + "?status=CONFIRMED&size=100", GUEST).body());
        assertFalse(ids(confirmed.get("items")).contains(id));
        confirmed.get("items").forEach((item) -> assertEquals("CONFIRMED", item.get("status").asString()));

        assertError(get(BOOKINGS + "?status=BOGUS", GUEST), 400, "INVALID_REQUEST");
    }

    @Test
    void K24_page와_size의_기본값과_범위와_범위_밖_page가_맞다() throws Exception {
        LocalDate checkIn = today().plusDays(10);
        String roomTypeId = roomTypeWith(checkIn, 1, 1);
        book(GUEST, roomTypeId, checkIn);

        JsonNode defaults = JSON.readTree(get(BOOKINGS, GUEST).body());
        assertEquals(0, defaults.get("page").asInt());
        assertEquals(20, defaults.get("size").asInt());
        assertTrue(defaults.get("totalElements").asInt() >= 1);

        // 공통 목록 규칙. page 0 이상, size 1부터 100. 경계 양쪽(T2)
        assertEquals(200, get(BOOKINGS + "?page=0&size=1", GUEST).statusCode());
        assertEquals(200, get(BOOKINGS + "?size=100", GUEST).statusCode());
        assertError(get(BOOKINGS + "?page=-1", GUEST), 400, "INVALID_REQUEST");
        assertError(get(BOOKINGS + "?size=0", GUEST), 400, "INVALID_REQUEST");
        assertError(get(BOOKINGS + "?size=101", GUEST), 400, "INVALID_REQUEST");

        JsonNode beyond = JSON.readTree(get(BOOKINGS + "?page=9999", GUEST).body());
        assertEquals(0, beyond.get("items").size());
        assertEquals(9999, beyond.get("page").asInt());
        assertTrue(beyond.get("totalElements").asInt() >= 1);
    }

    @Test
    void K24_목록도_행위자를_본다() throws Exception {
        assertError(get(BOOKINGS, null), 401, "ACTOR_REQUIRED");
        assertError(get(BOOKINGS, HOST), 403, "ACCESS_DENIED");
    }

    // ---------- K25 ----------

    @Test
    void K25_본인_예약은_200과_serverNow와_함께_온다() throws Exception {
        LocalDate checkIn = today().plusDays(10);
        String roomTypeId = roomTypeWith(checkIn, 1, 1);
        String id = book(GUEST, roomTypeId, checkIn);

        HttpResponse<String> res = get(BOOKINGS + "/" + id, GUEST);

        assertEquals(200, res.statusCode(), res.body());
        JsonNode json = JSON.readTree(res.body());
        assertEquals(20, json.size(), res.body());
        assertEquals(id, json.get("id").asString());
        assertEquals(GUEST, json.get("guestId").asString());
        assertEquals("HELD", json.get("status").asString());
        Instant createdAt = Instant.parse(json.get("createdAt").asString());
        Instant serverNow = Instant.parse(json.get("serverNow").asString());
        Instant expiresAt = Instant.parse(json.get("expiresAt").asString());
        // 프론트가 expiresAt과 serverNow로 남은 시간을 센다(BOOK-03 처리 규칙). 조회 시각은
        // 생성 뒤이고 만료 전이다. 재전송 응답과 달리 serverNow가 조회마다 새로 난다
        assertFalse(serverNow.isBefore(createdAt));
        assertTrue(serverNow.isBefore(expiresAt));
        assertEquals(0, json.get("payment").get("attemptCount").asInt());
    }

    @Test
    void K25_남의_예약과_없는_id는_자원_정보_없이_404다() throws Exception {
        // T02. 남의 것과 없는 것이 같은 응답이라 존재 여부가 새지 않는다
        LocalDate checkIn = today().plusDays(10);
        String roomTypeId = roomTypeWith(checkIn, 1, 1);
        String id = book(GUEST, roomTypeId, checkIn);

        HttpResponse<String> others = get(BOOKINGS + "/" + id, OTHER_GUEST);
        HttpResponse<String> missing = get(BOOKINGS + "/booking_none", GUEST);

        assertError(others, 404, "RESOURCE_NOT_FOUND");
        assertError(missing, 404, "RESOURCE_NOT_FOUND");
        assertFalse(others.body().contains(id), others.body());
        assertEquals(JSON.readTree(others.body()).get("message"),
                JSON.readTree(missing.body()).get("message"));
    }

    @Test
    void K25_상세도_행위자를_본다() throws Exception {
        LocalDate checkIn = today().plusDays(10);
        String roomTypeId = roomTypeWith(checkIn, 1, 1);
        String id = book(GUEST, roomTypeId, checkIn);

        assertError(get(BOOKINGS + "/" + id, null), 401, "ACTOR_REQUIRED");
        assertError(get(BOOKINGS + "/" + id, HOST), 403, "ACCESS_DENIED");
    }
}
