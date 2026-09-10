package com.o2o.inventory.api;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Date;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;

import com.o2o.inventory.domain.DailyInventoryRepository;
import com.o2o.shared.RoomTypeId;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 8-1절 V5와 V6과 V7과 V8과 V9, 그리고 INV-01과 INV-02와 INV-03과 RATE-01과 RATE-02의
 * 상태 코드와 오류 코드. 설계 근거: 11 재고 절, 11 요금 절, 11 에러 응답 표, T03과 T04와 T05.
 *
 * CatalogApiTest와 같은 방식이다. 진짜 포트를 열고 JDK HttpClient로 친다. 계약 8절 6단계의
 * 완료 조건이 로컬 HTTP 호출 결과라서다.
 *
 * 날짜를 상수로 박지 않고 오늘에서 센다. 앱 서비스의 과거 날짜 검사가 서버의 오늘을 보므로
 * 날짜를 박아 두면 그 날이 지나는 순간 테스트가 다른 것을 검사한다. 단위 테스트는 Clock을
 * 고정해서 막았는데 여기는 서버가 다른 스레드에서 돌아 그 방법이 안 맞는다. 그래서 상대
 * 날짜를 쓴다. 과거 날짜 검사인 V9는 반대로 오늘에서 하루를 뺀다.
 *
 * 테스트마다 숙소와 객실 타입을 새로 만든다. 트랜잭션 롤백을 쓸 수 없어 데이터가 남기
 * 때문이다. 같은 객실 타입을 쓰면 앞 테스트가 만든 날짜와 겹쳐 유일성 검사가 엉뚱하게 걸린다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class InventoryApiTest {

    private static final String HOST = "host_001";
    private static final String OTHER_HOST = "host_002";
    private static final String GUEST = "guest_001";
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).build();

    @LocalServerPort
    private int port;

    @Autowired
    private DailyInventoryRepository inventoryRepository;

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

    /** 소유자가 host인 객실 타입 하나를 만든다. 반환은 roomTypeId */
    private String roomTypeOf(String host) throws Exception {
        String propertyBody = """
                {"name":"재고 테스트 스테이","regionCode":"SEOUL","address":"서울특별시 중구 예시로 3","description":""}
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

    private HttpResponse<String> registerInventory(String roomTypeId, LocalDate date, int total)
            throws Exception {
        return send("POST", "/api/v1/room-types/" + roomTypeId + "/inventories", HOST,
                "{\"date\":\"" + date + "\",\"totalCount\":" + total + "}");
    }

    private HttpResponse<String> registerBulk(String roomTypeId, LocalDate from, LocalDate to,
                                              int total) throws Exception {
        return send("POST", "/api/v1/room-types/" + roomTypeId + "/inventories/bulk", HOST,
                "{\"from\":\"" + from + "\",\"to\":\"" + to + "\",\"totalCount\":" + total + "}");
    }

    private HttpResponse<String> adjustInventory(String roomTypeId, LocalDate date, long version,
                                                 int total) throws Exception {
        return send("PATCH", "/api/v1/room-types/" + roomTypeId + "/inventories/" + date, HOST,
                "{\"version\":" + version + ",\"totalCount\":" + total + "}");
    }

    private HttpResponse<String> registerRate(String roomTypeId, LocalDate date, long amount,
                                              String currency) throws Exception {
        return send("POST", "/api/v1/room-types/" + roomTypeId + "/rates", HOST,
                "{\"date\":\"" + date + "\",\"amount\":" + amount
                        + ",\"currency\":\"" + currency + "\"}");
    }

    private HttpResponse<String> adjustRate(String roomTypeId, LocalDate date, long version,
                                            long amount) throws Exception {
        return send("PATCH", "/api/v1/room-types/" + roomTypeId + "/rates/" + date, HOST,
                "{\"version\":" + version + ",\"amount\":" + amount + "}");
    }

    @Test
    void INV_01_재고를_등록하면_201과_Location과_일곱_필드가_온다() throws Exception {
        String roomTypeId = roomTypeOf(HOST);
        LocalDate date = today().plusDays(10);

        HttpResponse<String> res = registerInventory(roomTypeId, date, 5);

        assertEquals(201, res.statusCode(), res.body());
        assertEquals("/api/v1/room-types/" + roomTypeId + "/inventories/" + date,
                res.headers().firstValue("Location").orElse(""));
        JsonNode body = JSON.readTree(res.body());
        // 11 응답 모델 DailyInventory의 일곱 필드다. 여덟 번째가 있으면 명세와 다르다
        assertEquals(7, body.size(), res.body());
        assertEquals(5, body.get("totalCount").asInt());
        assertEquals(0, body.get("heldCount").asInt());
        assertEquals(0, body.get("soldCount").asInt());
        assertEquals(5, body.get("availableCount").asInt());
        assertEquals(0, body.get("version").asInt());
        assertEquals(date.toString(), body.get("date").asString());
    }

    @Test
    void INV_01_총_수량_0도_등록된다() throws Exception {
        // 11 명세의 제약이 0~100,000이다. 요금의 하한 1과 다른 자리라 짝으로 둔다
        String roomTypeId = roomTypeOf(HOST);

        HttpResponse<String> res = registerInventory(roomTypeId, today().plusDays(10), 0);

        assertEquals(201, res.statusCode(), res.body());
        assertEquals(0, JSON.readTree(res.body()).get("availableCount").asInt());
    }

    @Test
    void INV_01_같은_날짜를_두_번_등록하면_409다() throws Exception {
        String roomTypeId = roomTypeOf(HOST);
        LocalDate date = today().plusDays(10);
        assertEquals(201, registerInventory(roomTypeId, date, 5).statusCode());

        HttpResponse<String> res = registerInventory(roomTypeId, date, 9);

        assertEquals(409, res.statusCode(), res.body());
        assertTrue(res.body().contains("RESOURCE_ALREADY_EXISTS"), res.body());
    }

    @Test
    void INV_02_기간을_일괄_등록하면_201이고_끝_날짜는_빠진다() throws Exception {
        String roomTypeId = roomTypeOf(HOST);
        LocalDate from = today().plusDays(10);
        LocalDate to = from.plusDays(3);

        HttpResponse<String> res = registerBulk(roomTypeId, from, to, 5);

        assertEquals(201, res.statusCode(), res.body());
        JsonNode body = JSON.readTree(res.body());
        // to를 제외하므로 세 행이다. 11 INV-02의 제약 열이 끝 날짜 제외라고 적는다
        assertEquals(3, body.get("items").size(), res.body());
        assertEquals(0, body.get("missingDates").size());
        assertEquals(from.toString(), body.get("items").get(0).get("date").asString());
        assertEquals(to.minusDays(1).toString(), body.get("items").get(2).get("date").asString());
    }

    @Test
    void V5_일괄_등록은_한_날짜라도_겹치면_전부_실패한다() throws Exception {
        // T03. 11 INV-02의 처리 규칙이 새 날짜만 부분 등록하지 않는다고 적는다
        String roomTypeId = roomTypeOf(HOST);
        LocalDate from = today().plusDays(10);
        LocalDate middle = from.plusDays(1);
        assertEquals(201, registerInventory(roomTypeId, middle, 5).statusCode());

        HttpResponse<String> res = registerBulk(roomTypeId, from, from.plusDays(3), 7);

        assertEquals(409, res.statusCode(), res.body());
        assertTrue(res.body().contains("RESOURCE_ALREADY_EXISTS"), res.body());
        // 겹치지 않은 두 날짜가 남으면 부분 등록이 된 것이다. 이것이 이 테스트의 핵심이다
        RoomTypeId id = RoomTypeId.of(roomTypeId);
        assertFalse(inventoryRepository.findByRoomTypeIdAndStayDate(id, from).isPresent(),
                "겹치지 않은 첫 날짜가 남았다. 부분 등록이다");
        assertFalse(inventoryRepository.findByRoomTypeIdAndStayDate(id, from.plusDays(2)).isPresent(),
                "겹치지 않은 마지막 날짜가 남았다. 부분 등록이다");
        // 원래 있던 날짜는 그대로여야 한다. 롤백이 남의 행까지 건드리면 안 된다
        assertEquals(5, inventoryRepository.findByRoomTypeIdAndStayDate(id, middle)
                .orElseThrow().totalCount());
    }

    @Test
    void V8_기간이_366일을_넘으면_400이고_366일은_등록된다() throws Exception {
        // 11 명세 137행의 최대 366일. 넘는 쪽과 경계 쪽을 짝으로 본다
        String roomTypeId = roomTypeOf(HOST);
        LocalDate from = today().plusDays(1);

        HttpResponse<String> tooLong = registerBulk(roomTypeId, from, from.plusDays(367), 1);
        assertEquals(400, tooLong.statusCode(), tooLong.body());
        assertTrue(tooLong.body().contains("INVALID_DATE_RANGE"), tooLong.body());

        HttpResponse<String> exact = registerBulk(roomTypeId, from, from.plusDays(366), 1);
        assertEquals(201, exact.statusCode(), exact.body());
        assertEquals(366, JSON.readTree(exact.body()).get("items").size());
    }

    @Test
    void V8_from이_to보다_뒤면_400이다() throws Exception {
        String roomTypeId = roomTypeOf(HOST);
        LocalDate from = today().plusDays(10);

        HttpResponse<String> res = registerBulk(roomTypeId, from, from.minusDays(1), 5);

        assertEquals(400, res.statusCode(), res.body());
        assertTrue(res.body().contains("INVALID_DATE_RANGE"), res.body());
    }

    @Test
    void V9_과거_날짜는_등록도_수정도_400이다() throws Exception {
        // 11 명세 198행과 259행. 등록일과 수정 날짜는 서버의 오늘 이상이다
        String roomTypeId = roomTypeOf(HOST);
        LocalDate yesterday = today().minusDays(1);

        HttpResponse<String> register = registerInventory(roomTypeId, yesterday, 5);
        assertEquals(400, register.statusCode(), register.body());
        assertTrue(register.body().contains("INVALID_DATE_RANGE"), register.body());

        HttpResponse<String> adjust = adjustInventory(roomTypeId, yesterday, 0, 5);
        assertEquals(400, adjust.statusCode(), adjust.body());
        assertTrue(adjust.body().contains("INVALID_DATE_RANGE"), adjust.body());

        HttpResponse<String> rate = registerRate(roomTypeId, yesterday, 100_000, "KRW");
        assertEquals(400, rate.statusCode(), rate.body());
        assertTrue(rate.body().contains("INVALID_DATE_RANGE"), rate.body());
    }

    @Test
    void V9_오늘은_등록된다() throws Exception {
        // 과거 거절의 짝이다. 오늘 이상이라 오늘 자신은 허용 값이다
        String roomTypeId = roomTypeOf(HOST);

        HttpResponse<String> res = registerInventory(roomTypeId, today(), 5);

        assertEquals(201, res.statusCode(), res.body());
    }

    @Test
    void INV_03_수정하면_200이고_version이_오른다() throws Exception {
        String roomTypeId = roomTypeOf(HOST);
        LocalDate date = today().plusDays(10);
        assertEquals(201, registerInventory(roomTypeId, date, 5).statusCode());

        HttpResponse<String> res = adjustInventory(roomTypeId, date, 0, 8);

        assertEquals(200, res.statusCode(), res.body());
        JsonNode body = JSON.readTree(res.body());
        assertEquals(8, body.get("totalCount").asInt());
        assertEquals(8, body.get("availableCount").asInt());
        assertEquals(1, body.get("version").asInt());
    }

    @Test
    void V6_지난_version으로_수정하면_409이고_최근_변경이_남는다() throws Exception {
        // T05. 11 공통 요청과 응답 규칙이 불일치하면 적용하지 않는다고 적는다
        String roomTypeId = roomTypeOf(HOST);
        LocalDate date = today().plusDays(10);
        assertEquals(201, registerInventory(roomTypeId, date, 5).statusCode());
        assertEquals(200, adjustInventory(roomTypeId, date, 0, 8).statusCode());

        HttpResponse<String> res = adjustInventory(roomTypeId, date, 0, 3);

        assertEquals(409, res.statusCode(), res.body());
        assertTrue(res.body().contains("VERSION_CONFLICT"), res.body());
        // 적용하지 않는다는 것이 요점이다. 8이 그대로 남아야 한다
        assertEquals(8, inventoryRepository
                .findByRoomTypeIdAndStayDate(RoomTypeId.of(roomTypeId), date)
                .orElseThrow().totalCount());
    }

    @Test
    void INV_03_판매분_아래로_수정하면_409_INVENTORY_BELOW_COMMITTED다() throws Exception {
        // T04. 판매 수를 올리는 코드가 이번 묶음에 없어 상태를 DB에 직접 놓는다.
        // 근거는 InventoryApplicationServiceTest 머리 주석과 같다
        String roomTypeId = roomTypeOf(HOST);
        LocalDate date = today().plusDays(10);
        assertEquals(201, registerInventory(roomTypeId, date, 5).statusCode());
        jdbcTemplate.update(
                "update daily_inventory set sold_count = 3 where room_type_id = ? and stay_date = ?",
                roomTypeId, Date.valueOf(date));

        HttpResponse<String> res = adjustInventory(roomTypeId, date, 0, 2);

        assertEquals(409, res.statusCode(), res.body());
        assertTrue(res.body().contains("INVENTORY_BELOW_COMMITTED"), res.body());
    }

    @Test
    void INV_03_없는_날짜를_수정하면_404다() throws Exception {
        String roomTypeId = roomTypeOf(HOST);

        HttpResponse<String> res = adjustInventory(roomTypeId, today().plusDays(10), 0, 5);

        assertEquals(404, res.statusCode(), res.body());
        assertTrue(res.body().contains("RESOURCE_NOT_FOUND"), res.body());
    }

    @Test
    void V7_남의_객실_타입에_등록하면_404다() throws Exception {
        // T02. 11 인증과 접근 제어가 다른 사용자 소유 자원을 404로 적는다. 403이면 존재가 샌다
        String otherRoomTypeId = roomTypeOf(OTHER_HOST);

        HttpResponse<String> res = registerInventory(otherRoomTypeId, today().plusDays(10), 5);

        assertEquals(404, res.statusCode(), res.body());
        assertTrue(res.body().contains("RESOURCE_NOT_FOUND"), res.body());
    }

    @Test
    void V7_없는_객실_타입도_같은_404다() throws Exception {
        HttpResponse<String> res = registerInventory("room_없는것", today().plusDays(10), 5);

        assertEquals(404, res.statusCode(), res.body());
        assertTrue(res.body().contains("RESOURCE_NOT_FOUND"), res.body());
    }

    @Test
    void RATE_01_요금을_등록하면_201과_Location과_다섯_필드가_온다() throws Exception {
        String roomTypeId = roomTypeOf(HOST);
        LocalDate date = today().plusDays(10);

        HttpResponse<String> res = registerRate(roomTypeId, date, 100_000, "KRW");

        assertEquals(201, res.statusCode(), res.body());
        assertEquals("/api/v1/room-types/" + roomTypeId + "/rates/" + date,
                res.headers().firstValue("Location").orElse(""));
        JsonNode body = JSON.readTree(res.body());
        assertEquals(5, body.size(), res.body());
        assertEquals(100_000L, body.get("amount").asLong());
        assertEquals("KRW", body.get("currency").asString());
        assertEquals(0, body.get("version").asInt());
    }

    @Test
    void RATE_01_통화가_KRW가_아니면_400이다() throws Exception {
        String roomTypeId = roomTypeOf(HOST);

        HttpResponse<String> res = registerRate(roomTypeId, today().plusDays(10), 100_000, "USD");

        assertEquals(400, res.statusCode(), res.body());
        assertTrue(res.body().contains("INVALID_REQUEST"), res.body());
    }

    @Test
    void RATE_01_같은_날짜를_두_번_등록하면_409다() throws Exception {
        String roomTypeId = roomTypeOf(HOST);
        LocalDate date = today().plusDays(10);
        assertEquals(201, registerRate(roomTypeId, date, 100_000, "KRW").statusCode());

        HttpResponse<String> res = registerRate(roomTypeId, date, 120_000, "KRW");

        assertEquals(409, res.statusCode(), res.body());
        assertTrue(res.body().contains("RESOURCE_ALREADY_EXISTS"), res.body());
    }

    @Test
    void RATE_02_수정하면_200이고_통화는_그대로다() throws Exception {
        String roomTypeId = roomTypeOf(HOST);
        LocalDate date = today().plusDays(10);
        assertEquals(201, registerRate(roomTypeId, date, 100_000, "KRW").statusCode());

        HttpResponse<String> res = adjustRate(roomTypeId, date, 0, 110_000);

        assertEquals(200, res.statusCode(), res.body());
        JsonNode body = JSON.readTree(res.body());
        assertEquals(110_000L, body.get("amount").asLong());
        assertEquals("KRW", body.get("currency").asString());
        assertEquals(1, body.get("version").asInt());
    }

    @Test
    void V6_요금도_지난_version이면_409다() throws Exception {
        String roomTypeId = roomTypeOf(HOST);
        LocalDate date = today().plusDays(10);
        assertEquals(201, registerRate(roomTypeId, date, 100_000, "KRW").statusCode());
        assertEquals(200, adjustRate(roomTypeId, date, 0, 110_000).statusCode());

        HttpResponse<String> res = adjustRate(roomTypeId, date, 0, 120_000);

        assertEquals(409, res.statusCode(), res.body());
        assertTrue(res.body().contains("VERSION_CONFLICT"), res.body());
    }

    @Test
    void 날짜_형식이_틀리면_400_INVALID_DATE_RANGE다() throws Exception {
        // 11 에러 응답 표가 날짜 형식 오류를 INVALID_REQUEST가 아니라 이 코드로 적는다
        String roomTypeId = roomTypeOf(HOST);

        HttpResponse<String> res = send("POST", "/api/v1/room-types/" + roomTypeId + "/inventories",
                HOST, "{\"date\":\"2026-13-99\",\"totalCount\":5}");

        assertEquals(400, res.statusCode(), res.body());
        assertTrue(res.body().contains("INVALID_DATE_RANGE"), res.body());
    }

    @Test
    void 행위자가_없으면_401이고_GUEST면_403이다() throws Exception {
        // 11 인증과 접근 제어. 다섯 경로가 전부 HOST 전용이다
        String roomTypeId = roomTypeOf(HOST);
        String body = "{\"date\":\"" + today().plusDays(10) + "\",\"totalCount\":5}";
        String path = "/api/v1/room-types/" + roomTypeId + "/inventories";

        HttpResponse<String> anonymous = send("POST", path, null, body);
        assertEquals(401, anonymous.statusCode(), anonymous.body());
        assertTrue(anonymous.body().contains("ACTOR_REQUIRED"), anonymous.body());

        HttpResponse<String> guest = send("POST", path, GUEST, body);
        assertEquals(403, guest.statusCode(), guest.body());
        assertTrue(guest.body().contains("ACCESS_DENIED"), guest.body());
    }
}
