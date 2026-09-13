package com.o2o.booking.api;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
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
import com.o2o.payment.domain.MockMode;
import com.o2o.payment.domain.Payment;
import com.o2o.payment.domain.PaymentAttempt;
import com.o2o.payment.domain.PaymentRepository;
import com.o2o.shared.Money;
import com.o2o.shared.RoomTypeId;
import com.o2o.shared.SeoulDate;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * L14부터 L18. 설계 근거: task-S9-booking-lifecycle 8-1절과 2절 PAY-01 검사 순서 표, 11 PAY-01(요청
 * 표, 202와 Location, 에러 표, 처리 규칙)과 PAY-02(PaymentAttemptList, 처리 규칙), 11 멱등 처리 절,
 * 11 인증과 접근 제어, 응답 모델 PaymentAttempt와 PaymentSummary와 Booking, T02, T14, T15, T16, T17.
 *
 * 1차 BookingApiTest와 같은 방식이다. 진짜 포트를 열고 JDK HttpClient로 친다. 서버가 실제 시계로
 * 돌므로 날짜는 서울의 오늘에서 센다(testing.md T4, T5). 데이터를 되돌리지 않아 테스트마다 숙소와
 * 객실 타입과 멱등키를 새로 만든다. 스케줄러는 테스트 설정에서 꺼져 있어 만료 시각이 지난 HELD는
 * PAY-01의 선만료(7절 D-2 가)가 처음 만진다.
 *
 * 실제 시계로는 만들 수 없는 상태 둘은 DB에 직접 놓는다(T6). 만료 시각이 지난 HELD는 expires_at을
 * created_at으로 당기고, 한도 도달은 결제 집합체에 실패 시도 셋을 심는다. HTTP만으로 셋을 실패시키면
 * 셋째 실패가 예약을 만료시켜 넷째는 BOOKING_EXPIRED가 먼저이기 때문이다(8-1 L15).
 *
 * APPROVE와 DECLINE의 결과는 결제의 자동 결과 어댑터와 이 묶음의 구독자가 같은 요청 스레드에서
 * 커밋 뒤에 처리하므로 202가 돌아온 순간 GET이 확정이나 실패를 본다. DEFER는 INTERNAL-01이 오기
 * 전까지 REQUESTED다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BookingPaymentApiTest {

    private static final String HOST = "host_001";
    private static final String GUEST = "guest_001";
    private static final String OTHER_GUEST = "guest_002";
    private static final String MOCK = "mock_001";
    private static final String BOOKINGS = "/api/v1/bookings";
    private static final String EVENTS = "/internal/mock-payments/events";
    private static final long RATE = 100_000L;
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
    private PaymentRepository paymentRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    /** HELD 하나. 이 테스트가 만든 객실 타입과 날짜와 스냅샷 총액을 함께 든다 */
    private record Held(String bookingId, String roomTypeId, LocalDate checkIn, int nights, long total) {
        String path() {
            return BOOKINGS + "/" + bookingId;
        }

        String attemptsPath() {
            return path() + "/payment-attempts";
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

    /** PAY-01. 손님과 키와 body */
    private HttpResponse<String> pay(String guest, Held held, String key, String body) throws Exception {
        return send("POST", held.attemptsPath(), guest, key, body);
    }

    private HttpResponse<String> pay(Held held, String mockMode) throws Exception {
        return pay(GUEST, held, newKey(), "{\"mockMode\":\"" + mockMode + "\"}");
    }

    private String roomTypeOf(String host) throws Exception {
        String propertyBody = """
                {"name":"결제 테스트 스테이","regionCode":"SEOUL","address":"서울특별시 중구 예시로 6","description":""}
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
    private Held held(int nights) throws Exception {
        String roomTypeId = roomTypeOf(HOST);
        LocalDate checkIn = today().plusDays(10);
        prepare(roomTypeId, checkIn, nights, 2);
        String body = "{\"roomTypeId\":\"" + roomTypeId + "\",\"checkIn\":\"" + checkIn
                + "\",\"checkOut\":\"" + checkIn.plusDays(nights) + "\",\"guestCount\":1"
                + ",\"expectedTotalAmount\":" + RATE * nights + ",\"currency\":\"KRW\"}";
        HttpResponse<String> res = send("POST", BOOKINGS, GUEST, newKey(), body);
        assertEquals(201, res.statusCode(), res.body());
        return new Held(JSON.readTree(res.body()).get("id").asString(), roomTypeId, checkIn, nights,
                RATE * nights);
    }

    /** T6. 만료 시각을 생성 시각으로 당긴다. 실제 시계로 10분을 기다리는 대신 지난 상태를 놓는다 */
    private void expireInDb(Held held) {
        assertEquals(1, jdbcTemplate.update("update booking set expires_at = created_at where id = ?",
                held.bookingId()));
    }

    /** T6. 결제 집합체에 실패 시도를 심는다. 예약은 HELD 그대로라 한도 도달만 남는다 */
    private void seedFailed(Held held, int count) {
        new TransactionTemplate(transactionManager).executeWithoutResult((status) -> {
            Instant now = Instant.now();
            Money charge = Money.krw(held.total());
            Payment payment = Payment.open(held.bookingId(), charge, now);
            for (int i = 0; i < count; i++) {
                PaymentAttempt attempt = payment.openAttempt(charge, MockMode.DECLINE, now);
                String pgTransactionId = "mock_tx_seed_" + attempt.id().value();
                payment.attachPgTransaction(attempt.id(), pgTransactionId);
                payment.recordFailure(attempt.id(), pgTransactionId, PaymentAttempt.MOCK_DECLINED, now);
            }
            paymentRepository.save(payment);
        });
    }

    /** INTERNAL-01 승인. 11 요청 표의 필수 여섯을 202 body의 시도에서 옮긴다 */
    private HttpResponse<String> approveViaInternal(JsonNode attempt) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("eventId", "mock_event_" + UUID.randomUUID().toString().replace("-", ""));
        body.put("paymentAttemptId", attempt.get("id").asString());
        body.put("pgTransactionId", attempt.get("pgTransactionId").asString());
        body.put("outcome", "APPROVED");
        body.put("amount", attempt.get("amount").asLong());
        body.put("currency", attempt.get("currency").asString());
        return send("POST", EVENTS, MOCK, JSON.writeValueAsString(body));
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

    private JsonNode attempts(Held held) throws Exception {
        HttpResponse<String> res = get(held.attemptsPath(), GUEST);
        assertEquals(200, res.statusCode(), res.body());
        return JSON.readTree(res.body());
    }

    private static void assertError(HttpResponse<String> res, int status, String code) {
        assertEquals(status, res.statusCode(), res.body());
        JsonNode json = JSON.readTree(res.body());
        assertEquals(code, json.get("code").asString(), res.body());
        assertTrue(json.hasNonNull("traceId"), res.body());
    }

    /** 11 응답 모델 PaymentAttempt의 표는 열한 행이다. 열두 번째가 있으면 명세와 다르다(BN1) */
    private static void assertAttempt(JsonNode json, Held held, int attemptNumber, String status,
                                      String mockMode, String failureCode) {
        assertEquals(11, json.size(), json.toString());
        assertTrue(json.get("id").asString().startsWith("attempt_"), json.toString());
        assertEquals(held.bookingId(), json.get("bookingId").asString());
        assertEquals(attemptNumber, json.get("attemptNumber").asInt());
        assertEquals(status, json.get("status").asString());
        assertEquals(held.total(), json.get("amount").asLong());
        assertEquals("KRW", json.get("currency").asString());
        assertTrue(json.get("pgTransactionId").asString().startsWith("mock_tx_"), json.toString());
        assertEquals(mockMode, json.get("mockMode").asString());
        assertTrue(json.get("requestedAt").asString().matches(TIME_PATTERN), json.toString());
        if ("REQUESTED".equals(status)) {
            assertTrue(json.get("completedAt").isNull(), json.toString());
        } else {
            assertTrue(json.get("completedAt").asString().matches(TIME_PATTERN), json.toString());
        }
        if (failureCode == null) {
            assertTrue(json.get("failureCode").isNull(), json.toString());
        } else {
            assertEquals(failureCode, json.get("failureCode").asString());
        }
    }

    // ---------- L14 ----------

    @Test
    void L14_결제를_요청하면_202와_Location과_PaymentAttempt_11개_필드가_오고_mockMode_생략은_APPROVE다()
            throws Exception {
        Held held = held(2);

        HttpResponse<String> res = pay(GUEST, held, newKey(), "{}");

        assertEquals(202, res.statusCode(), res.body());
        assertEquals(held.attemptsPath(), res.headers().firstValue("Location").orElse(""));
        assertTrue(res.headers().firstValue("Idempotency-Replayed").isEmpty());
        JsonNode json = JSON.readTree(res.body());
        // 접수 결과다. 승인은 커밋 뒤 처리라 응답 body는 REQUESTED다(11 PAY-01 처리 규칙)
        assertAttempt(json, held, 1, "REQUESTED", "APPROVE", null);

        // body 없음은 빈 객체와 같다(계약 2절 PAY-01 표 1행). 다른 예약에 친다
        Held another = held(1);
        HttpResponse<String> noBody = pay(GUEST, another, newKey(), null);
        assertEquals(202, noBody.statusCode(), noBody.body());
        assertEquals("APPROVE", JSON.readTree(noBody.body()).get("mockMode").asString());
    }

    @Test
    void L14_같은_키_재전송은_최초_응답과_Idempotency_Replayed이고_시도_수가_그대로이며_다른_body는_409다()
            throws Exception {
        Held held = held(1);
        String key = newKey();
        String body = "{\"mockMode\":\"DEFER\"}";

        HttpResponse<String> first = pay(GUEST, held, key, body);
        HttpResponse<String> replay = pay(GUEST, held, key, body);

        assertEquals(202, first.statusCode(), first.body());
        assertEquals(202, replay.statusCode(), replay.body());
        assertEquals(first.body(), replay.body());
        assertEquals(held.attemptsPath(), replay.headers().firstValue("Location").orElse(""));
        assertEquals("true", replay.headers().firstValue("Idempotency-Replayed").orElse(""));
        // T14. 재전송은 시도를 늘리지 않는다
        assertEquals(1, attempts(held).get("attemptCount").asInt());

        // 멱등 규칙 2. 같은 키에 다른 body
        assertError(pay(GUEST, held, key, "{\"mockMode\":\"APPROVE\"}"), 409, "IDEMPOTENCY_KEY_REUSED");
        assertEquals(1, attempts(held).get("attemptCount").asInt());

        // 범위에 예약 ID가 들어가므로 다른 예약에 같은 키는 새 요청이다(계약 2절 표 4행)
        Held other = held(1);
        HttpResponse<String> otherBooking = pay(GUEST, other, key, body);
        assertEquals(202, otherBooking.statusCode(), otherBooking.body());
        assertTrue(otherBooking.headers().firstValue("Idempotency-Replayed").isEmpty());
        assertEquals(other.bookingId(), JSON.readTree(otherBooking.body()).get("bookingId").asString());
    }

    /**
     * L14. 멱등 규칙 4. 첫 요청이 잠긴 Booking 행에 붙잡혀 있는 동안 같은 키가 오면 409
     * REQUEST_IN_PROGRESS와 Retry-After: 1이다(1차 K17 방식). 선만료의 잠금이 먼저 붙잡힌다.
     */
    @Test
    void L14_처리_중인_같은_키는_409_REQUEST_IN_PROGRESS이고_잠금이_풀리면_첫_요청은_202다() throws Exception {
        Held held = held(1);
        String key = newKey();
        String body = "{\"mockMode\":\"DEFER\"}";

        ExecutorService waiter = Executors.newSingleThreadExecutor();
        try {
            Future<HttpResponse<String>> first = new TransactionTemplate(transactionManager)
                    .execute((status) -> {
                        bookingRepository.findByIdForUpdate(BookingId.of(held.bookingId())).orElseThrow();
                        Future<HttpResponse<String>> sent = waiter.submit(() -> pay(GUEST, held, key, body));
                        // 잠금을 쥔 동안 첫 요청이 끝나면 Booking을 잠그지 않고 시도를 연 것이다
                        assertThrows(TimeoutException.class, () -> sent.get(2, TimeUnit.SECONDS));

                        HttpResponse<String> second = assertDoesNotThrow(() -> pay(GUEST, held, key, body));
                        assertError(second, 409, "REQUEST_IN_PROGRESS");
                        assertEquals("1", second.headers().firstValue("Retry-After").orElse(""));
                        return sent;
                    });

            HttpResponse<String> res = first.get(30, TimeUnit.SECONDS);
            assertEquals(202, res.statusCode(), res.body());
        } finally {
            waiter.shutdownNow();
        }
        assertEquals(1, attempts(held).get("attemptCount").asInt());
        // 완료된 뒤의 같은 키는 재전송이다
        HttpResponse<String> replay = pay(GUEST, held, key, body);
        assertEquals(202, replay.statusCode(), replay.body());
        assertEquals("true", replay.headers().firstValue("Idempotency-Replayed").orElse(""));
    }

    // ---------- L15 ----------

    @Test
    void L15_멱등키가_없으면_400이고_형식_밖_body는_400이며_행위자_없음은_401_HOST는_403이다() throws Exception {
        Held held = held(1);
        String body = "{\"mockMode\":\"DEFER\"}";

        assertError(pay(GUEST, held, null, body), 400, "IDEMPOTENCY_KEY_REQUIRED");
        assertError(pay(GUEST, held, "short", body), 400, "IDEMPOTENCY_KEY_REQUIRED");
        assertError(pay(GUEST, held, newKey(), "{\"mockMode\":\"CASH\"}"), 400, "INVALID_REQUEST");
        assertError(pay(GUEST, held, newKey(), "{\"mockMode\":\"DEFER\",\"amount\":1}"), 400,
                "INVALID_REQUEST");
        assertError(pay(null, held, newKey(), body), 401, "ACTOR_REQUIRED");
        assertError(pay(HOST, held, newKey(), body), 403, "ACCESS_DENIED");

        // 어느 거절도 시도를 열지 않았다
        assertEquals(0, attempts(held).get("attemptCount").asInt());
        assertEquals("HELD", detail(held).get("status").asString());
    }

    @Test
    void L15_남의_예약과_없는_예약은_404이고_본문이_같다() throws Exception {
        Held held = held(1);
        String body = "{\"mockMode\":\"DEFER\"}";

        HttpResponse<String> others = pay(OTHER_GUEST, held, newKey(), body);
        HttpResponse<String> missing = send("POST", BOOKINGS + "/booking_none/payment-attempts", GUEST,
                newKey(), body);

        assertError(others, 404, "RESOURCE_NOT_FOUND");
        assertError(missing, 404, "RESOURCE_NOT_FOUND");
        // T02. 자원 정보 없이 같은 본문이다. traceId만 다르다
        assertEquals(JSON.readTree(others.body()).get("message"), JSON.readTree(missing.body()).get("message"));
        assertEquals(0, attempts(held).get("attemptCount").asInt());
    }

    @Test
    void L15_만료_시각_지난_HELD는_409_BOOKING_EXPIRED이고_응답_뒤_GET이_EXPIRED와_held_반환이며_이미_EXPIRED도_409다()
            throws Exception {
        Held held = held(2);
        assertCounts(held, 1, 0);
        expireInDb(held);

        HttpResponse<String> res = pay(held, "APPROVE");

        // 선만료가 별도 트랜잭션으로 저장된 뒤 409다(계약 2절 표 7행, 7절 D-2 가)
        assertError(res, 409, "BOOKING_EXPIRED");
        JsonNode json = detail(held);
        assertEquals("EXPIRED", json.get("status").asString());
        assertEquals("TTL_EXPIRED", json.get("expirationReason").asString());
        assertTrue(json.get("expiredAt").asString().matches(TIME_PATTERN), json.toString());
        assertEquals(1, json.get("version").asInt());
        assertEquals(0, json.get("payment").get("attemptCount").asInt());
        assertCounts(held, 0, 0);
        assertEquals(2, inventory(held, 0).availableCount());

        // 이미 EXPIRED. 새 키로 다시 요청해도 같은 409이고 시도는 열리지 않는다
        assertError(pay(held, "APPROVE"), 409, "BOOKING_EXPIRED");
        assertEquals(0, attempts(held).get("attemptCount").asInt());
    }

    @Test
    void L15_CONFIRMED에_새_키로_결제를_요청하면_409_BOOKING_STATE_CONFLICT다() throws Exception {
        Held held = held(1);
        assertEquals(202, pay(held, "APPROVE").statusCode());
        assertEquals("CONFIRMED", detail(held).get("status").asString());

        assertError(pay(held, "APPROVE"), 409, "BOOKING_STATE_CONFLICT");

        assertEquals(1, attempts(held).get("attemptCount").asInt());
        assertCounts(held, 0, 1);
    }

    @Test
    void L15_DEFER_시도가_진행_중이면_새_키는_409_PAYMENT_IN_PROGRESS다() throws Exception {
        // T15. 완료되지 않은 시도가 있으면 새 시도를 받지 않는다(I6)
        Held held = held(1);
        assertEquals(202, pay(held, "DEFER").statusCode());

        assertError(pay(held, "APPROVE"), 409, "PAYMENT_IN_PROGRESS");

        JsonNode list = attempts(held);
        assertEquals(1, list.get("attemptCount").asInt());
        assertEquals("REQUESTED", list.get("items").get(0).get("status").asString());
        assertEquals("HELD", detail(held).get("status").asString());
    }

    @Test
    void L15_DECLINE_셋_뒤_넷째는_BOOKING_EXPIRED가_먼저이고_실패_셋을_심은_HELD는_409_PAYMENT_ATTEMPTS_EXHAUSTED다()
            throws Exception {
        // 순서. 셋째 실패가 예약을 만료시키므로(P3) 넷째 요청은 한도가 아니라 만료가 먼저다
        Held expired = held(1);
        for (int i = 0; i < 3; i++) {
            assertEquals(202, pay(expired, "DECLINE").statusCode(), "attempt " + (i + 1));
        }
        assertEquals("EXPIRED", detail(expired).get("status").asString());
        assertError(pay(expired, "DECLINE"), 409, "BOOKING_EXPIRED");
        assertEquals(3, attempts(expired).get("attemptCount").asInt());

        // 한도 매핑. 예약은 HELD인데 결제에 실패 셋이 있는 상태를 심는다(T6). 앱 서비스 L6의 짝
        Held exhausted = held(1);
        seedFailed(exhausted, 3);

        assertError(pay(exhausted, "APPROVE"), 409, "PAYMENT_ATTEMPTS_EXHAUSTED");

        assertEquals("HELD", detail(exhausted).get("status").asString());
        assertEquals(3, attempts(exhausted).get("attemptCount").asInt());
        assertCounts(exhausted, 1, 0);
    }

    // ---------- L16 ----------

    @Test
    void L16_APPROVE_흐름_PAY_01_뒤_GET_상세가_CONFIRMED이고_payment가_채워지며_held_0_sold_1이다() throws Exception {
        Held held = held(2);

        HttpResponse<String> res = pay(GUEST, held, newKey(), "{}");

        assertEquals(202, res.statusCode(), res.body());
        String attemptId = JSON.readTree(res.body()).get("id").asString();
        JsonNode json = detail(held);
        assertEquals(20, json.size(), json.toString());
        assertEquals("CONFIRMED", json.get("status").asString());
        assertTrue(json.get("confirmedAt").asString().matches(TIME_PATTERN), json.toString());
        assertEquals(1, json.get("version").asInt());
        for (String nullField : List.of("expirationReason", "cancellationReason", "canceledAt", "expiredAt")) {
            assertTrue(json.get(nullField).isNull(), nullField);
        }
        JsonNode payment = json.get("payment");
        assertEquals(4, payment.size(), payment.toString());
        assertEquals(1, payment.get("attemptCount").asInt());
        assertEquals(attemptId, payment.get("approvedAttemptId").asString());
        assertEquals(1, payment.get("attempts").size());
        assertAttempt(payment.get("attempts").get(0), held, 1, "APPROVED", "APPROVE", null);
        assertEquals(attemptId, payment.get("attempts").get(0).get("id").asString());
        assertTrue(payment.get("refund").isNull());
        assertCounts(held, 0, 1);
        assertEquals(1, inventory(held, 0).availableCount());
    }

    // ---------- L17 ----------

    @Test
    void L17_DECLINE_뒤_GET이_HELD와_FAILED와_MOCK_DECLINED이고_DECLINE_셋이면_EXPIRED_PAYMENT_FAILED와_held_0이다()
            throws Exception {
        Held held = held(2);

        assertEquals(202, pay(held, "DECLINE").statusCode());

        // T16. 실패 하나는 HELD를 유지하고 선점도 그대로다
        JsonNode afterOne = detail(held);
        assertEquals("HELD", afterOne.get("status").asString());
        assertEquals(0, afterOne.get("version").asInt());
        JsonNode payment = afterOne.get("payment");
        assertEquals(1, payment.get("attemptCount").asInt());
        assertTrue(payment.get("approvedAttemptId").isNull());
        assertAttempt(payment.get("attempts").get(0), held, 1, "FAILED", "DECLINE", "MOCK_DECLINED");
        assertCounts(held, 1, 0);

        assertEquals(202, pay(held, "DECLINE").statusCode());
        assertEquals("HELD", detail(held).get("status").asString());
        assertEquals(202, pay(held, "DECLINE").statusCode());

        // T17. 셋째 실패가 만료시키고 선점을 돌려준다
        JsonNode afterThree = detail(held);
        assertEquals("EXPIRED", afterThree.get("status").asString());
        assertEquals("PAYMENT_FAILED", afterThree.get("expirationReason").asString());
        assertTrue(afterThree.get("expiredAt").asString().matches(TIME_PATTERN), afterThree.toString());
        assertEquals(1, afterThree.get("version").asInt());
        assertEquals(3, afterThree.get("payment").get("attemptCount").asInt());
        assertTrue(afterThree.get("payment").get("approvedAttemptId").isNull());
        assertCounts(held, 0, 0);
        assertEquals(2, inventory(held, 0).availableCount());
    }

    @Test
    void L17_DEFER는_REQUESTED로_남고_INTERNAL_01로_승인을_넣으면_CONFIRMED다() throws Exception {
        Held held = held(1);

        HttpResponse<String> res = pay(held, "DEFER");

        assertEquals(202, res.statusCode(), res.body());
        JsonNode attempt = JSON.readTree(res.body());
        JsonNode deferred = detail(held);
        assertEquals("HELD", deferred.get("status").asString());
        assertAttempt(deferred.get("payment").get("attempts").get(0), held, 1, "REQUESTED", "DEFER", null);
        assertCounts(held, 1, 0);

        HttpResponse<String> event = approveViaInternal(attempt);

        assertEquals(200, event.statusCode(), event.body());
        assertEquals("PROCESSED", JSON.readTree(event.body()).get("result").asString());
        JsonNode confirmed = detail(held);
        assertEquals("CONFIRMED", confirmed.get("status").asString());
        assertTrue(confirmed.get("confirmedAt").asString().matches(TIME_PATTERN), confirmed.toString());
        assertEquals(1, confirmed.get("version").asInt());
        assertEquals(attempt.get("id").asString(), confirmed.get("payment").get("approvedAttemptId").asString());
        assertAttempt(confirmed.get("payment").get("attempts").get(0), held, 1, "APPROVED", "DEFER", null);
        assertCounts(held, 0, 1);
    }

    // ---------- L18 ----------

    @Test
    void L18_시도_목록은_200과_3개_필드이고_attemptNumber_오름차순이며_시도_없으면_0과_빈_배열이다() throws Exception {
        Held held = held(1);

        JsonNode empty = attempts(held);
        // 11 응답 모델 PaymentAttemptList의 표는 세 행이다(BN1)
        assertEquals(3, empty.size(), empty.toString());
        assertEquals(held.bookingId(), empty.get("bookingId").asString());
        assertEquals(0, empty.get("attemptCount").asInt());
        assertEquals(0, empty.get("items").size());

        assertEquals(202, pay(held, "DECLINE").statusCode());
        assertEquals(202, pay(held, "DEFER").statusCode());

        JsonNode two = attempts(held);
        assertEquals(2, two.get("attemptCount").asInt());
        assertEquals(2, two.get("items").size());
        assertAttempt(two.get("items").get(0), held, 1, "FAILED", "DECLINE", "MOCK_DECLINED");
        assertAttempt(two.get("items").get(1), held, 2, "REQUESTED", "DEFER", null);
        assertFalse(two.has("refund"));
    }

    @Test
    void L18_시도_목록의_행위자_없음은_401_HOST는_403_남의_예약과_없는_예약은_404다() throws Exception {
        Held held = held(1);

        assertError(get(held.attemptsPath(), null), 401, "ACTOR_REQUIRED");
        assertError(get(held.attemptsPath(), HOST), 403, "ACCESS_DENIED");
        assertError(get(held.attemptsPath(), OTHER_GUEST), 404, "RESOURCE_NOT_FOUND");
        assertError(get(BOOKINGS + "/booking_none/payment-attempts", GUEST), 404, "RESOURCE_NOT_FOUND");
        assertEquals(200, get(held.attemptsPath(), GUEST).statusCode());
    }
}
