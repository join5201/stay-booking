package com.o2o.booking.api;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.o2o.booking.domain.BookingId;
import com.o2o.booking.domain.BookingRepository;
import com.o2o.inventory.domain.DailyInventory;
import com.o2o.inventory.domain.DailyInventoryRepository;
import com.o2o.shared.RoomTypeId;
import com.o2o.shared.SeoulDate;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * L19부터 L22. 설계 근거: task-S9-booking-lifecycle 8-1절과 2절 BOOK-04 검사 순서 표(7행부터
 * 14행), 11 BOOK-04(요청 표, 200 Booking, 에러 표, 처리 규칙), 11 응답 모델 Booking과
 * PaymentSummary와 Refund, 11 멱등 처리 절, 11 인증과 접근 제어, P05, T02, T22, T24, T25, T29,
 * 08-3 11-6.
 *
 * BookingPaymentApiTest와 같은 방식이다. 진짜 포트를 열고 JDK HttpClient로 친다. 서버가 실제
 * 시계로 돌므로 날짜는 서울의 오늘에서 센다(testing.md T4, T5). 데이터를 되돌리지 않아 테스트마다
 * 숙소와 객실 타입과 멱등키를 새로 만든다. CONFIRMED는 PAY-01 APPROVE로 만든다. 202가 돌아온
 * 순간 커밋 뒤 처리가 끝나 있어 바로 취소할 수 있다.
 *
 * 취소 가능 기간 밖은 체크인이 서울 오늘인 CONFIRMED로 본다. 계약 8-1 L20은 DB에 놓으라
 * 적었지만(T6) BOOK-01이 오늘 체크인을 받고 재고와 요금도 오늘을 열 수 있어 HTTP만으로 그 상태가
 * 된다. 실제 길로 만들 수 있으면 그 길이 낫다. 자정 직전에 돌리면 날짜가 넘어가 BOOK-01이 먼저
 * 400을 낼 수 있다. 1차 테스트가 같은 노출을 갖는다(T4). 만료 시각이 지난 HELD는
 * BookingPaymentApiTest와 같이 expires_at을 당겨 놓는다(T6).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BookingCancelApiTest {

    private static final String HOST = "host_001";
    private static final String GUEST = "guest_001";
    private static final String OTHER_GUEST = "guest_002";
    private static final String MOCK = "mock_001";
    private static final String BOOKINGS = "/api/v1/bookings";
    private static final String EVENTS = "/internal/mock-payments/events";
    private static final long RATE = 100_000L;
    private static final String REASON_BODY = "{\"reason\":\"일정 변경\"}";
    private static final String TIME_PATTERN = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z";
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).build();

    @LocalServerPort
    private int port;

    @Autowired
    private DailyInventoryRepository inventoryRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    /**
     * 예약 하나. 만든 객실 타입과 날짜와 스냅샷 총액에 더해 BOOK-01의 키와 body와 최초 응답을
     * 든다. T29가 그 키로 재전송한다
     */
    private record Held(String bookingId, String roomTypeId, LocalDate checkIn, int nights, long total,
                        String key, String requestBody, String firstResponse) {
        String path() {
            return BOOKINGS + "/" + bookingId;
        }

        String attemptsPath() {
            return path() + "/payment-attempts";
        }

        String cancelPath() {
            return path() + "/cancellations";
        }
    }

    private static LocalDate today() {
        return LocalDate.now(SeoulDate.ZONE);
    }

    private static String newKey() {
        return "key-" + UUID.randomUUID();
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

    private HttpResponse<String> send(String method, String path, String actorId, String body)
            throws Exception {
        return send(method, path, actorId, null, body);
    }

    private HttpResponse<String> get(String path, String actorId) throws Exception {
        return send("GET", path, actorId, null);
    }

    /** PAY-01. 새 키 */
    private HttpResponse<String> pay(Held held, String mockMode) throws Exception {
        return send("POST", held.attemptsPath(), GUEST, newKey(), "{\"mockMode\":\"" + mockMode + "\"}");
    }

    /** BOOK-04. 손님과 키와 body */
    private HttpResponse<String> cancel(String guest, Held held, String key, String body) throws Exception {
        return send("POST", held.cancelPath(), guest, key, body);
    }

    private HttpResponse<String> cancel(Held held) throws Exception {
        return cancel(GUEST, held, newKey(), REASON_BODY);
    }

    private static String reasonBody(String reason) {
        return JSON.writeValueAsString(Map.of("reason", reason));
    }

    private String roomTypeOf(String host) throws Exception {
        String propertyBody = """
                {"name":"취소 테스트 스테이","regionCode":"SEOUL","address":"서울특별시 중구 예시로 7","description":""}
                """;
        HttpResponse<String> property = send("POST", "/api/v1/properties", host, propertyBody);
        assertEquals(201, property.statusCode(), property.body());
        String propertyId = JSON.readTree(property.body()).get("id").asString();

        HttpResponse<String> roomType = send("POST",
                "/api/v1/properties/" + propertyId + "/room-types", host,
                "{\"name\":\"스탠다드\",\"maxOccupancy\":2,\"description\":\"\"}");
        assertEquals(201, roomType.statusCode(), roomType.body());
        return JSON.readTree(roomType.body()).get("id").asString();
    }

    private void prepare(String roomTypeId, LocalDate from, int nights, int total) throws Exception {
        for (int i = 0; i < nights; i++) {
            LocalDate date = from.plusDays(i);
            HttpResponse<String> inventory = send("POST",
                    "/api/v1/room-types/" + roomTypeId + "/inventories", HOST,
                    "{\"date\":\"" + date + "\",\"totalCount\":" + total + "}");
            assertEquals(201, inventory.statusCode(), inventory.body());
            HttpResponse<String> rate = send("POST", "/api/v1/room-types/" + roomTypeId + "/rates", HOST,
                    "{\"date\":\"" + date + "\",\"amount\":" + RATE + ",\"currency\":\"KRW\"}");
            assertEquals(201, rate.statusCode(), rate.body());
        }
    }

    /** 새 객실 타입에 재고 2를 열고 BOOK-01로 HELD 하나를 만든다 */
    private Held held(int nights, LocalDate checkIn) throws Exception {
        String roomTypeId = roomTypeOf(HOST);
        prepare(roomTypeId, checkIn, nights, 2);
        String key = newKey();
        String body = "{\"roomTypeId\":\"" + roomTypeId + "\",\"checkIn\":\"" + checkIn
                + "\",\"checkOut\":\"" + checkIn.plusDays(nights) + "\",\"guestCount\":1"
                + ",\"expectedTotalAmount\":" + RATE * nights + ",\"currency\":\"KRW\"}";
        HttpResponse<String> res = send("POST", BOOKINGS, GUEST, key, body);
        assertEquals(201, res.statusCode(), res.body());
        return new Held(JSON.readTree(res.body()).get("id").asString(), roomTypeId, checkIn, nights,
                RATE * nights, key, body, res.body());
    }

    private Held held(int nights) throws Exception {
        return held(nights, today().plusDays(10));
    }

    /** HELD를 PAY-01 APPROVE로 CONFIRMED까지. 응답이 온 순간 확정과 판매 전환이 끝나 있다 */
    private Held confirmed(int nights, LocalDate checkIn) throws Exception {
        Held held = held(nights, checkIn);
        HttpResponse<String> res = pay(held, "APPROVE");
        assertEquals(202, res.statusCode(), res.body());
        assertEquals("CONFIRMED", detail(held).get("status").asString());
        return held;
    }

    private Held confirmed(int nights) throws Exception {
        return confirmed(nights, today().plusDays(10));
    }

    /** T6. 만료 시각을 생성 시각으로 당기고 PAY-01의 선만료로 EXPIRED를 저장시킨다(7절 D-2 가) */
    private void expire(Held held) throws Exception {
        assertEquals(1, jdbcTemplate.update("update booking set expires_at = created_at where id = ?",
                held.bookingId()));
        assertError(pay(held, "APPROVE"), 409, "BOOKING_EXPIRED");
        assertEquals("EXPIRED", detail(held).get("status").asString());
    }

    /** INTERNAL-01 승인 body. 11 요청 표의 필수 여섯을 202 body의 시도에서 옮긴다. 두 번 보내려고 문자열로 든다 */
    private static String approvalEvent(JsonNode attempt) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("eventId", "mock_event_" + UUID.randomUUID().toString().replace("-", ""));
        body.put("paymentAttemptId", attempt.get("id").asString());
        body.put("pgTransactionId", attempt.get("pgTransactionId").asString());
        body.put("outcome", "APPROVED");
        body.put("amount", attempt.get("amount").asLong());
        body.put("currency", attempt.get("currency").asString());
        return JSON.writeValueAsString(body);
    }

    private HttpResponse<String> sendEvent(String body) throws Exception {
        return send("POST", EVENTS, MOCK, body);
    }

    private DailyInventory inventory(Held held, int dayOffset) {
        return inventoryRepository.findByRoomTypeIdAndStayDate(RoomTypeId.of(held.roomTypeId()),
                held.checkIn().plusDays(dayOffset)).orElseThrow();
    }

    private void assertCounts(Held held, int heldCount, int soldCount) {
        for (int i = 0; i < held.nights(); i++) {
            DailyInventory inventory = inventory(held, i);
            assertEquals(heldCount, inventory.heldCount(), "held " + i);
            assertEquals(soldCount, inventory.soldCount(), "sold " + i);
        }
    }

    private JsonNode detail(Held held) throws Exception {
        HttpResponse<String> res = get(held.path(), GUEST);
        assertEquals(200, res.statusCode(), res.body());
        return JSON.readTree(res.body());
    }

    /** BOOK-02. 상태 하나의 첫 페이지 100건 */
    private JsonNode itemOf(String status, Held held) throws Exception {
        HttpResponse<String> res = get(BOOKINGS + "?status=" + status + "&size=100", GUEST);
        assertEquals(200, res.statusCode(), res.body());
        for (JsonNode item : JSON.readTree(res.body()).get("items")) {
            if (held.bookingId().equals(item.get("id").asString())) {
                return item;
            }
        }
        throw new AssertionError(held.bookingId() + "가 " + status + " 목록에 없다: " + res.body());
    }

    /** 목록 항목과 상세는 같은 Booking이다. 조회 시각인 serverNow만 다르다(1차 K13, K25) */
    private static void assertSameBooking(JsonNode detail, JsonNode item) {
        ObjectNode a = (ObjectNode) detail.deepCopy();
        ObjectNode b = (ObjectNode) item.deepCopy();
        assertEquals(20, a.size(), a.toString());
        assertEquals(20, b.size(), b.toString());
        a.remove("serverNow");
        b.remove("serverNow");
        assertEquals(a, b);
    }

    private static void assertError(HttpResponse<String> res, int status, String code) {
        assertEquals(status, res.statusCode(), res.body());
        JsonNode json = JSON.readTree(res.body());
        assertEquals(code, json.get("code").asString(), res.body());
        assertTrue(json.hasNonNull("traceId"), res.body());
    }

    /** 11 응답 모델 PaymentAttempt의 표는 열한 행이다(BN1) */
    private static void assertAttempt(JsonNode json, Held held, String status, String mockMode) {
        assertEquals(11, json.size(), json.toString());
        assertTrue(json.get("id").asString().startsWith("attempt_"), json.toString());
        assertEquals(held.bookingId(), json.get("bookingId").asString());
        assertEquals(1, json.get("attemptNumber").asInt());
        assertEquals(status, json.get("status").asString());
        assertEquals(held.total(), json.get("amount").asLong());
        assertEquals("KRW", json.get("currency").asString());
        assertEquals(mockMode, json.get("mockMode").asString());
        assertTrue(json.get("completedAt").asString().matches(TIME_PATTERN), json.toString());
        assertTrue(json.get("failureCode").isNull(), json.toString());
    }

    /** 11 응답 모델 Refund의 표는 일곱 행이다(BN1). 전액이고 사유는 BOOKING_CANCELED다 */
    private static void assertRefund(JsonNode refund, Held held, String attemptId) {
        assertEquals(7, refund.size(), refund.toString());
        assertTrue(refund.get("id").asString().startsWith("refund_"), refund.toString());
        assertEquals(attemptId, refund.get("paymentAttemptId").asString());
        assertEquals(held.total(), refund.get("amount").asLong());
        assertEquals("KRW", refund.get("currency").asString());
        assertEquals("REFUNDED", refund.get("status").asString());
        assertEquals("BOOKING_CANCELED", refund.get("reason").asString());
        assertTrue(refund.get("refundedAt").asString().matches(TIME_PATTERN), refund.toString());
    }

    // ---------- L19 ----------

    @Test
    void L19_취소하면_200과_Booking_20개_필드이고_CANCELED와_환불_7개_필드와_승인_시도_유지와_sold_0이다()
            throws Exception {
        Held held = confirmed(2);
        JsonNode before = detail(held);
        String attemptId = before.get("payment").get("approvedAttemptId").asString();
        assertCounts(held, 0, 1);

        HttpResponse<String> res = cancel(GUEST, held, newKey(), REASON_BODY);

        assertEquals(200, res.statusCode(), res.body());
        // 11 BOOK-04 응답에 Location이 없다. 최초 응답이라 재전송 표시도 없다
        assertTrue(res.headers().firstValue("Location").isEmpty());
        assertTrue(res.headers().firstValue("Idempotency-Replayed").isEmpty());
        JsonNode json = JSON.readTree(res.body());
        assertEquals(20, json.size(), json.toString());
        assertEquals(held.bookingId(), json.get("id").asString());
        assertEquals("CANCELED", json.get("status").asString());
        assertEquals("일정 변경", json.get("cancellationReason").asString());
        assertTrue(json.get("canceledAt").asString().matches(TIME_PATTERN), json.toString());
        // 확정 시각은 남고 만료 쪽은 비어 있다. 전이마다 version이 하나 오른다(HELD 0, CONFIRMED 1)
        assertEquals(before.get("confirmedAt"), json.get("confirmedAt"));
        assertTrue(json.get("expirationReason").isNull());
        assertTrue(json.get("expiredAt").isNull());
        assertEquals(2, json.get("version").asInt());
        assertEquals(before.get("priceSnapshot"), json.get("priceSnapshot"));

        JsonNode payment = json.get("payment");
        assertEquals(4, payment.size(), payment.toString());
        assertEquals(1, payment.get("attemptCount").asInt());
        assertEquals(attemptId, payment.get("approvedAttemptId").asString());
        assertEquals(1, payment.get("attempts").size());
        // 승인 시도는 APPROVED 그대로다. 환불은 시도의 상태가 아니라 refund 객체다
        assertAttempt(payment.get("attempts").get(0), held, "APPROVED", "APPROVE");
        assertRefund(payment.get("refund"), held, attemptId);

        // 모든 숙박 날짜의 판매분이 돌아왔다(T25의 짝)
        assertCounts(held, 0, 0);
        assertEquals(2, inventory(held, 0).availableCount());
        assertEquals(2, inventory(held, 1).availableCount());

        // 취소 트랜잭션 안에서 채운 payment와 커밋 뒤 GET이 같은 것을 본다
        JsonNode after = detail(held);
        assertEquals("CANCELED", after.get("status").asString());
        assertEquals(json.get("payment"), after.get("payment"));
        assertEquals(json.get("canceledAt"), after.get("canceledAt"));
    }

    @Test
    void L19_같은_키_재전송은_최초_응답과_Idempotency_Replayed이고_환불과_반환이_한_번이며_reason_생략은_빈_문자열이다()
            throws Exception {
        Held held = confirmed(1);
        String key = newKey();

        HttpResponse<String> first = cancel(GUEST, held, key, REASON_BODY);
        HttpResponse<String> replay = cancel(GUEST, held, key, REASON_BODY);

        assertEquals(200, first.statusCode(), first.body());
        assertEquals(200, replay.statusCode(), replay.body());
        assertEquals(first.body(), replay.body());
        assertEquals("true", replay.headers().firstValue("Idempotency-Replayed").orElse(""));
        // T24. 환불 한 번, 반환 한 번. 두 번 돌았으면 version이 더 오르거나 sold가 음수로 가려다 깨진다
        JsonNode after = detail(held);
        assertEquals(2, after.get("version").asInt());
        assertEquals(JSON.readTree(first.body()).get("payment").get("refund"),
                after.get("payment").get("refund"));
        assertCounts(held, 0, 0);
        assertEquals(2, inventory(held, 0).availableCount());

        // 멱등 규칙 2. 같은 키에 다른 body
        assertError(cancel(GUEST, held, key, reasonBody("다른 사유")), 409, "IDEMPOTENCY_KEY_REUSED");

        // reason 생략은 빈 문자열이다(11 BOOK-04 요청 표). body 없음과 빈 객체와 빈 문자열은 같은
        // 요청이라 같은 키로 이어 보내면 재전송이다(계약 2절 BOOK-04 문단)
        Held omitted = confirmed(1);
        String omittedKey = newKey();
        HttpResponse<String> noBody = cancel(GUEST, omitted, omittedKey, null);
        assertEquals(200, noBody.statusCode(), noBody.body());
        assertEquals("", JSON.readTree(noBody.body()).get("cancellationReason").asString());
        HttpResponse<String> emptyObject = cancel(GUEST, omitted, omittedKey, "{}");
        assertEquals(200, emptyObject.statusCode(), emptyObject.body());
        assertEquals("true", emptyObject.headers().firstValue("Idempotency-Replayed").orElse(""));
        HttpResponse<String> emptyReason = cancel(GUEST, omitted, omittedKey, reasonBody(""));
        assertEquals(200, emptyReason.statusCode(), emptyReason.body());
        assertEquals("true", emptyReason.headers().firstValue("Idempotency-Replayed").orElse(""));
        assertEquals("", detail(omitted).get("cancellationReason").asString());
    }

    // ---------- L20 ----------

    @Test
    void L20_멱등키_없음과_reason_301자와_미정의_필드는_400이고_401_403_404이며_어느_경우도_수량이_안_바뀌고_300자는_통과다()
            throws Exception {
        Held held = confirmed(2);

        assertError(cancel(GUEST, held, null, REASON_BODY), 400, "IDEMPOTENCY_KEY_REQUIRED");
        assertError(cancel(GUEST, held, "short", REASON_BODY), 400, "IDEMPOTENCY_KEY_REQUIRED");
        assertError(cancel(GUEST, held, newKey(), reasonBody("가".repeat(301))), 400, "INVALID_REQUEST");
        assertError(cancel(GUEST, held, newKey(), "{\"reason\":\"일정 변경\",\"refund\":true}"), 400,
                "INVALID_REQUEST");
        assertError(cancel(null, held, newKey(), REASON_BODY), 401, "ACTOR_REQUIRED");
        assertError(cancel(HOST, held, newKey(), REASON_BODY), 403, "ACCESS_DENIED");
        HttpResponse<String> others = cancel(OTHER_GUEST, held, newKey(), REASON_BODY);
        HttpResponse<String> missing = send("POST", BOOKINGS + "/booking_none/cancellations", GUEST,
                newKey(), REASON_BODY);
        assertError(others, 404, "RESOURCE_NOT_FOUND");
        assertError(missing, 404, "RESOURCE_NOT_FOUND");
        // T02. 자원 정보 없이 같은 본문이다
        assertEquals(JSON.readTree(others.body()).get("message"), JSON.readTree(missing.body()).get("message"));

        // T25. 어느 거절도 취소와 환불과 반환을 남기지 않았다
        JsonNode json = detail(held);
        assertEquals("CONFIRMED", json.get("status").asString());
        assertEquals(1, json.get("version").asInt());
        assertTrue(json.get("cancellationReason").isNull());
        assertTrue(json.get("payment").get("refund").isNull());
        assertCounts(held, 0, 1);

        // 짝(T1, T2). 300자는 통과다. 글자 수라 한글 300자도 300이다
        String reason = "가".repeat(300);
        HttpResponse<String> ok = cancel(GUEST, held, newKey(), reasonBody(reason));
        assertEquals(200, ok.statusCode(), ok.body());
        assertEquals(reason, JSON.readTree(ok.body()).get("cancellationReason").asString());
        assertEquals(reason, detail(held).get("cancellationReason").asString());
        assertCounts(held, 0, 0);
    }

    @Test
    void L20_HELD와_EXPIRED와_이미_취소된_예약에_새_키는_409_BOOKING_STATE_CONFLICT이고_수량이_안_바뀐다()
            throws Exception {
        // 사람 경로라 종착 상태도 409다(08-3 결정 5). 정책 경로의 로그 후 무시와 다르다
        Held held = held(1);
        assertError(cancel(held), 409, "BOOKING_STATE_CONFLICT");
        JsonNode stillHeld = detail(held);
        assertEquals("HELD", stillHeld.get("status").asString());
        assertEquals(0, stillHeld.get("version").asInt());
        assertCounts(held, 1, 0);

        Held expired = held(1);
        expire(expired);
        assertError(cancel(expired), 409, "BOOKING_STATE_CONFLICT");
        JsonNode stillExpired = detail(expired);
        assertEquals("EXPIRED", stillExpired.get("status").asString());
        assertEquals(1, stillExpired.get("version").asInt());
        assertCounts(expired, 0, 0);

        Held canceled = confirmed(1);
        HttpResponse<String> first = cancel(canceled);
        assertEquals(200, first.statusCode(), first.body());
        // 11 BOOK-04 처리 규칙. 새 키로 이미 취소한 예약을 다시 취소하면 409다
        assertError(cancel(canceled), 409, "BOOKING_STATE_CONFLICT");
        JsonNode stillCanceled = detail(canceled);
        assertEquals("CANCELED", stillCanceled.get("status").asString());
        assertEquals(2, stillCanceled.get("version").asInt());
        assertEquals(JSON.readTree(first.body()).get("payment").get("refund"),
                stillCanceled.get("payment").get("refund"));
        assertCounts(canceled, 0, 0);
        assertEquals(2, inventory(canceled, 0).availableCount());
    }

    @Test
    void L20_체크인이_서울_오늘인_CONFIRMED는_409_CANCELLATION_NOT_ALLOWED이고_수량이_안_바뀌며_내일_체크인은_취소된다()
            throws Exception {
        // P05. 체크인 전날까지다. 서울의 오늘이 checkIn 이상이면 거절이다(2절 BOOK-04 표 9행)
        Held todayStay = confirmed(1, today());

        assertError(cancel(todayStay), 409, "CANCELLATION_NOT_ALLOWED");

        JsonNode json = detail(todayStay);
        assertEquals("CONFIRMED", json.get("status").asString());
        assertEquals(1, json.get("version").asInt());
        assertTrue(json.get("payment").get("refund").isNull());
        assertCounts(todayStay, 0, 1);

        // 짝(T2). 체크인이 내일이면 오늘이 전날이라 취소된다
        Held tomorrowStay = confirmed(1, today().plusDays(1));
        HttpResponse<String> ok = cancel(tomorrowStay);
        assertEquals(200, ok.statusCode(), ok.body());
        assertEquals("CANCELED", JSON.readTree(ok.body()).get("status").asString());
        assertCounts(tomorrowStay, 0, 0);
    }

    /**
     * L20. 멱등 규칙 4. 첫 요청이 잠긴 Booking 행에 붙잡혀 있는 동안 같은 키가 오면 409
     * REQUEST_IN_PROGRESS와 Retry-After: 1이다(1차 K17 방식). 소유 확인은 잠그지 않으므로 잠금 앞의
     * 읽기는 지나가고 findByIdForUpdate가 붙잡힌다.
     */
    @Test
    void L20_처리_중인_같은_키는_409_REQUEST_IN_PROGRESS이고_잠금이_풀리면_첫_요청은_200이며_취소는_한_번이다()
            throws Exception {
        Held held = confirmed(1);
        String key = newKey();

        ExecutorService waiter = Executors.newSingleThreadExecutor();
        try {
            Future<HttpResponse<String>> first = new TransactionTemplate(transactionManager)
                    .execute((status) -> {
                        bookingRepository.findByIdForUpdate(BookingId.of(held.bookingId())).orElseThrow();
                        Future<HttpResponse<String>> sent = waiter.submit(
                                () -> cancel(GUEST, held, key, REASON_BODY));
                        // 잠금을 쥔 동안 첫 요청이 끝나면 Booking을 잠그지 않고 취소한 것이다
                        assertThrows(TimeoutException.class, () -> sent.get(2, TimeUnit.SECONDS));

                        HttpResponse<String> second = assertDoesNotThrow(
                                () -> cancel(GUEST, held, key, REASON_BODY));
                        assertError(second, 409, "REQUEST_IN_PROGRESS");
                        assertEquals("1", second.headers().firstValue("Retry-After").orElse(""));
                        return sent;
                    });

            HttpResponse<String> res = first.get(30, TimeUnit.SECONDS);
            assertEquals(200, res.statusCode(), res.body());
            assertEquals("CANCELED", JSON.readTree(res.body()).get("status").asString());
        } finally {
            waiter.shutdownNow();
        }
        JsonNode after = detail(held);
        assertEquals(2, after.get("version").asInt());
        assertCounts(held, 0, 0);
        // 완료된 뒤의 같은 키는 재전송이다
        HttpResponse<String> replay = cancel(GUEST, held, key, REASON_BODY);
        assertEquals(200, replay.statusCode(), replay.body());
        assertEquals("true", replay.headers().firstValue("Idempotency-Replayed").orElse(""));
    }

    // ---------- L21 ----------

    @Test
    void L21_목록의_항목과_상세가_같은_payment를_내고_시도_없는_예약은_0과_null과_빈_배열과_null이다()
            throws Exception {
        Held none = held(1);
        Held confirmed = confirmed(1);
        Held canceled = confirmed(1);
        assertEquals(200, cancel(canceled).statusCode());

        // 시도 없음. 11 응답 모델 PaymentSummary의 네 행이 빈 값 그대로다
        JsonNode noneDetail = detail(none);
        JsonNode empty = noneDetail.get("payment");
        assertEquals(4, empty.size(), empty.toString());
        assertEquals(0, empty.get("attemptCount").asInt());
        assertTrue(empty.get("approvedAttemptId").isNull());
        assertEquals(0, empty.get("attempts").size());
        assertTrue(empty.get("refund").isNull());
        assertSameBooking(noneDetail, itemOf("HELD", none));

        // 승인 하나. 항목과 상세가 같은 시도와 같은 approvedAttemptId를 낸다
        JsonNode confirmedDetail = detail(confirmed);
        JsonNode approved = confirmedDetail.get("payment");
        assertEquals(1, approved.get("attemptCount").asInt());
        assertEquals(approved.get("attempts").get(0).get("id"), approved.get("approvedAttemptId"));
        assertTrue(approved.get("refund").isNull());
        assertSameBooking(confirmedDetail, itemOf("CONFIRMED", confirmed));

        // 취소 뒤. 환불까지 같다
        JsonNode canceledDetail = detail(canceled);
        assertRefund(canceledDetail.get("payment").get("refund"), canceled,
                canceledDetail.get("payment").get("approvedAttemptId").asString());
        assertSameBooking(canceledDetail, itemOf("CANCELED", canceled));

        // 셋은 서로 다른 payment다. 채움이 항목마다 한 번씩 제 예약의 것을 읽었다
        assertNotEquals(empty, approved);
        assertNotEquals(approved, canceledDetail.get("payment"));
    }

    // ---------- L22 ----------

    @Test
    void L22_승인_뒤_취소_뒤_같은_승인_이벤트를_다시_넣으면_200_DUPLICATE이고_CANCELED와_환불_한_번이_유지된다()
            throws Exception {
        // T22. DEFER로 열고 INTERNAL-01로 승인해야 같은 이벤트를 손에 들고 다시 보낼 수 있다
        Held held = held(1);
        HttpResponse<String> res = pay(held, "DEFER");
        assertEquals(202, res.statusCode(), res.body());
        String event = approvalEvent(JSON.readTree(res.body()));

        HttpResponse<String> first = sendEvent(event);
        assertEquals(200, first.statusCode(), first.body());
        assertEquals("PROCESSED", JSON.readTree(first.body()).get("result").asString());
        assertEquals("CONFIRMED", detail(held).get("status").asString());

        HttpResponse<String> canceled = cancel(held);
        assertEquals(200, canceled.statusCode(), canceled.body());
        JsonNode refund = JSON.readTree(canceled.body()).get("payment").get("refund");
        assertEquals("BOOKING_CANCELED", refund.get("reason").asString());

        HttpResponse<String> again = sendEvent(event);

        // 결제가 같은 eventId를 DUPLICATE로 막아 예약의 구독자에게 두 번 오지 않는다(08-3 11-6)
        assertEquals(200, again.statusCode(), again.body());
        assertEquals("DUPLICATE", JSON.readTree(again.body()).get("result").asString());
        JsonNode after = detail(held);
        assertEquals("CANCELED", after.get("status").asString());
        assertEquals(2, after.get("version").asInt());
        assertEquals(1, after.get("payment").get("attemptCount").asInt());
        assertAttempt(after.get("payment").get("attempts").get(0), held, "APPROVED", "DEFER");
        assertEquals(refund, after.get("payment").get("refund"));
        assertCounts(held, 0, 0);
        assertEquals(2, inventory(held, 0).availableCount());
    }

    @Test
    void L22_만료된_예약의_BOOK_01_성공_키를_재전송하면_최초_HELD_응답이고_GET은_EXPIRED다() throws Exception {
        // T29. 재전송 응답은 최초 시점의 스냅샷이다(11 멱등 처리 절 끝). 현재 상태는 GET이 준다
        Held held = held(1);
        expire(held);

        HttpResponse<String> replay = send("POST", BOOKINGS, GUEST, held.key(), held.requestBody());

        assertEquals(201, replay.statusCode(), replay.body());
        assertEquals("true", replay.headers().firstValue("Idempotency-Replayed").orElse(""));
        assertEquals(held.path(), replay.headers().firstValue("Location").orElse(""));
        assertEquals(held.firstResponse(), replay.body());
        assertEquals("HELD", JSON.readTree(replay.body()).get("status").asString());

        JsonNode now = detail(held);
        assertEquals("EXPIRED", now.get("status").asString());
        assertEquals("TTL_EXPIRED", now.get("expirationReason").asString());
        assertEquals(1, now.get("version").asInt());
        assertCounts(held, 0, 0);
        // 재전송은 새 예약을 만들지 않았다. HELD 목록에 이 예약은 없고 새 것도 없다
        for (JsonNode item : JSON.readTree(get(BOOKINGS + "?status=HELD&size=100", GUEST).body()).get("items")) {
            assertNotEquals(held.bookingId(), item.get("id").asString());
            assertNotEquals(held.roomTypeId(), item.get("roomTypeId").asString());
        }
    }
}
