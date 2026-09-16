package com.o2o.shared;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 공통 오류 형식의 경계 셋. 설계 근거: 11 에러 응답 표(공통 형식과 상태 코드는 모든 API에
 * 적용한다. 500 INTERNAL_ERROR는 내부 예외 내용을 응답에 넣지 않는다), 11 공통 요청과 응답
 * 규칙(잘못된 타입은 400). R1 평가 B-04.
 *
 * 예상하지 못한 예외를 내는 컨트롤러는 프로덕션에 없어야 하므로 이 테스트 컨텍스트에만 하나를
 * 더한다. 그래서 이 클래스는 컨텍스트를 따로 띄운다. 진짜 포트로 치는 이유는 CatalogApiTest와
 * 같다. 예외가 서블릿 컨테이너까지 어떻게 나가는지는 MockMvc로 보이지 않는다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SharedErrorResponseApiTest {

    private static final String SECRET = "내부에서만 보여야 하는 메시지 secret-detail-7f3a";
    private static final JsonMapper JSON = JsonMapper.builder().build();

    @TestConfiguration
    static class BoomConfiguration {
        @Bean
        BoomController boomController() {
            return new BoomController();
        }
    }

    @RestController
    static class BoomController {
        @GetMapping("/test/boom")
        String boom() {
            throw new IllegalStateException(SECRET);
        }
    }

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).build();

    @LocalServerPort
    private int port;

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + path))
                .timeout(Duration.ofSeconds(10))
                .GET().build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static void assertCommonShape(JsonNode body) {
        // 11 에러 응답 절의 네 칸. details는 없으면 빈 배열이다
        for (String field : new String[] {"code", "message", "traceId", "details"}) {
            assertTrue(body.has(field), "공통 오류 형식에 " + field + " 칸이 없다: " + body);
        }
        assertTrue(body.get("traceId").stringValue().startsWith("trace_"));
        assertTrue(body.get("details").isArray());
    }

    @Test
    void 예상하지_못한_예외는_500_INTERNAL_ERROR이고_내부_내용을_넣지_않는다() throws Exception {
        HttpResponse<String> res = get("/test/boom");

        assertEquals(500, res.statusCode(), res.body());
        assertTrue(res.headers().firstValue("Content-Type").orElse("").startsWith("application/json"),
                res.headers().toString());
        JsonNode body = JSON.readTree(res.body());
        assertCommonShape(body);
        assertEquals("INTERNAL_ERROR", body.get("code").stringValue());
        assertFalse(res.body().contains("secret-detail"), "예외 메시지가 응답에 새어 나갔다: " + res.body());
        assertFalse(res.body().contains("IllegalStateException"), "예외 클래스가 응답에 새어 나갔다: " + res.body());
        assertFalse(res.body().contains("boom"), "스택 정보가 응답에 새어 나갔다: " + res.body());
    }

    @Test
    void 쿼리_값의_타입이_틀리면_400_INVALID_REQUEST이고_어느_값인지_알려준다() throws Exception {
        HttpResponse<String> res = get("/api/v1/properties?page=abc");

        assertEquals(400, res.statusCode(), res.body());
        JsonNode body = JSON.readTree(res.body());
        assertCommonShape(body);
        assertEquals("INVALID_REQUEST", body.get("code").stringValue());
        assertEquals("page", body.get("details").get(0).get("field").stringValue());
    }

    @Test
    void 필수_쿼리_값이_없으면_400_INVALID_REQUEST이고_어느_값인지_알려준다() throws Exception {
        // SEARCH-01의 regionCode는 필수다. 스프링 기본 본문이 아니라 공통 형식으로 나가야 한다
        HttpResponse<String> res = get("/api/v1/search/properties");

        assertEquals(400, res.statusCode(), res.body());
        JsonNode body = JSON.readTree(res.body());
        assertCommonShape(body);
        assertEquals("INVALID_REQUEST", body.get("code").stringValue());
        assertEquals("regionCode", body.get("details").get(0).get("field").stringValue());
    }

    @Test
    void 컨텍스트_핸들러의_404는_마지막_자리에_삼켜지지_않는다() throws Exception {
        // 마지막 자리(500)는 어드바이스 여섯이 전부 지나간 뒤에만 불린다. 카탈로그의 404가
        // 그대로 나가는 것이 그 순서의 증거다
        HttpResponse<String> res = get("/api/v1/properties/prop_nothing");

        assertEquals(404, res.statusCode(), res.body());
        assertEquals("RESOURCE_NOT_FOUND", JSON.readTree(res.body()).get("code").stringValue());
    }
}
