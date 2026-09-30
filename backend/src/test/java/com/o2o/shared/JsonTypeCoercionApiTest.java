package com.o2o.shared;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import com.o2o.inventory.domain.DailyInventoryRepository;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 요청 body의 타입 강제 변환 거절. 설계 근거: 11 공통 요청과 응답 규칙(금액은 소수 없는 JSON
 * 정수, 수량과 인원은 정수, 잘못된 타입은 400), 11 에러 응답 표의 INVALID_REQUEST.
 *
 * Jackson 기본값은 정수 칸에 2.7을 넣으면 2로 자르고 "2"도 2로 읽는다. 그러면 명세상 타입
 * 오류인 body가 201이 되고, BOOK-01의 멱등 지문은 2와 2.7을 같은 body로 본다.
 *
 * 칸마다 대조군을 먼저 친다. 정수로 쓴 같은 body가 400이 아니어야 변형의 400이 타입 때문이라고
 * 말할 수 있다. 없는 자원을 치는 PATCH와 INTERNAL-01은 대조군이 404 같은 값이고, 변형이 400이면
 * body를 읽는 단계에서 막혔다는 뜻이다. 진짜 포트로 치는 이유는 CatalogApiTest와 같다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class JsonTypeCoercionApiTest {

    private static final String HOST = "host_001";
    private static final String GUEST = "guest_001";
    private static final String OPERATOR = "operator_001";
    private static final String MOCK = "mock_001";
    private static final long RATE = 100_000L;
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).build();

    @LocalServerPort
    private int port;

    @Autowired
    private DailyInventoryRepository inventoryRepository;

    /** 부를 때마다 다른 날짜. 변형이 통과했을 때 같은 날짜의 409로 가려지지 않게 한다 */
    private final AtomicInteger dayOffset = new AtomicInteger(30);

    private LocalDate nextDate() {
        return today().plusDays(dayOffset.getAndIncrement());
    }

    /** 정수 base를 정수가 아닌 JSON 넷으로 바꾼다. 소수, 정수 모양 소수, 문자열, 지수 표기 */
    private static List<String> notIntegers(long base) {
        return List.of(base + ".7", base + ".0", "\"" + base + "\"", base + "e0");
    }

    private static LocalDate today() {
        return LocalDate.now(SeoulDate.ZONE);
    }

    private HttpResponse<String> send(String method, String path, String actorId,
                                      String idempotencyKey, String body) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + path))
                .timeout(Duration.ofSeconds(60))
                .header("X-Dev-Actor-Id", actorId)
                .header("Content-Type", "application/json");
        if (idempotencyKey != null) {
            b.header("Idempotency-Key", idempotencyKey);
        }
        b.method(method, HttpRequest.BodyPublishers.ofString(body));
        return client.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> send(String method, String path, String actorId, String body)
            throws Exception {
        return send(method, path, actorId, null, body);
    }

    private String roomTypeOf() throws Exception {
        HttpResponse<String> property = send("POST", "/api/v1/properties", HOST,
                "{\"name\":\"타입 테스트 스테이\",\"regionCode\":\"SEOUL\","
                        + "\"address\":\"서울특별시 중구 예시로 5\",\"description\":\"\"}");
        assertEquals(201, property.statusCode(), property.body());
        String propertyId = JSON.readTree(property.body()).get("id").asString();
        HttpResponse<String> roomType = send("POST",
                "/api/v1/properties/" + propertyId + "/room-types", HOST,
                "{\"name\":\"스탠다드\",\"maxOccupancy\":2,\"description\":\"\"}");
        assertEquals(201, roomType.statusCode(), roomType.body());
        return JSON.readTree(roomType.body()).get("id").asString();
    }

    private String propertyOf() throws Exception {
        HttpResponse<String> property = send("POST", "/api/v1/properties", HOST,
                "{\"name\":\"타입 테스트 스테이\",\"regionCode\":\"SEOUL\","
                        + "\"address\":\"서울특별시 중구 예시로 5\",\"description\":\"\"}");
        assertEquals(201, property.statusCode(), property.body());
        return JSON.readTree(property.body()).get("id").asString();
    }

    /** 요청 하나. body는 검사할 칸의 JSON 값을 받아 본문을 만든다 */
    private interface Call {
        HttpResponse<String> send(String value) throws Exception;
    }

    /**
     * 대조군 값은 400이 아니고 변형 값은 전부 400 INVALID_REQUEST여야 한다. 변형이 새면
     * 한 번에 다 보이도록 모아서 실패시킨다.
     */
    private static void assertOnlyIntegerAccepted(String field, String control, List<String> variants,
                                                  Call call) throws Exception {
        HttpResponse<String> ok = call.send(control);
        assertNotEquals(400, ok.statusCode(), field + " 대조군 " + control + "이 400이다: " + ok.body());

        List<String> leaked = new ArrayList<>();
        for (String value : variants) {
            HttpResponse<String> res = call.send(value);
            if (res.statusCode() != 400 || !"INVALID_REQUEST".equals(code(res))) {
                leaked.add(field + "=" + value + " -> " + res.statusCode() + " " + res.body());
            }
        }
        assertTrue(leaked.isEmpty(), "타입이 틀린 값이 통과했다\n" + String.join("\n", leaked));
    }

    private static String code(HttpResponse<String> res) {
        try {
            JsonNode json = JSON.readTree(res.body());
            return json.hasNonNull("code") ? json.get("code").asString() : "";
        } catch (RuntimeException e) {
            return "";
        }
    }

    // ---------- BOOK-01 ----------

    private static String bookingBody(String roomTypeId, LocalDate checkIn, String guestCount,
                                      String total) {
        return "{\"roomTypeId\":\"" + roomTypeId + "\",\"checkIn\":\"" + checkIn
                + "\",\"checkOut\":\"" + checkIn.plusDays(2) + "\",\"guestCount\":" + guestCount
                + ",\"expectedTotalAmount\":" + total + ",\"currency\":\"KRW\"}";
    }

    /** 재고 total과 요금 RATE로 두 밤을 연 객실 타입 */
    private String bookableRoomType(LocalDate checkIn, int total) throws Exception {
        String roomTypeId = roomTypeOf();
        for (int i = 0; i < 2; i++) {
            LocalDate date = checkIn.plusDays(i);
            assertEquals(201, send("POST", "/api/v1/room-types/" + roomTypeId + "/inventories", HOST,
                    "{\"date\":\"" + date + "\",\"totalCount\":" + total + "}").statusCode());
            assertEquals(201, send("POST", "/api/v1/room-types/" + roomTypeId + "/rates", HOST,
                    "{\"date\":\"" + date + "\",\"amount\":" + RATE + ",\"currency\":\"KRW\"}")
                    .statusCode());
        }
        return roomTypeId;
    }

    @Test
    void BOOK_01_guestCount가_정수가_아니면_400이고_재고를_잡지_않는다() throws Exception {
        LocalDate checkIn = today().plusDays(20);
        String roomTypeId = bookableRoomType(checkIn, 20);

        assertOnlyIntegerAccepted("guestCount", "2", notIntegers(2), (v) -> send("POST",
                "/api/v1/bookings", GUEST, "key-" + UUID.randomUUID(),
                bookingBody(roomTypeId, checkIn, v, String.valueOf(RATE * 2))));

        // 대조군 한 건만 재고를 잡았다
        assertEquals(1, inventoryRepository.findByRoomTypeIdAndStayDate(RoomTypeId.of(roomTypeId), checkIn)
                .orElseThrow().heldCount());
    }

    @Test
    void BOOK_01_expectedTotalAmount가_정수가_아니면_400이다() throws Exception {
        LocalDate checkIn = today().plusDays(20);
        String roomTypeId = bookableRoomType(checkIn, 20);

        assertOnlyIntegerAccepted("expectedTotalAmount", String.valueOf(RATE * 2),
                notIntegers(RATE * 2), (v) -> send("POST", "/api/v1/bookings", GUEST,
                        "key-" + UUID.randomUUID(), bookingBody(roomTypeId, checkIn, "2", v)));
    }

    @Test
    void BOOK_01_같은_키에_guestCount만_2에서_2_7로_바꾸면_재생이_아니라_400이다() throws Exception {
        // 강제 변환이 살아 있으면 2.7이 2가 되어 지문이 같아지고 첫 응답이 재생된다
        LocalDate checkIn = today().plusDays(20);
        String roomTypeId = bookableRoomType(checkIn, 20);
        String key = "key-" + UUID.randomUUID();

        HttpResponse<String> first = send("POST", "/api/v1/bookings", GUEST, key,
                bookingBody(roomTypeId, checkIn, "2", String.valueOf(RATE * 2)));
        assertEquals(201, first.statusCode(), first.body());

        HttpResponse<String> second = send("POST", "/api/v1/bookings", GUEST, key,
                bookingBody(roomTypeId, checkIn, "2.7", String.valueOf(RATE * 2)));
        assertEquals(400, second.statusCode(), second.headers() + " " + second.body());
        assertEquals("INVALID_REQUEST", code(second), second.body());
    }

    // ---------- 카탈로그 ----------

    @Test
    void CAT_06_maxOccupancy가_정수가_아니면_400이다() throws Exception {
        String propertyId = propertyOf();
        assertOnlyIntegerAccepted("maxOccupancy", "2", notIntegers(2), (v) -> send("POST",
                "/api/v1/properties/" + propertyId + "/room-types", HOST,
                "{\"name\":\"스탠다드\",\"maxOccupancy\":" + v + ",\"description\":\"\"}"));
    }

    @Test
    void CAT_07_version과_maxOccupancy가_정수가_아니면_400이다() throws Exception {
        String path = "/api/v1/room-types/room_nothing";
        assertOnlyIntegerAccepted("version", "0", notIntegers(0), (v) -> send("PATCH", path, HOST,
                "{\"version\":" + v + ",\"maxOccupancy\":2}"));
        assertOnlyIntegerAccepted("maxOccupancy", "2", notIntegers(2), (v) -> send("PATCH", path,
                HOST, "{\"version\":0,\"maxOccupancy\":" + v + "}"));
    }

    @Test
    void CAT_03_version이_정수가_아니면_400이다() throws Exception {
        assertOnlyIntegerAccepted("version", "0", notIntegers(0), (v) -> send("PATCH",
                "/api/v1/properties/prop_nothing", HOST, "{\"version\":" + v + ",\"name\":\"x\"}"));
    }

    @Test
    void CAT_01_문자열_칸에_숫자를_보내면_400이다() throws Exception {
        assertOnlyIntegerAccepted("name", "\"123\"", List.of("123", "1.5", "true"), (v) -> send(
                "POST", "/api/v1/properties", HOST, "{\"name\":" + v + ",\"regionCode\":\"SEOUL\","
                        + "\"address\":\"서울특별시 중구 예시로 5\",\"description\":\"\"}"));
    }

    // ---------- 재고와 요금 ----------

    private static String bulkBody(LocalDate from, String totalCount) {
        return "{\"from\":\"" + from.plusDays(400) + "\",\"to\":\"" + from.plusDays(401)
                + "\",\"totalCount\":" + totalCount + "}";
    }

    @Test
    void INV_재고_등록과_일괄_등록의_totalCount가_정수가_아니면_400이다() throws Exception {
        String roomTypeId = roomTypeOf();
        String base = "/api/v1/room-types/" + roomTypeId + "/inventories";
        assertOnlyIntegerAccepted("totalCount", "5", notIntegers(5), (v) -> send("POST", base, HOST,
                "{\"date\":\"" + nextDate() + "\",\"totalCount\":" + v + "}"));
        assertOnlyIntegerAccepted("totalCount", "5", notIntegers(5), (v) -> send("POST",
                base + "/bulk", HOST, bulkBody(nextDate(), v)));
    }

    @Test
    void INV_재고_조정의_version과_totalCount가_정수가_아니면_400이다() throws Exception {
        String path = "/api/v1/room-types/room_nothing/inventories/" + today().plusDays(10);
        assertOnlyIntegerAccepted("version", "0", notIntegers(0), (v) -> send("PATCH", path, HOST,
                "{\"version\":" + v + ",\"totalCount\":5}"));
        assertOnlyIntegerAccepted("totalCount", "5", notIntegers(5), (v) -> send("PATCH", path, HOST,
                "{\"version\":0,\"totalCount\":" + v + "}"));
    }

    @Test
    void RATE_요금_등록과_조정의_amount와_version이_정수가_아니면_400이다() throws Exception {
        String roomTypeId = roomTypeOf();
        assertOnlyIntegerAccepted("amount", String.valueOf(RATE), notIntegers(RATE), (v) -> send(
                "POST", "/api/v1/room-types/" + roomTypeId + "/rates", HOST,
                "{\"date\":\"" + nextDate() + "\",\"amount\":" + v + ",\"currency\":\"KRW\"}"));

        String path = "/api/v1/room-types/room_nothing/rates/" + today().plusDays(10);
        assertOnlyIntegerAccepted("version", "0", notIntegers(0), (v) -> send("PATCH", path, HOST,
                "{\"version\":" + v + ",\"amount\":" + RATE + "}"));
        assertOnlyIntegerAccepted("amount", String.valueOf(RATE), notIntegers(RATE), (v) -> send(
                "PATCH", path, HOST, "{\"version\":0,\"amount\":" + v + "}"));
    }

    // ---------- 프로모션 ----------

    private static String promotionBody(String discountRate, String minNights, String enabled) {
        LocalDate start = today().plusDays(100);
        return "{\"name\":\"타입 테스트 할인\",\"discountRate\":" + discountRate
                + ",\"campaignStartDate\":\"" + start + "\",\"campaignEndDate\":\"" + start.plusDays(30)
                + "\",\"minNights\":" + minNights + ",\"regionCodes\":[\"SEOUL\"],\"enabled\":" + enabled
                + "}";
    }

    @Test
    void PROMO_01_discountRate와_minNights와_enabled의_타입이_틀리면_400이다() throws Exception {
        String path = "/api/v1/promotions";
        assertOnlyIntegerAccepted("discountRate", "10", notIntegers(10), (v) -> send("POST", path,
                OPERATOR, promotionBody(v, "2", "false")));
        assertOnlyIntegerAccepted("minNights", "2", notIntegers(2), (v) -> send("POST", path,
                OPERATOR, promotionBody("10", v, "false")));
        assertOnlyIntegerAccepted("enabled", "false", List.of("\"false\"", "0"), (v) -> send("POST",
                path, OPERATOR, promotionBody("10", "2", v)));
    }

    @Test
    void PROMO_02_version과_discountRate가_정수가_아니면_400이다() throws Exception {
        String path = "/api/v1/promotions/promo_nothing";
        assertOnlyIntegerAccepted("version", "0", notIntegers(0), (v) -> send("PATCH", path, OPERATOR,
                "{\"version\":" + v + ",\"discountRate\":10}"));
        assertOnlyIntegerAccepted("discountRate", "10", notIntegers(10), (v) -> send("PATCH", path,
                OPERATOR, "{\"version\":0,\"discountRate\":" + v + "}"));
    }

    // ---------- INTERNAL-01 ----------

    @Test
    void INTERNAL_01_amount가_정수가_아니면_400이다() throws Exception {
        assertOnlyIntegerAccepted("amount", String.valueOf(RATE), notIntegers(RATE), (v) -> send(
                "POST", "/internal/mock-payments/events", MOCK,
                "{\"eventId\":\"mock_event_" + UUID.randomUUID().toString().replace("-", "")
                        + "\",\"paymentAttemptId\":\"pay_nothing\",\"pgTransactionId\":\"pg_nothing\","
                        + "\"outcome\":\"APPROVED\",\"amount\":" + v + ",\"currency\":\"KRW\"}"));
    }
}
