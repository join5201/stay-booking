package com.o2o.booking.api;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.o2o.booking.domain.Booking;
import com.o2o.booking.domain.BookingRepository;
import com.o2o.booking.domain.IdempotencyKey;
import com.o2o.booking.domain.IdempotencyRecord;
import com.o2o.booking.domain.IdempotencyRecordRepository;
import com.o2o.booking.domain.IdempotencyScope;
import com.o2o.booking.domain.UserId;
import com.o2o.inventory.domain.DailyInventory;
import com.o2o.inventory.domain.DailyInventoryRepository;
import com.o2o.shared.PageQuery;
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
 * K13부터 K23. 설계 근거: task-S9-booking 8-1절과 2절 검사 순서 표, 11 BOOK-01(요청 표, 201과
 * Location, 에러 표, 처리 규칙), 11 멱등 처리 절의 아홉 규칙, 11 인증과 접근 제어, 11 응답 모델
 * Booking, T04, T07, T08, T10, T11, T12.
 *
 * 앞 묶음의 InventoryApiTest와 같은 방식이다. 진짜 포트를 열고 JDK HttpClient로 친다. 서버가
 * 다른 스레드에서 실제 시계로 돌므로 날짜는 서울의 오늘에서 센다(testing.md T4, T5). 트랜잭션을
 * 되돌릴 수 없어 테스트마다 숙소와 객실 타입과 멱등키를 새로 만든다.
 *
 * 잠금이 낀 둘(K17, K19)은 앞 묶음 V14의 방식이다. 테스트 트랜잭션이 재고 행을 잠근 채 다른
 * 스레드로 서버에 요청을 보내고, 요청이 잠금에 붙잡혀 있는 동안 두 번째 요청을 보낸다. 순차
 * 호출 둘로는 진행 중 거절도 경합도 볼 수 없다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BookingApiTest {

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
    private DailyInventoryRepository inventoryRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private IdempotencyRecordRepository idempotencyRecordRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    /** 서버와 같은 오늘. 서버가 서울 날짜로 판정하므로 여기도 서울 날짜다(11 명세 35행) */
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

    /** BOOK-01. 손님과 키와 body */
    private HttpResponse<String> book(String guest, String key, String body) throws Exception {
        return send("POST", BOOKINGS, guest, key, body);
    }

    /** 소유자가 host이고 최대 인원 2인 객실 타입 하나를 만든다. 반환은 roomTypeId */
    private String roomTypeOf(String host) throws Exception {
        String propertyBody = """
                {"name":"예약 테스트 스테이","regionCode":"SEOUL","address":"서울특별시 중구 예시로 4","description":""}
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

    private void openInventory(String roomTypeId, LocalDate date, int total) throws Exception {
        HttpResponse<String> res = send("POST", "/api/v1/room-types/" + roomTypeId + "/inventories",
                HOST, "{\"date\":\"" + date + "\",\"totalCount\":" + total + "}");
        assertEquals(201, res.statusCode(), res.body());
    }

    private void registerRate(String roomTypeId, LocalDate date, long amount) throws Exception {
        HttpResponse<String> res = send("POST", "/api/v1/room-types/" + roomTypeId + "/rates", HOST,
                "{\"date\":\"" + date + "\",\"amount\":" + amount + ",\"currency\":\"KRW\"}");
        assertEquals(201, res.statusCode(), res.body());
    }

    /** from부터 nights 밤을 재고 total과 요금 RATE로 연다. 체크아웃 날짜는 열지 않는다(T06) */
    private void prepare(String roomTypeId, LocalDate from, int nights, int total) throws Exception {
        for (int i = 0; i < nights; i++) {
            openInventory(roomTypeId, from.plusDays(i), total);
            registerRate(roomTypeId, from.plusDays(i), RATE);
        }
    }

    private static String body(String roomTypeId, LocalDate checkIn, LocalDate checkOut,
                               int guestCount, long total) {
        return "{\"roomTypeId\":\"" + roomTypeId + "\",\"checkIn\":\"" + checkIn
                + "\",\"checkOut\":\"" + checkOut + "\",\"guestCount\":" + guestCount
                + ",\"expectedTotalAmount\":" + total + ",\"currency\":\"KRW\"}";
    }

    private static String body(String roomTypeId, LocalDate checkIn, int nights, int guestCount) {
        return body(roomTypeId, checkIn, checkIn.plusDays(nights), guestCount, RATE * nights);
    }

    private int heldCount(String roomTypeId, LocalDate date) {
        return inventoryRepository.findByRoomTypeIdAndStayDate(RoomTypeId.of(roomTypeId), date)
                .orElseThrow().heldCount();
    }

    /** 이 손님이 이 객실 타입에 가진 예약. 데이터를 되돌리지 않으므로 객실 타입으로 거른다 */
    private List<Booking> bookingsOf(String guest, String roomTypeId) {
        return bookingRepository.findByUserId(UserId.of(guest), null, PageQuery.of(0, 100))
                .items().stream()
                .filter((b) -> b.roomTypeId().value().equals(roomTypeId))
                .toList();
    }

    private static void assertError(HttpResponse<String> res, int status, String code) {
        assertEquals(status, res.statusCode(), res.body());
        JsonNode json = JSON.readTree(res.body());
        assertEquals(code, json.get("code").asString(), res.body());
        assertTrue(json.hasNonNull("traceId"), res.body());
    }

    // ---------- K13 ----------

    @Test
    void K13_예약을_요청하면_201과_Location과_Booking_20개_필드가_온다() throws Exception {
        String roomTypeId = roomTypeOf(HOST);
        LocalDate checkIn = today().plusDays(10);
        prepare(roomTypeId, checkIn, 2, 2);

        HttpResponse<String> res = book(GUEST, newKey(), body(roomTypeId, checkIn, 2, 2));

        assertEquals(201, res.statusCode(), res.body());
        JsonNode json = JSON.readTree(res.body());
        String id = json.get("id").asString();
        assertTrue(id.startsWith("booking_"), id);
        assertEquals(BOOKINGS + "/" + id, res.headers().firstValue("Location").orElse(""));
        assertTrue(res.headers().firstValue("Idempotency-Replayed").isEmpty());

        // 11 응답 모델 Booking의 표는 스무 행이다. 스물한 번째가 있으면 명세와 다르다(BN1)
        assertEquals(20, json.size(), res.body());
        assertEquals(GUEST, json.get("guestId").asString());
        assertEquals(roomTypeId, json.get("roomTypeId").asString());
        assertTrue(json.get("propertyId").asString().startsWith("prop_"));
        assertEquals(checkIn.toString(), json.get("checkIn").asString());
        assertEquals(checkIn.plusDays(2).toString(), json.get("checkOut").asString());
        assertEquals(2, json.get("guestCount").asInt());
        assertEquals("HELD", json.get("status").asString());
        assertEquals(0, json.get("version").asInt());
        for (String nullField : List.of("expirationReason", "cancellationReason", "confirmedAt",
                "canceledAt", "expiredAt")) {
            assertTrue(json.has(nullField) && json.get(nullField).isNull(), nullField);
        }

        // 만료는 생성 시각 더하기 10분이고(08-3 결정 11-3) serverNow는 그 사이의 실제 시각이다
        Instant createdAt = Instant.parse(json.get("createdAt").asString());
        Instant expiresAt = Instant.parse(json.get("expiresAt").asString());
        Instant serverNow = Instant.parse(json.get("serverNow").asString());
        assertEquals(Duration.ofMinutes(10), Duration.between(createdAt, expiresAt));
        assertFalse(serverNow.isBefore(createdAt));
        assertTrue(serverNow.isBefore(expiresAt));
        assertEquals(createdAt, Instant.parse(json.get("updatedAt").asString()));

        JsonNode snapshot = json.get("priceSnapshot");
        assertEquals(6, snapshot.size(), res.body());
        assertEquals("KRW", snapshot.get("currency").asString());
        assertEquals(RATE * 2, snapshot.get("baseTotalAmount").asLong());
        assertEquals(0, snapshot.get("discountTotalAmount").asLong());
        assertEquals(RATE * 2, snapshot.get("totalAmount").asLong());
        assertTrue(snapshot.get("appliedPromotion").isNull());
        JsonNode days = snapshot.get("days");
        assertEquals(2, days.size());
        assertEquals(checkIn.toString(), days.get(0).get("date").asString());
        assertEquals(checkIn.plusDays(1).toString(), days.get(1).get("date").asString());
        assertEquals(4, days.get(0).size());
        assertEquals(RATE, days.get(0).get("baseAmount").asLong());
        assertEquals(0, days.get(0).get("discountAmount").asLong());
        assertEquals(RATE, days.get(0).get("finalAmount").asLong());

        JsonNode payment = json.get("payment");
        assertEquals(4, payment.size(), res.body());
        assertEquals(0, payment.get("attemptCount").asInt());
        assertTrue(payment.get("approvedAttemptId").isNull());
        assertEquals(0, payment.get("attempts").size());
        assertTrue(payment.get("refund").isNull());

        assertEquals(1, heldCount(roomTypeId, checkIn));
        assertEquals(1, heldCount(roomTypeId, checkIn.plusDays(1)));
    }

    // ---------- K14 ----------

    @Test
    void K14_멱등키가_없거나_형식이_틀리면_400_IDEMPOTENCY_KEY_REQUIRED다() throws Exception {
        String roomTypeId = roomTypeOf(HOST);
        LocalDate checkIn = today().plusDays(10);
        prepare(roomTypeId, checkIn, 1, 2);
        String body = body(roomTypeId, checkIn, 1, 1);

        assertError(book(GUEST, null, body), 400, "IDEMPOTENCY_KEY_REQUIRED");
        assertError(book(GUEST, "a".repeat(7), body), 400, "IDEMPOTENCY_KEY_REQUIRED");
        assertError(book(GUEST, "a".repeat(129), body), 400, "IDEMPOTENCY_KEY_REQUIRED");
        assertError(book(GUEST, "key with space", body), 400, "IDEMPOTENCY_KEY_REQUIRED");
        assertEquals(0, heldCount(roomTypeId, checkIn));

        // 경계의 짝. 8자와 128자는 통과다(T2)
        assertEquals(201, book(GUEST, "b".repeat(8), body).statusCode());
        assertEquals(201, book(GUEST, "c".repeat(128), body).statusCode());
    }

    // ---------- K15 ----------

    @Test
    void K15_같은_키_같은_body_재전송은_같은_응답과_Idempotency_Replayed이고_선점이_더_안_오른다()
            throws Exception {
        // T10과 멱등 규칙 3. 재전송 응답은 최초 시점의 스냅샷이라 serverNow까지 같다
        String roomTypeId = roomTypeOf(HOST);
        LocalDate checkIn = today().plusDays(10);
        prepare(roomTypeId, checkIn, 2, 1);
        String key = newKey();
        String body = body(roomTypeId, checkIn, 2, 1);

        HttpResponse<String> first = book(GUEST, key, body);
        HttpResponse<String> replay = book(GUEST, key, body);

        assertEquals(201, first.statusCode(), first.body());
        assertEquals(201, replay.statusCode(), replay.body());
        assertEquals(first.body(), replay.body());
        assertEquals(first.headers().firstValue("Location"), replay.headers().firstValue("Location"));
        assertTrue(first.headers().firstValue("Idempotency-Replayed").isEmpty());
        assertEquals("true", replay.headers().firstValue("Idempotency-Replayed").orElse(""));
        assertEquals(1, heldCount(roomTypeId, checkIn));
        assertEquals(1, heldCount(roomTypeId, checkIn.plusDays(1)));
        assertEquals(1, bookingsOf(GUEST, roomTypeId).size());
    }

    // ---------- K16 ----------

    @Test
    void K16_같은_키_다른_body는_409_IDEMPOTENCY_KEY_REUSED다() throws Exception {
        // T11과 멱등 규칙 2. 인원 하나 차이가 다른 body다
        String roomTypeId = roomTypeOf(HOST);
        LocalDate checkIn = today().plusDays(10);
        prepare(roomTypeId, checkIn, 1, 2);
        String key = newKey();
        assertEquals(201, book(GUEST, key, body(roomTypeId, checkIn, 1, 1)).statusCode());

        HttpResponse<String> res = book(GUEST, key, body(roomTypeId, checkIn, 1, 2));

        assertError(res, 409, "IDEMPOTENCY_KEY_REUSED");
        assertEquals(1, heldCount(roomTypeId, checkIn));
        assertEquals(1, bookingsOf(GUEST, roomTypeId).size());
    }

    @Test
    void K16_키_순서만_다른_body는_같은_body라_재전송이다() throws Exception {
        // 멱등 규칙 2의 대조가 원문이 아니라 정규형이라는 것. 원문 비교면 여기서 409가 난다
        String roomTypeId = roomTypeOf(HOST);
        LocalDate checkIn = today().plusDays(10);
        prepare(roomTypeId, checkIn, 1, 2);
        String key = newKey();
        HttpResponse<String> first = book(GUEST, key, body(roomTypeId, checkIn, 1, 1));
        assertEquals(201, first.statusCode(), first.body());

        String reordered = "{\"currency\":\"KRW\",\"expectedTotalAmount\":" + RATE
                + ",\"guestCount\":1,\"checkOut\":\"" + checkIn.plusDays(1)
                + "\",\"checkIn\":\"" + checkIn + "\",\"roomTypeId\":\"" + roomTypeId + "\"}";
        HttpResponse<String> replay = book(GUEST, key, reordered);

        assertEquals(201, replay.statusCode(), replay.body());
        assertEquals("true", replay.headers().firstValue("Idempotency-Replayed").orElse(""));
        assertEquals(JSON.readTree(first.body()).get("id"), JSON.readTree(replay.body()).get("id"));
        assertEquals(1, heldCount(roomTypeId, checkIn));
    }

    // ---------- K17 ----------

    /**
     * K17. 멱등 규칙 4. 첫 요청이 잠긴 재고 행에 붙잡혀 있는 동안 같은 키가 오면 409
     * REQUEST_IN_PROGRESS와 Retry-After: 1이다. 진행 중 기록이 본 트랜잭션보다 먼저 커밋돼야
     * (REQUIRES_NEW) 두 번째 요청이 그것을 본다. 같은 트랜잭션에 두면 두 번째 요청은 기록을 못
     * 보고 재고 잠금에 같이 붙잡혀 2초 안에 답이 없어 이 테스트가 붉어진다.
     */
    @Test
    void K17_처리_중인_같은_키는_409_REQUEST_IN_PROGRESS이고_잠금이_풀리면_첫_요청은_201이다()
            throws Exception {
        String roomTypeId = roomTypeOf(HOST);
        LocalDate checkIn = today().plusDays(10);
        prepare(roomTypeId, checkIn, 1, 2);
        RoomTypeId id = RoomTypeId.of(roomTypeId);
        String key = newKey();
        String body = body(roomTypeId, checkIn, 1, 1);

        ExecutorService waiter = Executors.newSingleThreadExecutor();
        try {
            Future<HttpResponse<String>> first = new TransactionTemplate(transactionManager)
                    .execute((status) -> {
                        inventoryRepository.findForUpdate(id, checkIn).orElseThrow();
                        Future<HttpResponse<String>> sent = waiter.submit(() -> book(GUEST, key, body));
                        // 잠금을 쥔 동안 첫 요청이 끝나면 재고를 잠그지 않고 선점한 것이다
                        assertThrows(TimeoutException.class, () -> sent.get(2, TimeUnit.SECONDS));

                        HttpResponse<String> second = assertDoesNotThrow(() -> book(GUEST, key, body));
                        assertError(second, 409, "REQUEST_IN_PROGRESS");
                        assertEquals("1", second.headers().firstValue("Retry-After").orElse(""));
                        return sent;
                    });

            HttpResponse<String> res = first.get(30, TimeUnit.SECONDS);
            assertEquals(201, res.statusCode(), res.body());
        } finally {
            waiter.shutdownNow();
        }
        assertEquals(1, heldCount(roomTypeId, checkIn));
        assertEquals(1, bookingsOf(GUEST, roomTypeId).size());
        // 진행 중이던 기록은 첫 요청과 같은 트랜잭션에서 완료로 바뀌었다(규칙 6)
        IdempotencyRecord record = idempotencyRecordRepository.findByScope(
                new IdempotencyScope(GUEST, "POST", BOOKINGS, IdempotencyKey.of(key))).orElseThrow();
        assertTrue(record.isCompleted());
        assertEquals(201, record.responseStatus());
    }

    // ---------- K18 ----------

    @Test
    void K18_거절된_요청은_캐시되지_않고_같은_키로_고친_body가_201이다() throws Exception {
        // 멱등 규칙 7. PRICE_CHANGED로 거절된 뒤 진행 중 기록이 남아 있으면 두 번째가 409다
        String roomTypeId = roomTypeOf(HOST);
        LocalDate checkIn = today().plusDays(10);
        prepare(roomTypeId, checkIn, 2, 2);
        String key = newKey();
        IdempotencyScope scope = new IdempotencyScope(GUEST, "POST", BOOKINGS, IdempotencyKey.of(key));

        HttpResponse<String> rejected = book(GUEST, key,
                body(roomTypeId, checkIn, checkIn.plusDays(2), 1, RATE * 2 - 1));
        assertError(rejected, 409, "PRICE_CHANGED");
        assertTrue(idempotencyRecordRepository.findByScope(scope).isEmpty());
        assertEquals(0, heldCount(roomTypeId, checkIn));

        HttpResponse<String> fixed = book(GUEST, key, body(roomTypeId, checkIn, 2, 1));

        assertEquals(201, fixed.statusCode(), fixed.body());
        assertTrue(fixed.headers().firstValue("Idempotency-Replayed").isEmpty());
        assertTrue(idempotencyRecordRepository.findByScope(scope).orElseThrow().isCompleted());
        assertEquals(1, heldCount(roomTypeId, checkIn));
    }

    // ---------- K19 ----------

    /**
     * K19. T08과 08-3 결정 3. 마지막 객실 하나에 두 손님이 동시에 요청한다. 테스트 트랜잭션이
     * 재고 행을 잠근 채 둘을 보내 둘 다 그 잠금에 붙잡히게 만들고, 잠금을 풀면 MySQL이 둘을
     * 차례로 세운다. 먼저 잡은 쪽이 선점하고 커밋한 뒤 나중 쪽이 잠금을 잡아 가용 0을 읽는다.
     * 잠금 없이 읽는 구현은 둘 다 가용 1을 읽어 둘 다 201이 된다.
     */
    @Test
    void K19_마지막_객실_하나에_두_손님이_동시에_요청하면_하나만_201이다() throws Exception {
        String roomTypeId = roomTypeOf(HOST);
        LocalDate checkIn = today().plusDays(10);
        prepare(roomTypeId, checkIn, 1, 1);
        RoomTypeId id = RoomTypeId.of(roomTypeId);
        String body = body(roomTypeId, checkIn, 1, 1);

        ExecutorService racers = Executors.newFixedThreadPool(2);
        try {
            List<Future<HttpResponse<String>>> sent = new TransactionTemplate(transactionManager)
                    .execute((status) -> {
                        inventoryRepository.findForUpdate(id, checkIn).orElseThrow();
                        Future<HttpResponse<String>> a = racers.submit(() -> book(GUEST, newKey(), body));
                        Future<HttpResponse<String>> b = racers.submit(
                                () -> book(OTHER_GUEST, newKey(), body));
                        // 둘 다 잠금에 붙잡혀 있어야 한다. 하나라도 끝나면 잠금 없이 선점한 것이다
                        assertThrows(TimeoutException.class, () -> a.get(2, TimeUnit.SECONDS));
                        assertThrows(TimeoutException.class, () -> b.get(1, TimeUnit.SECONDS));
                        return List.of(a, b);
                    });

            HttpResponse<String> a = sent.get(0).get(30, TimeUnit.SECONDS);
            HttpResponse<String> b = sent.get(1).get(30, TimeUnit.SECONDS);
            List<Integer> statuses = List.of(a.statusCode(), b.statusCode()).stream().sorted().toList();
            assertEquals(List.of(201, 409), statuses, a.body() + "\n" + b.body());
            HttpResponse<String> loser = a.statusCode() == 409 ? a : b;
            assertError(loser, 409, "INVENTORY_UNAVAILABLE");
        } finally {
            racers.shutdownNow();
        }
        DailyInventory after = inventoryRepository.findByRoomTypeIdAndStayDate(id, checkIn).orElseThrow();
        assertEquals(1, after.heldCount());
        assertEquals(0, after.availableCount());
        assertEquals(1, bookingsOf(GUEST, roomTypeId).size() + bookingsOf(OTHER_GUEST, roomTypeId).size());
    }

    // ---------- K20 ----------

    @Test
    void K20_필드_값이_범위를_벗어나거나_미정의_필드가_있으면_400_INVALID_REQUEST다() throws Exception {
        String roomTypeId = roomTypeOf(HOST);
        LocalDate checkIn = today().plusDays(10);
        LocalDate checkOut = checkIn.plusDays(1);

        assertError(book(GUEST, newKey(), body(roomTypeId, checkIn, checkOut, 0, RATE)),
                400, "INVALID_REQUEST");
        assertError(book(GUEST, newKey(), body(roomTypeId, checkIn, checkOut, 101, RATE)),
                400, "INVALID_REQUEST");
        assertError(book(GUEST, newKey(), body(roomTypeId, checkIn, checkOut, 1, 0)),
                400, "INVALID_REQUEST");
        assertError(book(GUEST, newKey(),
                body(roomTypeId, checkIn, checkOut, 1, RATE).replace("KRW", "USD")),
                400, "INVALID_REQUEST");
        // 공통 절 38행. 명세에 없는 필드는 400이다. 프로모션 ID는 입력받지 않는다(BOOK-01 처리 규칙)
        assertError(book(GUEST, newKey(), body(roomTypeId, checkIn, checkOut, 1, RATE)
                        .replace("}", ",\"promotionId\":\"promo_001\"}")),
                400, "INVALID_REQUEST");
        assertError(book(GUEST, newKey(), "{\"roomTypeId\":\"" + roomTypeId + "\"}"),
                400, "INVALID_REQUEST");
    }

    @Test
    void K20_날짜_순서와_30박과_과거와_형식은_400_INVALID_DATE_RANGE이고_서울_오늘은_통과한다()
            throws Exception {
        String roomTypeId = roomTypeOf(HOST);
        LocalDate checkIn = today().plusDays(10);

        assertError(book(GUEST, newKey(), body(roomTypeId, checkIn, checkIn, 1, RATE)),
                400, "INVALID_DATE_RANGE");
        assertError(book(GUEST, newKey(), body(roomTypeId, checkIn, checkIn.minusDays(1), 1, RATE)),
                400, "INVALID_DATE_RANGE");
        assertError(book(GUEST, newKey(), body(roomTypeId, checkIn, checkIn.plusDays(31), 1, RATE * 31)),
                400, "INVALID_DATE_RANGE");
        assertError(book(GUEST, newKey(), body(roomTypeId, today().minusDays(1), today().plusDays(1), 1,
                RATE * 2)), 400, "INVALID_DATE_RANGE");
        assertError(book(GUEST, newKey(), body(roomTypeId, checkIn, checkIn.plusDays(1), 1, RATE)
                .replace(checkIn.toString(), "2026/10/10")), 400, "INVALID_DATE_RANGE");

        // 거절의 짝. 서울 오늘 1박과 30박은 통과다(T2). 날짜 검사가 재고 검사보다 앞이라 재고가
        // 없어도 400이 났다. 통과 쪽은 재고와 요금을 실제로 연다
        prepare(roomTypeId, today(), 1, 2);
        assertEquals(201, book(GUEST, newKey(), body(roomTypeId, today(), 1, 1)).statusCode());
        String longStay = roomTypeOf(HOST);
        prepare(longStay, checkIn, 30, 1);
        HttpResponse<String> thirty = book(GUEST, newKey(), body(longStay, checkIn, 30, 1));
        assertEquals(201, thirty.statusCode(), thirty.body());
        assertEquals(30, JSON.readTree(thirty.body()).get("priceSnapshot").get("days").size());
    }

    // ---------- K21 ----------

    @Test
    void K21_행위자가_없으면_401이고_HOST가_예약하면_403이다() throws Exception {
        String roomTypeId = roomTypeOf(HOST);
        LocalDate checkIn = today().plusDays(10);
        prepare(roomTypeId, checkIn, 1, 2);
        String body = body(roomTypeId, checkIn, 1, 1);

        assertError(book(null, newKey(), body), 401, "ACTOR_REQUIRED");
        assertError(book("guest_999", newKey(), body), 401, "ACTOR_REQUIRED");
        assertError(book(HOST, newKey(), body), 403, "ACCESS_DENIED");
        // 계약 2절 검사 순서. 행위자(1)가 멱등키 형식(2)보다 앞이다
        assertError(book(null, null, body), 401, "ACTOR_REQUIRED");
        assertEquals(0, heldCount(roomTypeId, checkIn));
    }

    // ---------- K22 ----------

    @Test
    void K22_선점된_날짜의_총량을_선점_아래로_내리면_409_INVENTORY_BELOW_COMMITTED다() throws Exception {
        // T04의 heldCount 몫. 앞 묶음 V1은 판매분을 DB에 직접 놓았고 이번엔 예약이 실제로 만든다
        String roomTypeId = roomTypeOf(HOST);
        LocalDate checkIn = today().plusDays(10);
        prepare(roomTypeId, checkIn, 1, 1);
        assertEquals(201, book(GUEST, newKey(), body(roomTypeId, checkIn, 1, 1)).statusCode());

        HttpResponse<String> res = send("PATCH",
                "/api/v1/room-types/" + roomTypeId + "/inventories/" + checkIn, HOST,
                "{\"version\":0,\"totalCount\":0}");

        assertError(res, 409, "INVENTORY_BELOW_COMMITTED");
        DailyInventory after = inventoryRepository.findByRoomTypeIdAndStayDate(
                RoomTypeId.of(roomTypeId), checkIn).orElseThrow();
        assertEquals(1, after.totalCount());
        assertEquals(1, after.heldCount());
        assertEquals(0L, after.version());
    }

    // ---------- K23 ----------

    @Test
    void K23_없는_객실_타입은_404_RESOURCE_NOT_FOUND다() throws Exception {
        HttpResponse<String> res = book(GUEST, newKey(),
                body("room_없는것", today().plusDays(10), 1, 1));

        assertError(res, 404, "RESOURCE_NOT_FOUND");
        assertEquals(0, bookingsOf(GUEST, "room_없는것").size());
    }

    @Test
    void K23_최대_인원_초과는_409_OCCUPANCY_EXCEEDED다() throws Exception {
        String roomTypeId = roomTypeOf(HOST);
        LocalDate checkIn = today().plusDays(10);
        prepare(roomTypeId, checkIn, 1, 2);

        assertError(book(GUEST, newKey(), body(roomTypeId, checkIn, 1, 3)), 409, "OCCUPANCY_EXCEEDED");

        assertEquals(0, heldCount(roomTypeId, checkIn));
        assertEquals(0, bookingsOf(GUEST, roomTypeId).size());
    }

    @Test
    void K23_요금_없는_날짜는_409_RATE_NOT_CONFIGURED다() throws Exception {
        // T07. 재고는 2박 다 있고 둘째 날 요금이 없다
        String roomTypeId = roomTypeOf(HOST);
        LocalDate checkIn = today().plusDays(10);
        openInventory(roomTypeId, checkIn, 2);
        openInventory(roomTypeId, checkIn.plusDays(1), 2);
        registerRate(roomTypeId, checkIn, RATE);

        assertError(book(GUEST, newKey(), body(roomTypeId, checkIn, 2, 1)), 409, "RATE_NOT_CONFIGURED");

        assertEquals(0, heldCount(roomTypeId, checkIn));
        assertEquals(0, heldCount(roomTypeId, checkIn.plusDays(1)));
        assertEquals(0, bookingsOf(GUEST, roomTypeId).size());
    }

    @Test
    void K23_재고_행_없는_날짜는_409_INVENTORY_NOT_CONFIGURED다() throws Exception {
        // T07. 요금은 2박 다 있고 둘째 날 재고 행이 없다
        String roomTypeId = roomTypeOf(HOST);
        LocalDate checkIn = today().plusDays(10);
        openInventory(roomTypeId, checkIn, 2);
        registerRate(roomTypeId, checkIn, RATE);
        registerRate(roomTypeId, checkIn.plusDays(1), RATE);

        assertError(book(GUEST, newKey(), body(roomTypeId, checkIn, 2, 1)),
                409, "INVENTORY_NOT_CONFIGURED");

        assertEquals(0, heldCount(roomTypeId, checkIn));
        assertEquals(0, bookingsOf(GUEST, roomTypeId).size());
    }

    @Test
    void K23_체크아웃_날짜에는_재고와_요금이_없어도_201이다() throws Exception {
        // T06. 위 둘의 짝. 끝 날짜는 숙박 대상이 아니다
        String roomTypeId = roomTypeOf(HOST);
        LocalDate checkIn = today().plusDays(10);
        prepare(roomTypeId, checkIn, 1, 2);

        HttpResponse<String> res = book(GUEST, newKey(), body(roomTypeId, checkIn, 1, 1));

        assertEquals(201, res.statusCode(), res.body());
        assertEquals(1, heldCount(roomTypeId, checkIn));
        assertTrue(inventoryRepository.findByRoomTypeIdAndStayDate(
                RoomTypeId.of(roomTypeId), checkIn.plusDays(1)).isEmpty());
    }
}
