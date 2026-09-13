package com.o2o.payment.api;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Y23의 dev 밖 쪽. T30. 설계 근거: 11 인증과 접근 제어 셋째 문단(개발 프로파일 밖에서는 X-Dev-Actor-Id
 * 어댑터와 /internal 경로를 비활성화한다), 계약 7절 D-3 가.
 *
 * 설정 파일의 spring.profiles.active=dev를 @ActiveProfiles가 덮어 dev가 아닌 프로파일로 컨텍스트를
 * 띄운다. 그 컨텍스트에서 행위자 헤더는 있어도 401이고 INTERNAL-01 경로는 컨트롤러 빈이 없어 404다.
 * dev 쪽 짝(둘 다 산다)은 MockPaymentEventApiTest의 Y23이다. 공개 API는 프로파일과 무관하게 열린다.
 *
 * 컨텍스트가 하나 더 뜨는 대가는 D-3 표에 적혀 있다. 이 클래스는 데이터를 만들지 않는다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("verify")
class DevProfileBoundaryTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).build();

    @LocalServerPort
    private int port;

    @Autowired
    private Environment environment;

    @Autowired
    private ApplicationContext context;

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

    @Test
    void Y23_dev가_아닌_프로파일에서는_행위자_헤더가_있어도_HOST_전용_조회가_401이고_INTERNAL_01은_404다()
            throws Exception {
        assertFalse(environment.matchesProfiles("dev"), String.join(",", environment.getActiveProfiles()));
        assertTrue(context.getBeansOfType(MockPaymentEventController.class).isEmpty());

        HttpResponse<String> hostOnly = send("GET", "/api/v1/host/properties", "host_001", null);
        assertEquals(401, hostOnly.statusCode(), hostOnly.body());
        JsonNode error = JSON.readTree(hostOnly.body());
        assertEquals("ACTOR_REQUIRED", error.get("code").asString(), hostOnly.body());

        String body = """
                {"eventId":"mock_event_t30","paymentAttemptId":"attempt_t30","pgTransactionId":"mock_tx_t30",
                 "outcome":"APPROVED","amount":180000,"currency":"KRW"}
                """;
        HttpResponse<String> internal = send("POST", "/internal/mock-payments/events", "mock_001", body);
        assertEquals(404, internal.statusCode(), internal.body());

        // 공개 API는 프로파일 밖에서도 열린다(11 인증과 접근 제어의 공개 행)
        HttpResponse<String> open = send("GET", "/api/v1/properties?regionCode=SEOUL", null, null);
        assertEquals(200, open.statusCode(), open.body());
    }
}
