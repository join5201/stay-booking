package com.o2o.catalog.api;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
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

import com.o2o.catalog.domain.Property;
import com.o2o.catalog.domain.PropertyRepository;
import com.o2o.catalog.domain.RoomType;
import com.o2o.catalog.domain.RoomTypeRepository;
import com.o2o.shared.PropertyId;
import com.o2o.shared.RoomTypeId;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 8-1절 C11. 같은 version을 읽은 두 수정이 실제 MySQL에서 경합한다. R1 평가 A-01과 B-01.
 * 설계 근거: 11 공통 요청과 응답 규칙의 version 불일치 409, 계약 2-2절의 낙관적 잠금.
 *
 * 순차적인 낡은 요청(C4)은 애그리거트의 메모리 대조가 잡는다. 동시 요청은 둘 다 version 0을
 * 읽어 그 대조를 지나므로 저장 시점의 대조(@Version)가 있어야 잡힌다. 그 차이를 이렇게 본다.
 * 테스트 트랜잭션이 version 0을 읽어 먼저 수정하고 flush해 행 잠금을 쥔 채로 서버에 version 0의
 * PATCH를 보낸다. 서버는 커밋 전 값인 0을 읽어 메모리 대조를 지나고 저장에서 잠금을 기다린다.
 * 잠금이 풀리기 전에 서버 요청이 끝나면 저장 시점 대조가 없는 것이다. 테스트 트랜잭션이 커밋되면
 * 서버의 UPDATE는 where version = 0 에서 0행이 되어 409 VERSION_CONFLICT다. 남는 값은 먼저
 * 커밋한 쪽이고 version은 1이다. 재고 묶음 R1 반영의 V14와 같은 모양이다.
 *
 * CatalogApiTest와 같은 방식으로 진짜 포트를 연다. 스프링 설정이 같아서 컨텍스트는 재사용된다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CatalogConcurrentUpdateApiTest {

    private static final String HOST = "host_001";
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).build();

    @LocalServerPort
    private int port;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private RoomTypeRepository roomTypeRepository;

    private HttpResponse<String> send(String method, String path, String actorId, String body)
            throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + path))
                .timeout(Duration.ofSeconds(60));
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
        HttpResponse<String> res = send("POST", "/api/v1/properties", HOST, """
                {"name":"%s","regionCode":"SEOUL","address":"서울특별시 종로구 예시로 30","description":""}
                """.formatted(name));
        assertEquals(201, res.statusCode(), res.body());
        return JSON.readTree(res.body()).get("id").stringValue();
    }

    private String registerRoomType(String propertyId, String name) throws Exception {
        HttpResponse<String> res = send("POST", "/api/v1/properties/" + propertyId + "/room-types",
                HOST, """
                {"name":"%s","maxOccupancy":4,"description":""}
                """.formatted(name));
        assertEquals(201, res.statusCode(), res.body());
        return JSON.readTree(res.body()).get("id").stringValue();
    }

    @Test
    void C11_같은_version의_동시_숙소_수정은_뒤에_저장하는_쪽이_409다() throws Exception {
        String id = registerProperty("경합 전");
        PropertyId propertyId = PropertyId.of(id);

        ExecutorService waiter = Executors.newSingleThreadExecutor();
        try {
            Future<HttpResponse<String>> request = new TransactionTemplate(transactionManager)
                    .execute(status -> {
                        Property mine = propertyRepository.findById(propertyId).orElseThrow();
                        assertEquals(0L, mine.version());
                        // 같은 version 0으로 먼저 수정한다. flush된 UPDATE가 행 잠금을 쥔다
                        mine.update(0L, "트랜잭션 쪽", null, null, null, Instant.now());
                        propertyRepository.save(mine);
                        Future<HttpResponse<String>> sent = waiter.submit(() -> send(
                                "PATCH", "/api/v1/properties/" + id, HOST,
                                """
                                {"version":0,"name":"서버 쪽"}
                                """));
                        // 잠금을 쥔 동안 서버 요청이 끝나면 저장 시점 대조 없이 덮어쓴 것이다
                        assertThrows(TimeoutException.class, () -> sent.get(2, TimeUnit.SECONDS));
                        return sent;
                    });

            HttpResponse<String> res = request.get(30, TimeUnit.SECONDS);
            assertEquals(409, res.statusCode(), res.body());
            assertTrue(res.body().contains("VERSION_CONFLICT"), res.body());
        } finally {
            waiter.shutdownNow();
        }
        // 먼저 커밋한 쪽이 남고 version은 한 번만 오른다
        JsonNode after = JSON.readTree(send("GET", "/api/v1/properties/" + id, null, null).body());
        assertEquals("트랜잭션 쪽", after.get("name").stringValue());
        assertEquals(1, after.get("version").asInt());
    }

    @Test
    void C11_같은_version의_동시_객실_타입_수정도_뒤에_저장하는_쪽이_409다() throws Exception {
        String propertyId = registerProperty("객실 경합용");
        String id = registerRoomType(propertyId, "경합 전 객실");
        RoomTypeId roomTypeId = RoomTypeId.of(id);

        ExecutorService waiter = Executors.newSingleThreadExecutor();
        try {
            Future<HttpResponse<String>> request = new TransactionTemplate(transactionManager)
                    .execute(status -> {
                        RoomType mine = roomTypeRepository.findById(roomTypeId).orElseThrow();
                        assertEquals(0L, mine.version());
                        mine.update(0L, null, 3, null, Instant.now());
                        roomTypeRepository.save(mine);
                        Future<HttpResponse<String>> sent = waiter.submit(() -> send(
                                "PATCH", "/api/v1/room-types/" + id, HOST,
                                """
                                {"version":0,"maxOccupancy":2}
                                """));
                        assertThrows(TimeoutException.class, () -> sent.get(2, TimeUnit.SECONDS));
                        return sent;
                    });

            HttpResponse<String> res = request.get(30, TimeUnit.SECONDS);
            assertEquals(409, res.statusCode(), res.body());
            assertTrue(res.body().contains("VERSION_CONFLICT"), res.body());
        } finally {
            waiter.shutdownNow();
        }
        JsonNode after = JSON.readTree(send("GET", "/api/v1/room-types/" + id, null, null).body());
        assertEquals(3, after.get("maxOccupancy").asInt());
        assertEquals(1, after.get("version").asInt());
    }
}
