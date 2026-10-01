package com.o2o.shared;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 관측 3-1(이슈 228). Prometheus가 긁어 갈 주소와 열린 엔드포인트의 경계.
 *
 * 진짜 포트로 친다. 노출 목록은 웹 계층 설정이라 MockMvc로는 actuator 경로가 실제로 열렸는지 보이지 않는다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ActuatorEndpointTest {

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).build();

    @LocalServerPort
    private int port;

    @Test
    void prometheus_endpoint_exposes_jvm_http_and_pool_metrics() throws Exception {
        // HTTP 지표는 요청이 한 번은 지나가야 생긴다
        get("/actuator/health");

        HttpResponse<String> response = get("/actuator/prometheus");

        assertEquals(200, response.statusCode());
        String body = response.body();
        assertTrue(body.contains("jvm_memory_used_bytes"), "JVM 지표");
        assertTrue(body.contains("http_server_requests_seconds"), "HTTP 요청 지표");
        assertTrue(body.contains("hikaricp_connections_pending"), "커넥션 풀 대기 지표");
        assertTrue(body.contains("application=\"backend\""), "공통 태그");
    }

    @Test
    void health_is_up() throws Exception {
        HttpResponse<String> response = get("/actuator/health");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"status\":\"UP\""), response.body());
    }

    @Test
    void endpoints_outside_the_exposure_list_are_closed() throws Exception {
        for (String path : new String[] {"/actuator/env", "/actuator/beans", "/actuator/heapdump", "/actuator/metrics"}) {
            assertEquals(404, get(path).statusCode(), path);
        }
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .timeout(Duration.ofSeconds(10)).GET().build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
