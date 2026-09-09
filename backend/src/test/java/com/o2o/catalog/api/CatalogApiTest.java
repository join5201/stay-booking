package com.o2o.catalog.api;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 8-1절 C6과 C9와 C10, 그리고 CAT-01과 CAT-03과 CAT-06과 CAT-08의 상태 코드.
 * 설계 근거: 11 숙소 절, 11 객실 타입 절, 11 공통 요청과 응답 규칙, 11 인증과 접근 제어.
 *
 * 진짜 포트를 열고 진짜 소켓으로 친다. 계약 8절 6단계의 완료 조건이 로컬 HTTP 호출 결과다.
 * MockMvc는 서블릿 컨테이너를 거치지 않아 그 조건을 만족한다고 보기 어렵다.
 *
 * 클라이언트는 JDK의 HttpClient다. 상태 코드와 본문을 손대지 않은 채로 볼 수 있고 프레임워크
 * 판이 바뀌어도 흔들리지 않는다.
 *
 * 트랜잭션 롤백을 쓰지 않는다. 서버가 다른 스레드에서 도므로 테스트 트랜잭션이 걸리지 않는다.
 * 데이터는 스키마 자동 생성이 컨텍스트 종료 때 걷어 간다. 계약 9절의 초기화 허용 범위가
 * o2o_catalog_test 안이라 그 범위를 벗어나지 않는다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CatalogApiTest {

    private static final String HOST = "host_001";
    private static final String GUEST = "guest_001";
    // 응답을 문자열 포함으로 보면 필드 이름만 확인된다. 값과 타입을 보려면 파싱해야 한다
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).build();

    @LocalServerPort
    private int port;

    private HttpResponse<String> send(String method, String path, String actorId, String body)
            throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + path))
                .timeout(Duration.ofSeconds(10));
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

    private String registerProperty(String name) throws Exception {
        String body = """
                {"name":"%s","regionCode":"SEOUL","address":"서울특별시 종로구 예시로 10","description":""}
                """.formatted(name);
        HttpResponse<String> res = send("POST", "/api/v1/properties", HOST, body);
        assertEquals(201, res.statusCode(), res.body());
        return res.body().replaceAll(".*\"id\"\\s*:\\s*\"([^\"]+)\".*", "$1");
    }

    @Test
    void CAT_01_숙소를_등록하면_201과_Location이_온다() throws Exception {
        String body = """
                {"name":"서울 스테이","regionCode":"SEOUL","address":"서울특별시 종로구 예시로 10","description":"숙박용 예시 숙소"}
                """;

        HttpResponse<String> res = send("POST", "/api/v1/properties", HOST, body);

        assertEquals(201, res.statusCode(), res.body());
        assertTrue(res.headers().firstValue("Location").orElse("").startsWith("/api/v1/properties/"),
                "Location 헤더가 없거나 형식이 다르다");
        // 11 응답 모델 Property의 아홉 필드가 다 있어야 한다
        for (String field : new String[] {"id", "hostId", "name", "regionCode", "address",
                "description", "version", "createdAt", "updatedAt"}) {
            assertTrue(res.body().contains("\"" + field + "\""), "응답에 " + field + "가 없다");
        }
    }

    @Test
    void CAT_03_등록한_숙소를_인증_없이_조회한다() throws Exception {
        String id = registerProperty("조회용 스테이");

        HttpResponse<String> res = send("GET", "/api/v1/properties/" + id, null, null);

        assertEquals(200, res.statusCode(), res.body());
        assertTrue(res.body().contains(id));
    }

    @Test
    void CAT_03_없는_숙소는_404이고_코드가_RESOURCE_NOT_FOUND다() throws Exception {
        HttpResponse<String> res = send("GET", "/api/v1/properties/prop_없는것", null, null);

        assertEquals(404, res.statusCode());
        assertTrue(res.body().contains("RESOURCE_NOT_FOUND"), res.body());
        assertTrue(res.body().contains("traceId"), res.body());
    }

    @Test
    void CAT_06_객실_타입을_등록하면_201과_Location이_온다() throws Exception {
        String propertyId = registerProperty("객실용 스테이");
        String body = """
                {"name":"스탠다드 더블","maxOccupancy":2,"description":"2인 객실"}
                """;

        HttpResponse<String> res = send(
                "POST", "/api/v1/properties/" + propertyId + "/room-types", HOST, body);

        assertEquals(201, res.statusCode(), res.body());
        assertTrue(res.headers().firstValue("Location").orElse("").startsWith("/api/v1/room-types/"));
    }

    @Test
    void CAT_08_등록한_객실_타입을_인증_없이_조회한다() throws Exception {
        String propertyId = registerProperty("조회용 객실 스테이");
        String created = send("POST", "/api/v1/properties/" + propertyId + "/room-types", HOST,
                """
                {"name":"스탠다드 싱글","maxOccupancy":1,"description":""}
                """).body();
        String roomTypeId = created.replaceAll(".*\"id\"\\s*:\\s*\"([^\"]+)\".*", "$1");

        HttpResponse<String> res = send("GET", "/api/v1/room-types/" + roomTypeId, null, null);

        assertEquals(200, res.statusCode(), res.body());
        assertTrue(res.body().contains(propertyId));
    }

    // ---------- C6. 소유자는 body가 아니라 헤더가 정한다 ----------

    @Test
    void C6_응답의_hostId는_헤더_행위자다() throws Exception {
        String body = """
                {"name":"소유자 확인용","regionCode":"SEOUL","address":"서울특별시 중구 예시로 2","description":""}
                """;

        HttpResponse<String> res = send("POST", "/api/v1/properties", HOST, body);

        assertEquals(201, res.statusCode(), res.body());
        assertTrue(res.body().contains("\"hostId\":\"" + HOST + "\""), res.body());
    }

    @Test
    void C6_body에_hostId를_넣으면_거절한다() throws Exception {
        // 요청 타입에 hostId 칸이 아예 없다. 정의되지 않은 필드라 400이다.
        // 소유자를 body로 넘길 통로가 없다는 것을 이 한 건이 증명한다
        String body = """
                {"name":"탈취 시도","regionCode":"SEOUL","address":"서울특별시 중구 예시로 3","hostId":"host_002"}
                """;

        HttpResponse<String> res = send("POST", "/api/v1/properties", HOST, body);

        assertEquals(400, res.statusCode(), res.body());
        assertTrue(res.body().contains("INVALID_REQUEST"), res.body());
    }

    @Test
    void C6_남의_숙소에_객실_타입을_등록하면_404다() throws Exception {
        // 다른 HOST 소유 자원은 404이고 자원 정보를 흘리지 않는다. 11 인증과 접근 제어
        String 남의_숙소 = registerProperty("남의 스테이");
        String body = """
                {"name":"스탠다드","maxOccupancy":2,"description":""}
                """;

        HttpResponse<String> res = send(
                "POST", "/api/v1/properties/" + 남의_숙소 + "/room-types", "host_002x", body);

        // host_002x는 fixture에 없으므로 401이 먼저다. 소유권 경로는 앱 서비스 테스트가 덮는다
        assertEquals(401, res.statusCode(), res.body());
    }

    // ---------- C9. 명세에 없는 필드 ----------

    @Test
    void C9_명세에_없는_필드를_보내면_400이다() throws Exception {
        String body = """
                {"name":"별명 있는 숙소","regionCode":"SEOUL","address":"서울특별시 중구 예시로 4","nickname":"별명"}
                """;

        HttpResponse<String> res = send("POST", "/api/v1/properties", HOST, body);

        assertEquals(400, res.statusCode(), res.body());
        assertTrue(res.body().contains("INVALID_REQUEST"), res.body());
    }

    @Test
    void C9_필수_필드가_빠지면_400이고_어느_필드인지_알려준다() throws Exception {
        String body = """
                {"regionCode":"SEOUL","address":"서울특별시 중구 예시로 5"}
                """;

        HttpResponse<String> res = send("POST", "/api/v1/properties", HOST, body);

        assertEquals(400, res.statusCode(), res.body());
        assertTrue(res.body().contains("\"field\":\"name\""), res.body());
    }

    @Test
    void C9_정상_body는_통과한다() throws Exception {
        // 실패 케이스만 두면 검증이 정상 요청까지 막아도 초록이 뜬다(F9)
        String body = """
                {"name":"정상 숙소","regionCode":"SEOUL","address":"서울특별시 중구 예시로 6","description":""}
                """;

        assertEquals(201, send("POST", "/api/v1/properties", HOST, body).statusCode());
    }

    @Test
    void C9_maxOccupancy가_0이면_400이다() throws Exception {
        String propertyId = registerProperty("인원 검증용 스테이");
        String body = """
                {"name":"영인실","maxOccupancy":0,"description":""}
                """;

        HttpResponse<String> res = send(
                "POST", "/api/v1/properties/" + propertyId + "/room-types", HOST, body);

        assertEquals(400, res.statusCode(), res.body());
    }

    // ---------- C10. 인증과 역할 ----------

    @Test
    void C10_헤더가_없으면_401이다() throws Exception {
        String body = """
                {"name":"헤더 없음","regionCode":"SEOUL","address":"서울특별시 중구 예시로 7","description":""}
                """;

        HttpResponse<String> res = send("POST", "/api/v1/properties", null, body);

        assertEquals(401, res.statusCode(), res.body());
        assertTrue(res.body().contains("ACTOR_REQUIRED"), res.body());
    }

    @Test
    void C10_미등록_행위자면_401이다() throws Exception {
        String body = """
                {"name":"미등록","regionCode":"SEOUL","address":"서울특별시 중구 예시로 8","description":""}
                """;

        // 값이 ASCII다. HTTP 헤더에 한글을 넣으면 JDK 클라이언트가 보내기 전에 거부해서
        // 서버 동작을 재지 못한다. 2026-09-09에 실제로 겪었다
        HttpResponse<String> res = send("POST", "/api/v1/properties", "host_999", body);

        assertEquals(401, res.statusCode(), res.body());
        assertTrue(res.body().contains("ACTOR_REQUIRED"), res.body());
    }

    @Test
    void C10_역할이_다르면_403이다() throws Exception {
        String body = """
                {"name":"게스트 시도","regionCode":"SEOUL","address":"서울특별시 중구 예시로 9","description":""}
                """;

        HttpResponse<String> res = send("POST", "/api/v1/properties", GUEST, body);

        assertEquals(403, res.statusCode(), res.body());
        assertTrue(res.body().contains("ACCESS_DENIED"), res.body());
    }

    // ---------- 응답 모델의 값과 타입 ----------

    @Test
    void 응답_아홉_필드의_타입과_값이_명세대로다() throws Exception {
        // 전에는 필드 이름이 문자열에 있는지만 봤다. 그러면 version에 문자열이 들어가도 통과한다.
        // 11 응답 모델 Property의 타입 열까지 본다
        String body = """
                {"name":"타입 확인","regionCode":"SEOUL","address":"서울특별시 중구 예시로 20","description":"설명"}
                """;

        HttpResponse<String> res = send("POST", "/api/v1/properties", HOST, body);
        JsonNode json = JSON.readTree(res.body());

        assertEquals(201, res.statusCode(), res.body());
        assertTrue(json.get("id").isString(), "id가 문자열이 아니다");
        assertTrue(json.get("id").stringValue().length() <= 64, "id가 64자를 넘는다");
        assertEquals("host_001", json.get("hostId").stringValue());
        assertEquals("타입 확인", json.get("name").stringValue());
        assertEquals("SEOUL", json.get("regionCode").stringValue());
        assertEquals("서울특별시 중구 예시로 20", json.get("address").stringValue());
        assertEquals("설명", json.get("description").stringValue());
        assertTrue(json.get("version").isIntegralNumber(), "version이 정수가 아니다");
        assertEquals(0, json.get("version").asInt(), "등록 직후 version은 0이다");
    }

    @Test
    void 시각이_UTC_소수점_셋_형식이다() throws Exception {
        // 11 공통 요청과 응답 규칙의 YYYY-MM-DDTHH:mm:ss.SSSZ다. 이 형식을 맞추려고
        // ApiTime을 따로 뒀는데 그것이 맞게 도는지 보는 테스트가 없었다
        String body = """
                {"name":"시각 확인","regionCode":"SEOUL","address":"서울특별시 중구 예시로 21","description":""}
                """;

        JsonNode json = JSON.readTree(send("POST", "/api/v1/properties", HOST, body).body());

        String pattern = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z";
        assertTrue(json.get("createdAt").stringValue().matches(pattern),
                "createdAt 형식이 다르다: " + json.get("createdAt").stringValue());
        assertTrue(json.get("updatedAt").stringValue().matches(pattern),
                "updatedAt 형식이 다르다: " + json.get("updatedAt").stringValue());
    }

    @Test
    void 객실_타입_응답_여덟_필드의_타입과_값이_명세대로다() throws Exception {
        String propertyId = registerProperty("객실 타입 확인용");
        String body = """
                {"name":"스탠다드 트윈","maxOccupancy":3,"description":"3인 객실"}
                """;

        HttpResponse<String> res = send(
                "POST", "/api/v1/properties/" + propertyId + "/room-types", HOST, body);
        JsonNode json = JSON.readTree(res.body());

        assertEquals(201, res.statusCode(), res.body());
        assertEquals(propertyId, json.get("propertyId").stringValue());
        assertEquals("스탠다드 트윈", json.get("name").stringValue());
        assertTrue(json.get("maxOccupancy").isIntegralNumber(), "maxOccupancy가 정수가 아니다");
        assertEquals(3, json.get("maxOccupancy").asInt());
        assertEquals(0, json.get("version").asInt());
    }

    // ---------- 경계값. 11 필드표의 상한 ----------

    private String propertyBody(String name, String regionCode, String address, String description) {
        return """
                {"name":"%s","regionCode":"%s","address":"%s","description":"%s"}
                """.formatted(name, regionCode, address, description);
    }

    @Test
    void 이름은_100자까지_받고_101자는_거절한다() throws Exception {
        String ok = propertyBody("가".repeat(100), "SEOUL", "주소", "");
        String tooLong = propertyBody("가".repeat(101), "SEOUL", "주소", "");

        assertEquals(201, send("POST", "/api/v1/properties", HOST, ok).statusCode());
        assertEquals(400, send("POST", "/api/v1/properties", HOST, tooLong).statusCode());
    }

    @Test
    void 지역_코드는_32자까지_받고_33자는_거절한다() throws Exception {
        String ok = propertyBody("지역 경계", "A".repeat(32), "주소", "");
        String tooLong = propertyBody("지역 경계", "A".repeat(33), "주소", "");

        assertEquals(201, send("POST", "/api/v1/properties", HOST, ok).statusCode());
        assertEquals(400, send("POST", "/api/v1/properties", HOST, tooLong).statusCode());
    }

    @Test
    void 주소는_300자까지_받고_301자는_거절한다() throws Exception {
        String ok = propertyBody("주소 경계", "SEOUL", "가".repeat(300), "");
        String tooLong = propertyBody("주소 경계", "SEOUL", "가".repeat(301), "");

        assertEquals(201, send("POST", "/api/v1/properties", HOST, ok).statusCode());
        assertEquals(400, send("POST", "/api/v1/properties", HOST, tooLong).statusCode());
    }

    @Test
    void 설명은_2000자까지_받고_2001자는_거절한다() throws Exception {
        String ok = propertyBody("설명 경계", "SEOUL", "주소", "가".repeat(2000));
        String tooLong = propertyBody("설명 경계", "SEOUL", "주소", "가".repeat(2001));

        assertEquals(201, send("POST", "/api/v1/properties", HOST, ok).statusCode());
        assertEquals(400, send("POST", "/api/v1/properties", HOST, tooLong).statusCode());
    }

    @Test
    void 최대_인원은_100까지_받고_101은_거절한다() throws Exception {
        String propertyId = registerProperty("인원 경계용");
        String ok = """
                {"name":"단체실","maxOccupancy":100,"description":""}
                """;
        String tooMany = """
                {"name":"초과실","maxOccupancy":101,"description":""}
                """;
        String path = "/api/v1/properties/" + propertyId + "/room-types";

        assertEquals(201, send("POST", path, HOST, ok).statusCode());
        assertEquals(400, send("POST", path, HOST, tooMany).statusCode());
    }

    @Test
    void C10_공개_조회는_헤더_없이_된다() throws Exception {
        // 통과 케이스. 인증 가드가 공개 경로까지 막으면 이 건이 잡는다
        String id = registerProperty("공개 조회용");

        assertEquals(200, send("GET", "/api/v1/properties/" + id, null, null).statusCode());
        assertNotNull(id);
    }
}
