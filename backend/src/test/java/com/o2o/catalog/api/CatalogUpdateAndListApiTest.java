package com.o2o.catalog.api;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import com.o2o.shared.RegionRegistry;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 6-2단계의 다섯 API와 8-2절의 C3, C4, C5, C7, C8.
 * 설계 근거: 11 숙소 CAT-02, CAT-04, CAT-05, 11 객실 타입 CAT-07, CAT-09, 11 공통 목록과 날짜 범위.
 *
 * CatalogApiTest와 같은 방식이다. 진짜 포트를 열고 JDK HttpClient로 친다.
 * 두 클래스의 스프링 설정이 같아서 컨텍스트는 재사용된다.
 *
 * 목록 검사는 전체 개수를 세지 않는다. 이 테스트에는 트랜잭션 롤백이 없어서 다른 테스트가
 * 만든 자료가 같은 DB에 남아 있다. 그래서 검사할 때마다 고유한 지역 코드를 붙여 그 범위만 본다.
 * 고유한 코드는 등록된 지역이어야 숙소가 되므로(CAT-01, R1 평가 A-02) fixture에 먼저 넣는다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CatalogUpdateAndListApiTest {

    private static final String HOST = "host_001";
    private static final String OTHER_HOST = "host_002";
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).build();

    @LocalServerPort
    private int port;

    @Autowired
    private RegionRegistry regionRegistry;

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

    /** 이 테스트만 쓰는 지역 코드를 뽑아 fixture에 넣는다. 목록 검사의 범위가 된다 */
    private String 새_지역(String prefix) {
        String region = prefix + System.nanoTime();
        regionRegistry.register(region);
        return region;
    }

    private JsonNode registerProperty(String actorId, String name, String regionCode)
            throws Exception {
        String body = """
                {"name":"%s","regionCode":"%s","address":"서울특별시 종로구 예시로 30","description":""}
                """.formatted(name, regionCode);
        HttpResponse<String> res = send("POST", "/api/v1/properties", actorId, body);
        assertEquals(201, res.statusCode(), res.body());
        return JSON.readTree(res.body());
    }

    // ---------- CAT-02와 C3, C4, C5 ----------

    @Test
    void CAT_02_맞는_버전으로_수정하면_200이고_버전이_오른다() throws Exception {
        JsonNode created = registerProperty(HOST, "수정 전", "SEOUL");
        String id = created.get("id").stringValue();
        String body = """
                {"version":0,"name":"수정 후"}
                """;

        HttpResponse<String> res = send("PATCH", "/api/v1/properties/" + id, HOST, body);
        JsonNode json = JSON.readTree(res.body());

        assertEquals(200, res.statusCode(), res.body());
        assertEquals("수정 후", json.get("name").stringValue());
        assertEquals(1, json.get("version").asInt(), "수정하면 version이 1 올라야 한다");
        // 생략한 필드는 유지된다. 11 CAT-02 필드표
        assertEquals("SEOUL", json.get("regionCode").stringValue());
    }

    @Test
    void C4_틀린_버전으로_수정하면_409이고_바뀌지_않는다() throws Exception {
        JsonNode created = registerProperty(HOST, "버전 확인용", "SEOUL");
        String id = created.get("id").stringValue();
        String body = """
                {"version":99,"name":"바뀌면 안 됨"}
                """;

        HttpResponse<String> res = send("PATCH", "/api/v1/properties/" + id, HOST, body);

        assertEquals(409, res.statusCode(), res.body());
        assertTrue(res.body().contains("VERSION_CONFLICT"), res.body());

        JsonNode after = JSON.readTree(send("GET", "/api/v1/properties/" + id, null, null).body());
        assertEquals("버전 확인용", after.get("name").stringValue(), "거절됐는데 값이 바뀌었다");
        assertEquals(0, after.get("version").asInt());
    }

    @Test
    void C3_남의_숙소를_수정하면_404이고_바뀌지_않는다() throws Exception {
        // T02. 11 인증과 접근 제어가 다른 사용자 소유 자원을 404로 적고 자원 정보를 흘리지 않는다
        JsonNode created = registerProperty(HOST, "내 숙소", "SEOUL");
        String id = created.get("id").stringValue();
        String body = """
                {"version":0,"name":"남이 고침"}
                """;

        HttpResponse<String> res = send("PATCH", "/api/v1/properties/" + id, OTHER_HOST, body);

        assertEquals(404, res.statusCode(), res.body());
        assertTrue(res.body().contains("RESOURCE_NOT_FOUND"), res.body());

        JsonNode after = JSON.readTree(send("GET", "/api/v1/properties/" + id, null, null).body());
        assertEquals("내 숙소", after.get("name").stringValue(), "거절됐는데 값이 바뀌었다");
    }

    @Test
    void C5_version_외에_바꿀_필드가_없으면_400이다() throws Exception {
        JsonNode created = registerProperty(HOST, "빈 수정용", "SEOUL");
        String id = created.get("id").stringValue();
        String body = """
                {"version":0}
                """;

        HttpResponse<String> res = send("PATCH", "/api/v1/properties/" + id, HOST, body);

        assertEquals(400, res.statusCode(), res.body());
        assertTrue(res.body().contains("INVALID_REQUEST"), res.body());
    }

    @Test
    void C5_version이_없으면_400이다() throws Exception {
        JsonNode created = registerProperty(HOST, "버전 없는 수정용", "SEOUL");
        String id = created.get("id").stringValue();
        String body = """
                {"name":"버전 없이"}
                """;

        assertEquals(400, send("PATCH", "/api/v1/properties/" + id, HOST, body).statusCode());
    }

    // ---------- 수정의 공백과 명시적 null. C13 (R1 평가 A-03, B-03) ----------

    @Test
    void C13_수정_이름은_strip한_값으로_저장한다() throws Exception {
        JsonNode created = registerProperty(HOST, "strip 전", "SEOUL");
        String id = created.get("id").stringValue();

        HttpResponse<String> res = send("PATCH", "/api/v1/properties/" + id, HOST, """
                {"version":0,"name":"  strip 후  "}
                """);

        assertEquals(200, res.statusCode(), res.body());
        assertEquals("strip 후", JSON.readTree(res.body()).get("name").stringValue());
    }

    @Test
    void C13_수정에서_공백만_있는_문자열은_400이고_바뀌지_않는다() throws Exception {
        // 11 CAT-02 필드표. 생략은 유지이지만 공백만 있는 값은 값이 아니다
        JsonNode created = registerProperty(HOST, "공백 수정용", "SEOUL");
        String id = created.get("id").stringValue();

        HttpResponse<String> name = send("PATCH", "/api/v1/properties/" + id, HOST, """
                {"version":0,"name":"   "}
                """);
        HttpResponse<String> region = send("PATCH", "/api/v1/properties/" + id, HOST, """
                {"version":0,"regionCode":"  "}
                """);
        HttpResponse<String> address = send("PATCH", "/api/v1/properties/" + id, HOST, """
                {"version":0,"address":" "}
                """);

        assertEquals(400, name.statusCode(), name.body());
        assertEquals("name", JSON.readTree(name.body()).get("details").get(0).get("field").stringValue());
        assertEquals(400, region.statusCode(), region.body());
        assertEquals(400, address.statusCode(), address.body());
        JsonNode after = JSON.readTree(send("GET", "/api/v1/properties/" + id, null, null).body());
        assertEquals("공백 수정용", after.get("name").stringValue(), "거절됐는데 값이 바뀌었다");
        assertEquals(0, after.get("version").asInt());
    }

    @Test
    void C13_수정에서_명시적_null은_400이고_생략은_유지다() throws Exception {
        // 11 공통 요청과 응답 규칙의 허용하지 않은 null. 생략(유지)과 null(거절)을 가른다
        JsonNode created = registerProperty(HOST, "null 수정용", "SEOUL");
        String id = created.get("id").stringValue();

        HttpResponse<String> nullName = send("PATCH", "/api/v1/properties/" + id, HOST, """
                {"version":0,"name":null}
                """);
        HttpResponse<String> nullDescription = send("PATCH", "/api/v1/properties/" + id, HOST, """
                {"version":0,"name":"바꿈","description":null}
                """);
        HttpResponse<String> omitted = send("PATCH", "/api/v1/properties/" + id, HOST, """
                {"version":0,"name":"바꿈"}
                """);

        assertEquals(400, nullName.statusCode(), nullName.body());
        assertTrue(nullName.body().contains("INVALID_REQUEST"), nullName.body());
        assertEquals(400, nullDescription.statusCode(), nullDescription.body());
        assertEquals(200, omitted.statusCode(), omitted.body());
        JsonNode json = JSON.readTree(omitted.body());
        assertEquals("바꿈", json.get("name").stringValue());
        assertEquals("SEOUL", json.get("regionCode").stringValue(), "생략한 필드가 유지되지 않았다");
    }

    @Test
    void C13_객실_타입_수정도_strip하고_공백만과_명시적_null은_400이다() throws Exception {
        // 11 CAT-07 필드표. 숙소 수정과 같은 규칙이다
        JsonNode property = registerProperty(HOST, "객실 공백 수정용", "SEOUL");
        String roomTypeId = JSON.readTree(send("POST",
                "/api/v1/properties/" + property.get("id").stringValue() + "/room-types", HOST,
                """
                {"name":"객실","maxOccupancy":4,"description":""}
                """).body()).get("id").stringValue();
        String path = "/api/v1/room-types/" + roomTypeId;

        HttpResponse<String> blank = send("PATCH", path, HOST, """
                {"version":0,"name":"   "}
                """);
        HttpResponse<String> nullOccupancy = send("PATCH", path, HOST, """
                {"version":0,"maxOccupancy":null}
                """);
        HttpResponse<String> padded = send("PATCH", path, HOST, """
                {"version":0,"name":"  새 객실  "}
                """);

        assertEquals(400, blank.statusCode(), blank.body());
        assertEquals(400, nullOccupancy.statusCode(), nullOccupancy.body());
        assertEquals(200, padded.statusCode(), padded.body());
        assertEquals("새 객실", JSON.readTree(padded.body()).get("name").stringValue());
    }

    // ---------- CAT-07 ----------

    @Test
    void CAT_07_객실_타입을_수정한다() throws Exception {
        JsonNode property = registerProperty(HOST, "객실 수정용", "SEOUL");
        String roomTypeId = JSON.readTree(send("POST",
                "/api/v1/properties/" + property.get("id").stringValue() + "/room-types", HOST,
                """
                {"name":"수정 전 객실","maxOccupancy":4,"description":""}
                """).body()).get("id").stringValue();

        HttpResponse<String> res = send("PATCH", "/api/v1/room-types/" + roomTypeId, HOST,
                """
                {"version":0,"maxOccupancy":2}
                """);
        JsonNode json = JSON.readTree(res.body());

        assertEquals(200, res.statusCode(), res.body());
        // 하향을 막지 않는다. 11 CAT-07 처리 규칙이 신규 예약에만 적용한다고 적는다
        assertEquals(2, json.get("maxOccupancy").asInt());
        assertEquals(1, json.get("version").asInt());
        assertEquals("수정 전 객실", json.get("name").stringValue(), "생략한 필드가 바뀌었다");
    }

    @Test
    void CAT_07_남의_객실_타입을_수정하면_404다() throws Exception {
        JsonNode property = registerProperty(HOST, "남의 객실용", "SEOUL");
        String roomTypeId = JSON.readTree(send("POST",
                "/api/v1/properties/" + property.get("id").stringValue() + "/room-types", HOST,
                """
                {"name":"내 객실","maxOccupancy":2,"description":""}
                """).body()).get("id").stringValue();

        HttpResponse<String> res = send("PATCH", "/api/v1/room-types/" + roomTypeId, OTHER_HOST,
                """
                {"version":0,"name":"남이 고침"}
                """);

        assertEquals(404, res.statusCode(), res.body());
    }

    @Test
    void C14_CAT_07_maxOccupancy_0과_101은_400이고_값과_version이_그대로다() throws Exception {
        // R1 평가 B-05. I14는 수정 경로에서도 지켜야 하고 상한 100은 11 공통 규칙이다.
        // 수정 DTO의 하한이나 RoomType.update의 검사를 지우면 이 테스트가 잡는다
        JsonNode property = registerProperty(HOST, "인원 경계 수정용", "SEOUL");
        String roomTypeId = JSON.readTree(send("POST",
                "/api/v1/properties/" + property.get("id").stringValue() + "/room-types", HOST,
                """
                {"name":"경계 객실","maxOccupancy":4,"description":""}
                """).body()).get("id").stringValue();
        String path = "/api/v1/room-types/" + roomTypeId;

        HttpResponse<String> zero = send("PATCH", path, HOST, """
                {"version":0,"maxOccupancy":0}
                """);
        HttpResponse<String> over = send("PATCH", path, HOST, """
                {"version":0,"maxOccupancy":101}
                """);

        assertEquals(400, zero.statusCode(), zero.body());
        assertTrue(zero.body().contains("INVALID_REQUEST"), zero.body());
        assertEquals(400, over.statusCode(), over.body());
        assertTrue(over.body().contains("INVALID_REQUEST"), over.body());
        JsonNode after = JSON.readTree(send("GET", path, null, null).body());
        assertEquals(4, after.get("maxOccupancy").asInt(), "거절됐는데 값이 바뀌었다");
        assertEquals(0, after.get("version").asInt());
    }

    // ---------- CAT-04와 C7, C8 ----------

    @Test
    void CAT_04_지역_코드로_거른다() throws Exception {
        String region = 새_지역("REGION_");
        registerProperty(HOST, "지역 하나", region);
        registerProperty(HOST, "지역 둘", region);

        JsonNode json = JSON.readTree(
                send("GET", "/api/v1/properties?regionCode=" + region, null, null).body());

        assertEquals(2, json.get("totalElements").asInt());
        assertEquals(2, json.get("items").size());
    }

    @Test
    void C7_page와_size의_기본값이_0과_20이다() throws Exception {
        JsonNode json = JSON.readTree(send("GET", "/api/v1/properties", null, null).body());

        assertEquals(0, json.get("page").asInt());
        assertEquals(20, json.get("size").asInt());
    }

    @Test
    void C7_size_상한은_100이고_101은_400이다() throws Exception {
        assertEquals(200, send("GET", "/api/v1/properties?size=100", null, null).statusCode());
        assertEquals(400, send("GET", "/api/v1/properties?size=101", null, null).statusCode());
    }

    @Test
    void C7_size가_0이거나_page가_음수면_400이다() throws Exception {
        assertEquals(400, send("GET", "/api/v1/properties?size=0", null, null).statusCode());
        assertEquals(400, send("GET", "/api/v1/properties?page=-1", null, null).statusCode());
    }

    @Test
    void C8_기본_정렬이_id_오름차순이다() throws Exception {
        String region = 새_지역("SORT_");
        for (int i = 0; i < 3; i++) {
            registerProperty(HOST, "정렬 " + i, region);
        }

        JsonNode json = JSON.readTree(
                send("GET", "/api/v1/properties?regionCode=" + region, null, null).body());

        List<String> ids = new ArrayList<>();
        json.get("items").forEach((n) -> ids.add(n.get("id").stringValue()));
        assertEquals(3, ids.size());
        assertEquals(ids.stream().sorted().toList(), ids, "id 오름차순이 아니다");
    }

    @Test
    void C8_페이지_범위를_넘으면_빈_items다() throws Exception {
        String region = 새_지역("EMPTY_");
        registerProperty(HOST, "한 건", region);

        JsonNode json = JSON.readTree(send(
                "GET", "/api/v1/properties?regionCode=" + region + "&page=5", null, null).body());

        assertEquals(0, json.get("items").size());
        assertEquals(1, json.get("totalElements").asInt());
    }

    // ---------- 등록 지역 코드. C12 (R1 평가 A-02, B-02) ----------

    @Test
    void C12_미등록_지역_코드로_등록하면_400_INVALID_REQUEST다() throws Exception {
        // 11 CAT-01 처리 규칙. 등록된 지역 코드를 확인하고 저장한다. 형식은 맞는 값이다
        String body = """
                {"name":"미등록 지역","regionCode":"NOWHERE_%d","address":"서울특별시 종로구 예시로 30","description":""}
                """.formatted(System.nanoTime());

        HttpResponse<String> res = send("POST", "/api/v1/properties", HOST, body);

        assertEquals(400, res.statusCode(), res.body());
        JsonNode json = JSON.readTree(res.body());
        assertEquals("INVALID_REQUEST", json.get("code").stringValue());
        assertEquals("regionCode", json.get("details").get(0).get("field").stringValue());
    }

    @Test
    void C12_등록된_fixture_지역_코드로는_등록된다() throws Exception {
        // 통과 짝. 초기 fixture의 값 하나
        registerProperty(HOST, "제주 스테이", "JEJU");
    }

    @Test
    void C12_미등록_지역_코드로_수정하면_400이고_바뀌지_않는다() throws Exception {
        JsonNode created = registerProperty(HOST, "지역 수정용", "SEOUL");
        String id = created.get("id").stringValue();
        String body = """
                {"version":0,"regionCode":"NOWHERE_%d"}
                """.formatted(System.nanoTime());

        HttpResponse<String> res = send("PATCH", "/api/v1/properties/" + id, HOST, body);

        assertEquals(400, res.statusCode(), res.body());
        assertTrue(res.body().contains("INVALID_REQUEST"), res.body());
        JsonNode after = JSON.readTree(send("GET", "/api/v1/properties/" + id, null, null).body());
        assertEquals("SEOUL", after.get("regionCode").stringValue(), "거절됐는데 값이 바뀌었다");
        assertEquals(0, after.get("version").asInt());
    }

    @Test
    void C12_등록된_지역_코드로_수정하면_200이다() throws Exception {
        JsonNode created = registerProperty(HOST, "지역 수정 통과용", "SEOUL");
        String id = created.get("id").stringValue();

        HttpResponse<String> res = send("PATCH", "/api/v1/properties/" + id, HOST, """
                {"version":0,"regionCode":"BUSAN"}
                """);

        assertEquals(200, res.statusCode(), res.body());
        assertEquals("BUSAN", JSON.readTree(res.body()).get("regionCode").stringValue());
    }

    @Test
    void C12_미등록_지역_코드로_목록을_조회하면_400_INVALID_REQUEST다() throws Exception {
        // 11 CAT-04 쿼리표. 빈 목록이 아니라 400이다. 미등록 코드로 빈 목록을 주면 없는 지역이
        // 있는 것처럼 보인다
        HttpResponse<String> res = send(
                "GET", "/api/v1/properties?regionCode=NOWHERE_" + System.nanoTime(), null, null);

        assertEquals(400, res.statusCode(), res.body());
        assertEquals("INVALID_REQUEST", JSON.readTree(res.body()).get("code").stringValue());
    }

    @Test
    void C12_목록의_지역_코드는_32자까지_받고_33자와_공백만은_400이다() throws Exception {
        // 11 CAT-04 쿼리표의 1자 이상 32자 이하. 길이 검사가 등록 검사보다 먼저다
        String code32 = ("B" + System.nanoTime() + "A".repeat(32)).substring(0, 32);
        regionRegistry.register(code32);

        assertEquals(200, send("GET", "/api/v1/properties?regionCode=" + code32, null, null)
                .statusCode());
        HttpResponse<String> tooLong = send(
                "GET", "/api/v1/properties?regionCode=" + code32 + "A", null, null);
        HttpResponse<String> blank = send("GET", "/api/v1/properties?regionCode=%20", null, null);

        assertEquals(400, tooLong.statusCode(), tooLong.body());
        assertEquals("INVALID_REQUEST", JSON.readTree(tooLong.body()).get("code").stringValue());
        assertEquals(400, blank.statusCode(), blank.body());
    }

    // ---------- CAT-05 ----------

    @Test
    void CAT_05_본인_숙소만_보이고_헤더가_없으면_401이다() throws Exception {
        registerProperty(OTHER_HOST, "남의 숙소", "SEOUL");

        HttpResponse<String> res = send("GET", "/api/v1/host/properties?size=100", HOST, null);
        JsonNode json = JSON.readTree(res.body());

        assertEquals(200, res.statusCode(), res.body());
        json.get("items").forEach((n) ->
                assertEquals(HOST, n.get("hostId").stringValue(), "남의 숙소가 섞였다"));

        assertEquals(401, send("GET", "/api/v1/host/properties", null, null).statusCode());
    }

    // ---------- CAT-09 ----------

    @Test
    void CAT_09_숙소의_객실_타입_목록을_준다() throws Exception {
        JsonNode property = registerProperty(HOST, "객실 목록용", "SEOUL");
        String propertyId = property.get("id").stringValue();
        for (int i = 0; i < 2; i++) {
            send("POST", "/api/v1/properties/" + propertyId + "/room-types", HOST, """
                    {"name":"객실 %d","maxOccupancy":2,"description":""}
                    """.formatted(i));
        }

        JsonNode json = JSON.readTree(
                send("GET", "/api/v1/properties/" + propertyId + "/room-types", null, null).body());

        assertEquals(2, json.get("totalElements").asInt());
    }

    @Test
    void CAT_09_없는_숙소_아래를_조회하면_404다() throws Exception {
        // 11 인증과 접근 제어가 중첩 경로의 부모와 자식 관계도 확인하라고 적는다.
        // 빈 목록으로 답하면 없는 숙소가 있는 것처럼 보인다
        HttpResponse<String> res = send(
                "GET", "/api/v1/properties/prop_nothing/room-types", null, null);

        assertEquals(404, res.statusCode(), res.body());
    }
}
