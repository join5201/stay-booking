package com.o2o.payment.api;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

import com.o2o.payment.CommittedPaymentEvents;
import com.o2o.payment.application.PaymentApplicationService;
import com.o2o.payment.application.PaymentAttemptView;
import com.o2o.payment.application.PaymentSummaryView;
import com.o2o.payment.application.RefundView;
import com.o2o.payment.domain.MockEventResult;
import com.o2o.payment.domain.MockMode;
import com.o2o.payment.domain.MockPaymentEvent;
import com.o2o.payment.domain.MockPaymentEventRepository;
import com.o2o.payment.domain.PaymentApproved;
import com.o2o.payment.domain.PaymentAttempt;
import com.o2o.payment.domain.PaymentAttemptId;
import com.o2o.payment.domain.PaymentRepository;
import com.o2o.payment.domain.RefundReason;
import com.o2o.shared.ApiTime;
import com.o2o.shared.Money;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Y15부터 Y22와 Y23의 dev 쪽. 설계 근거: 계약 2절 INTERNAL-01 검사 순서 표와 8-1절, 11 INTERNAL-01
 * (요청 표, 200 MockEventResult, 에러 표, 중복과 결과 처리 규칙 1부터 8), 11 인증과 접근 제어,
 * 11 공통 절 38행, 응답 모델 MockEventResult, T21, T22, T23, T30, 7절 D-2와 D-4.
 *
 * 앞 묶음 BookingApiTest와 같은 방식이다. 진짜 포트를 열고 JDK HttpClient로 친다. 서버가 다른
 * 스레드에서 실제 시계로 돌므로 시각은 고정하지 않고 형식과 같음만 본다(testing.md T5). 트랜잭션을
 * 되돌릴 수 없어 시도는 예약 ID를 새로 만들어 앱 서비스로 연다. 결제 접수 API는 예약의 것이라
 * (11 PAY-01) 이 묶음에 HTTP 입구가 없고, DEFER 시도는 INTERNAL-01이 오기 전까지 REQUESTED다.
 *
 * 잠금이 낀 Y22는 앞 묶음 V14의 방식이다. 테스트 트랜잭션이 Payment 행을 잠근 채 다른 스레드로
 * 요청 둘을 보내고, 둘 다 잠금에 붙잡힌 것을 본 뒤 풀어 준다. 순차 호출 둘로는 같은 순간의 둘이
 * 줄을 서는지 볼 수 없다. 커밋 뒤 이벤트 도착은 테스트 전용 구독자 CommittedPaymentEvents로 센다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MockPaymentEventApiTest {

    private static final String MOCK = "mock_001";
    private static final String EVENTS = "/internal/mock-payments/events";
    private static final Money CHARGE = Money.krw(180_000);
    private static final String TIME_PATTERN = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z";
    private static final JsonMapper JSON = JsonMapper.builder().build();

    /**
     * Y21. 이벤트 기록 뒤 예약 정책이 실패하는 상황(T23)을 흉내 내는 트랜잭션 안 구독자. 커밋 직전에
     * 한 번 예외를 던진다. 예약 정책은 이 묶음 밖이라 그 자리에 이 구독자를 세운다. BEFORE_COMMIT이라
     * 계약 2절 12행(이벤트 기록 저장)까지 끝난 뒤에 터지고 그 예외가 트랜잭션을 되돌린다.
     */
    static class PolicyFailureOnce {

        final AtomicBoolean armed = new AtomicBoolean(false);

        @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
        public void onApproved(PaymentApproved event) {
            if (armed.compareAndSet(true, false)) {
                throw new IllegalStateException("테스트 전용 강제 실패: " + event.paymentAttemptId().value());
            }
        }
    }

    @TestConfiguration
    static class ApiTestConfiguration {

        @Bean
        CommittedPaymentEvents committedPaymentEvents() {
            return new CommittedPaymentEvents();
        }

        @Bean
        PolicyFailureOnce policyFailureOnce() {
            return new PolicyFailureOnce();
        }
    }

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).build();

    @LocalServerPort
    private int port;

    @Autowired
    private PaymentApplicationService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private MockPaymentEventRepository eventRepository;

    @Autowired
    private CommittedPaymentEvents committed;

    @Autowired
    private PolicyFailureOnce policyFailure;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private static String newBookingId() {
        return "bk_test_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    private static String newEventId() {
        return "mock_event_" + UUID.randomUUID().toString().replace("-", "");
    }

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

    /** INTERNAL-01. 행위자와 body */
    private HttpResponse<String> deliver(String actorId, String body) throws Exception {
        return send("POST", EVENTS, actorId, body);
    }

    private HttpResponse<String> deliver(Map<String, Object> body) throws Exception {
        return deliver(MOCK, JSON.writeValueAsString(body));
    }

    /** 11 INTERNAL-01 요청 표의 필수 여섯. 시도에 저장된 거래 번호와 금액을 그대로 싣는다 */
    private static Map<String, Object> body(String eventId, PaymentAttemptView attempt, String outcome) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("eventId", eventId);
        body.put("paymentAttemptId", attempt.id());
        body.put("pgTransactionId", attempt.pgTransactionId());
        body.put("outcome", outcome);
        body.put("amount", attempt.amount());
        body.put("currency", attempt.currency());
        if ("FAILED".equals(outcome)) {
            body.put("failureCode", PaymentAttempt.MOCK_DECLINED);
        }
        return body;
    }

    private static Map<String, Object> with(Map<String, Object> body, String field, Object value) {
        Map<String, Object> copy = new LinkedHashMap<>(body);
        if (value == null) {
            copy.remove(field);
        } else {
            copy.put(field, value);
        }
        return copy;
    }

    /** DEFER 시도 하나를 연다. 예약 ID는 새로 만든다 */
    private PaymentAttemptView deferredAttempt() {
        return paymentService.openAttempt(newBookingId(), CHARGE, MockMode.DEFER);
    }

    private PaymentAttemptView stored(PaymentAttemptView attempt) {
        return paymentService.attemptsOf(attempt.bookingId()).attempts().stream()
                .filter(a -> a.id().equals(attempt.id()))
                .findFirst()
                .orElseThrow();
    }

    private static void assertError(HttpResponse<String> res, int status, String code) {
        assertEquals(status, res.statusCode(), res.body());
        JsonNode json = JSON.readTree(res.body());
        assertEquals(code, json.get("code").asString(), res.body());
        assertTrue(json.hasNonNull("traceId"), res.body());
    }

    /** 200과 응답 모델 MockEventResult의 4개 필드 그대로(BN1) */
    private static JsonNode assertResult(HttpResponse<String> res, String eventId, String attemptId,
                                         String result) {
        assertEquals(200, res.statusCode(), res.body());
        JsonNode json = JSON.readTree(res.body());
        assertEquals(4, json.size(), res.body());
        assertEquals(eventId, json.get("eventId").asString());
        assertEquals(attemptId, json.get("paymentAttemptId").asString());
        assertEquals(result, json.get("result").asString());
        assertTrue(json.get("processedAt").asString().matches(TIME_PATTERN), res.body());
        return json;
    }

    // ---------- Y15 ----------

    @Test
    void Y15_DEFER_REQUESTED_시도에_APPROVED가_오면_200_PROCESSED이고_시도가_APPROVED이며_PaymentApproved가_한_번_난다()
            throws Exception {
        PaymentAttemptView attempt = deferredAttempt();
        assertEquals("REQUESTED", stored(attempt).status());
        String eventId = newEventId();

        HttpResponse<String> res = deliver(body(eventId, attempt, "APPROVED"));

        JsonNode json = assertResult(res, eventId, attempt.id(), "PROCESSED");
        PaymentAttemptView approved = stored(attempt);
        assertEquals("APPROVED", approved.status());
        assertNotNull(approved.completedAt());
        assertNull(approved.failureCode());
        assertEquals(attempt.id(), paymentService.attemptsOf(attempt.bookingId()).approvedAttemptId());
        assertEquals(1, committed.approvedOf(attempt.id()));
        assertEquals(0, committed.failedOf(attempt.id()));
        // 이벤트 기록 하나. processedAt이 응답과 같다
        MockPaymentEvent record = eventRepository.findByEventId(eventId).orElseThrow();
        assertEquals(MockEventResult.Result.PROCESSED, record.result());
        assertEquals(attempt.id(), record.paymentAttemptId().value());
        assertEquals(json.get("processedAt").asString(), ApiTime.format(record.processedAt()));
    }

    @Test
    void Y15_FAILED와_MOCK_DECLINED는_시도를_FAILED로_만들고_PaymentFailed가_한_번_난다() throws Exception {
        PaymentAttemptView attempt = deferredAttempt();
        String eventId = newEventId();

        HttpResponse<String> res = deliver(body(eventId, attempt, "FAILED"));

        assertResult(res, eventId, attempt.id(), "PROCESSED");
        PaymentAttemptView failed = stored(attempt);
        assertEquals("FAILED", failed.status());
        assertEquals(PaymentAttempt.MOCK_DECLINED, failed.failureCode());
        assertNotNull(failed.completedAt());
        assertNull(paymentService.attemptsOf(attempt.bookingId()).approvedAttemptId());
        assertEquals(1, committed.failedOf(attempt.id()));
        assertEquals(0, committed.approvedOf(attempt.id()));
        // 실패 뒤에는 2번이 열린다(I9의 해제). 예약 2차가 T16으로 본다
        assertEquals(2, paymentService.openAttempt(attempt.bookingId(), CHARGE, MockMode.DEFER).attemptNumber());
    }

    // ---------- Y16 ----------

    /** T21. 규칙 2와 3. 같은 이벤트 재전달과 다른 eventId의 같은 거래 같은 결과는 DUPLICATE다 */
    @Test
    void Y16_같은_eventId_같은_body_재전달은_200_DUPLICATE와_최초_processedAt이고_다른_eventId의_같은_결과도_DUPLICATE다()
            throws Exception {
        PaymentAttemptView attempt = deferredAttempt();
        String eventId = newEventId();
        Map<String, Object> approval = body(eventId, attempt, "APPROVED");
        JsonNode first = assertResult(deliver(approval), eventId, attempt.id(), "PROCESSED");
        PaymentAttemptView afterFirst = stored(attempt);

        JsonNode sameEvent = assertResult(deliver(approval), eventId, attempt.id(), "DUPLICATE");
        String otherEventId = newEventId();
        JsonNode sameTransaction = assertResult(deliver(body(otherEventId, attempt, "APPROVED")),
                otherEventId, attempt.id(), "DUPLICATE");

        assertEquals(first.get("processedAt"), sameEvent.get("processedAt"));
        assertEquals(first.get("processedAt"), sameTransaction.get("processedAt"));
        assertEquals(afterFirst, stored(attempt));
        assertEquals(1, committed.approvedOf(attempt.id()));
        // 규칙 2의 기록은 하나, 규칙 3의 다른 eventId는 DUPLICATE로 기록된다(재전달이 규칙 2로 답하게)
        assertEquals(MockEventResult.Result.PROCESSED, eventRepository.findByEventId(eventId).orElseThrow().result());
        assertEquals(MockEventResult.Result.DUPLICATE, eventRepository.findByEventId(otherEventId).orElseThrow().result());
    }

    @Test
    void Y16_실패_결과도_같은_거래_같은_결과의_재전달은_DUPLICATE이고_PaymentFailed는_한_번이다() throws Exception {
        PaymentAttemptView attempt = deferredAttempt();
        String eventId = newEventId();
        assertResult(deliver(body(eventId, attempt, "FAILED")), eventId, attempt.id(), "PROCESSED");

        assertResult(deliver(body(eventId, attempt, "FAILED")), eventId, attempt.id(), "DUPLICATE");
        String otherEventId = newEventId();
        assertResult(deliver(body(otherEventId, attempt, "FAILED")), otherEventId, attempt.id(), "DUPLICATE");

        assertEquals("FAILED", stored(attempt).status());
        assertEquals(1, committed.failedOf(attempt.id()));
    }

    // ---------- Y17 ----------

    /** 규칙 1. 없는 시도는 404이고 존재 검사가 거래 번호와 금액보다 앞이다(계약 2절 3행) */
    @Test
    void Y17_없는_시도는_404_RESOURCE_NOT_FOUND다() throws Exception {
        PaymentAttemptView attempt = deferredAttempt();
        Map<String, Object> unknown = with(body(newEventId(), attempt, "APPROVED"),
                "paymentAttemptId", "attempt_none");

        assertError(deliver(unknown), 404, "RESOURCE_NOT_FOUND");
        assertError(deliver(with(unknown, "amount", 1L)), 404, "RESOURCE_NOT_FOUND");

        assertEquals("REQUESTED", stored(attempt).status());
        // 통과 쪽 짝. 있는 시도는 같은 body로 200이다
        String eventId = newEventId();
        assertResult(deliver(body(eventId, attempt, "APPROVED")), eventId, attempt.id(), "PROCESSED");
    }

    /** 규칙 1. 다른 거래 번호는 409 MOCK_EVENT_CONFLICT이고 거래 번호 검사가 금액보다 앞이다(2절 5행과 6행) */
    @Test
    void Y17_다른_pgTransactionId는_409_MOCK_EVENT_CONFLICT이고_시도와_기록이_그대로다() throws Exception {
        PaymentAttemptView attempt = deferredAttempt();
        String eventId = newEventId();
        Map<String, Object> otherTransaction = with(body(eventId, attempt, "APPROVED"),
                "pgTransactionId", "mock_tx_other");

        assertError(deliver(otherTransaction), 409, "MOCK_EVENT_CONFLICT");
        assertError(deliver(with(otherTransaction, "amount", 1L)), 409, "MOCK_EVENT_CONFLICT");

        assertEquals("REQUESTED", stored(attempt).status());
        assertTrue(eventRepository.findByEventId(eventId).isEmpty());
        assertEquals(0, committed.approvedOf(attempt.id()));
    }

    /** 규칙 1. 금액 차이와 통화 차이는 409 PAYMENT_AMOUNT_MISMATCH다 */
    @Test
    void Y17_금액_차이와_통화_차이는_409_PAYMENT_AMOUNT_MISMATCH이고_시도와_기록이_그대로다() throws Exception {
        PaymentAttemptView attempt = deferredAttempt();
        String eventId = newEventId();
        Map<String, Object> approval = body(eventId, attempt, "APPROVED");

        assertError(deliver(with(approval, "amount", attempt.amount() + 1)), 409, "PAYMENT_AMOUNT_MISMATCH");
        assertError(deliver(with(approval, "amount", attempt.amount() - 1)), 409, "PAYMENT_AMOUNT_MISMATCH");
        assertError(deliver(with(approval, "currency", "USD")), 409, "PAYMENT_AMOUNT_MISMATCH");

        assertEquals("REQUESTED", stored(attempt).status());
        assertTrue(eventRepository.findByEventId(eventId).isEmpty());
        // 통과 쪽 짝. 같은 금액과 통화는 200이다
        assertResult(deliver(approval), eventId, attempt.id(), "PROCESSED");
    }

    /** 규칙 2. 같은 eventId 다른 body는 409 MOCK_EVENT_CONFLICT이고 최초 기록이 그대로다 */
    @Test
    void Y17_같은_eventId_다른_body는_409_MOCK_EVENT_CONFLICT이고_최초_기록이_그대로다() throws Exception {
        PaymentAttemptView attempt = deferredAttempt();
        String eventId = newEventId();
        Map<String, Object> approval = body(eventId, attempt, "APPROVED");
        assertResult(deliver(approval), eventId, attempt.id(), "PROCESSED");
        MockPaymentEvent before = eventRepository.findByEventId(eventId).orElseThrow();

        // 같은 eventId로 결과만 FAILED. 다른 시도를 가리키는 것도 다른 body다
        assertError(deliver(body(eventId, attempt, "FAILED")), 409, "MOCK_EVENT_CONFLICT");
        PaymentAttemptView other = deferredAttempt();
        assertError(deliver(body(eventId, other, "APPROVED")), 409, "MOCK_EVENT_CONFLICT");

        MockPaymentEvent after = eventRepository.findByEventId(eventId).orElseThrow();
        assertEquals(before.bodyHash(), after.bodyHash());
        assertEquals(before.processedAt(), after.processedAt());
        assertEquals("APPROVED", stored(attempt).status());
        assertEquals("REQUESTED", stored(other).status());
        assertEquals(1, committed.approvedOf(attempt.id()));
    }

    /** 규칙 4. 종착 시도에 반대 결과는 409 MOCK_EVENT_CONFLICT다(7절 D-4. 고아를 만들지 않는다) */
    @Test
    void Y17_APPROVED_시도에_FAILED와_FAILED_시도에_APPROVED는_409_MOCK_EVENT_CONFLICT이고_시도가_그대로다()
            throws Exception {
        PaymentAttemptView approved = deferredAttempt();
        String approvalId = newEventId();
        assertResult(deliver(body(approvalId, approved, "APPROVED")), approvalId, approved.id(), "PROCESSED");
        PaymentAttemptView failed = deferredAttempt();
        String failureId = newEventId();
        assertResult(deliver(body(failureId, failed, "FAILED")), failureId, failed.id(), "PROCESSED");

        String lateFailure = newEventId();
        assertError(deliver(body(lateFailure, approved, "FAILED")), 409, "MOCK_EVENT_CONFLICT");
        String lateApproval = newEventId();
        assertError(deliver(body(lateApproval, failed, "APPROVED")), 409, "MOCK_EVENT_CONFLICT");

        assertEquals("APPROVED", stored(approved).status());
        assertEquals("FAILED", stored(failed).status());
        assertTrue(eventRepository.findByEventId(lateFailure).isEmpty());
        assertTrue(eventRepository.findByEventId(lateApproval).isEmpty());
        assertEquals(1, committed.approvedOf(approved.id()));
        assertEquals(1, committed.failedOf(failed.id()));
        assertEquals(0, committed.failedOf(approved.id()));
        assertEquals(0, committed.approvedOf(failed.id()));
    }

    // ---------- Y18 ----------

    @Test
    void Y18_필수_누락과_길이와_outcome과_amount와_failureCode_조합과_미정의_필드는_400_INVALID_REQUEST다()
            throws Exception {
        PaymentAttemptView attempt = deferredAttempt();
        Map<String, Object> approval = body(newEventId(), attempt, "APPROVED");
        Map<String, Object> failure = body(newEventId(), attempt, "FAILED");

        assertError(deliver(with(approval, "eventId", null)), 400, "INVALID_REQUEST");
        assertError(deliver(with(approval, "eventId", "")), 400, "INVALID_REQUEST");
        assertError(deliver(with(approval, "eventId", "e".repeat(129))), 400, "INVALID_REQUEST");
        assertError(deliver(with(approval, "paymentAttemptId", "a".repeat(65))), 400, "INVALID_REQUEST");
        assertError(deliver(with(approval, "pgTransactionId", "")), 400, "INVALID_REQUEST");
        assertError(deliver(with(approval, "outcome", "DECLINED")), 400, "INVALID_REQUEST");
        assertError(deliver(with(approval, "amount", 0)), 400, "INVALID_REQUEST");
        assertError(deliver(with(approval, "amount", 30_000_000_001L)), 400, "INVALID_REQUEST");
        assertError(deliver(with(approval, "amount", "abc")), 400, "INVALID_REQUEST");
        assertError(deliver(with(approval, "currency", null)), 400, "INVALID_REQUEST");
        assertError(deliver(with(failure, "failureCode", null)), 400, "INVALID_REQUEST");
        assertError(deliver(with(approval, "failureCode", PaymentAttempt.MOCK_DECLINED)), 400, "INVALID_REQUEST");
        assertError(deliver(with(failure, "failureCode", "CARD_LIMIT")), 400, "INVALID_REQUEST");
        // 공통 절 38행. 명세에 없는 필드는 400이다
        assertError(deliver(with(approval, "note", "x")), 400, "INVALID_REQUEST");
        assertError(deliver(MOCK, "{"), 400, "INVALID_REQUEST");
        // 계약 2절 1행. body 형식이 행위자보다 앞이다
        assertError(deliver(null, JSON.writeValueAsString(with(approval, "outcome", "DECLINED"))),
                400, "INVALID_REQUEST");

        assertEquals("REQUESTED", stored(attempt).status());
        assertEquals(0, committed.approvedOf(attempt.id()));
    }

    /** T2. 경계의 통과 쪽. 128자 eventId와 64자 ID와 상한 금액은 형식을 지나 그다음 검사에서 답한다 */
    @Test
    void Y18_맞는_body는_통과하고_경계값은_형식을_지난다() throws Exception {
        PaymentAttemptView attempt = deferredAttempt();
        Map<String, Object> approval = body("e".repeat(128), attempt, "APPROVED");

        // 64자 ID는 형식을 지나 존재 검사(404)에서, 상한 금액은 대조(409)에서 답한다
        assertError(deliver(with(approval, "paymentAttemptId", "a".repeat(64))), 404, "RESOURCE_NOT_FOUND");
        assertError(deliver(with(approval, "pgTransactionId", "p".repeat(64))), 409, "MOCK_EVENT_CONFLICT");
        assertError(deliver(with(approval, "amount", 30_000_000_000L)), 409, "PAYMENT_AMOUNT_MISMATCH");
        assertError(deliver(with(approval, "amount", 1)), 409, "PAYMENT_AMOUNT_MISMATCH");

        assertResult(deliver(approval), "e".repeat(128), attempt.id(), "PROCESSED");
        PaymentAttemptView other = deferredAttempt();
        String failureId = newEventId();
        assertResult(deliver(body(failureId, other, "FAILED")), failureId, other.id(), "PROCESSED");
    }

    // ---------- Y19 ----------

    @Test
    void Y19_헤더_없음과_미등록은_401이고_guest와_host는_403이며_mock_001은_통과한다() throws Exception {
        PaymentAttemptView attempt = deferredAttempt();
        String eventId = newEventId();
        String body = JSON.writeValueAsString(body(eventId, attempt, "APPROVED"));

        assertError(deliver(null, body), 401, "ACTOR_REQUIRED");
        assertError(deliver("mock_999", body), 401, "ACTOR_REQUIRED");
        assertError(deliver("guest_001", body), 403, "ACCESS_DENIED");
        assertError(deliver("host_001", body), 403, "ACCESS_DENIED");
        assertError(deliver("operator_001", body), 403, "ACCESS_DENIED");
        assertEquals("REQUESTED", stored(attempt).status());
        assertTrue(eventRepository.findByEventId(eventId).isEmpty());

        assertResult(deliver(MOCK, body), eventId, attempt.id(), "PROCESSED");
    }

    // ---------- Y20 ----------

    /** T22의 결제 몫. 규칙 8. 환불된 시도에 같은 승인이 다시 와도 추가 환불도 새 이벤트도 없다 */
    @Test
    void Y20_환불된_시도에_같은_승인_이벤트가_다시_오면_200_DUPLICATE이고_환불이_하나_그대로다() throws Exception {
        PaymentAttemptView attempt = deferredAttempt();
        String eventId = newEventId();
        Map<String, Object> approval = body(eventId, attempt, "APPROVED");
        JsonNode first = assertResult(deliver(approval), eventId, attempt.id(), "PROCESSED");
        paymentService.refund(PaymentAttemptId.of(attempt.id()), RefundReason.BOOKING_CANCELED);
        RefundView refund = paymentService.attemptsOf(attempt.bookingId()).refund();
        assertNotNull(refund);
        assertEquals(1, committed.refundedOf(attempt.id()));

        JsonNode sameEvent = assertResult(deliver(approval), eventId, attempt.id(), "DUPLICATE");
        String otherEventId = newEventId();
        assertResult(deliver(body(otherEventId, attempt, "APPROVED")), otherEventId, attempt.id(), "DUPLICATE");

        assertEquals(first.get("processedAt"), sameEvent.get("processedAt"));
        PaymentSummaryView summary = paymentService.attemptsOf(attempt.bookingId());
        assertEquals(refund, summary.refund());
        assertEquals(attempt.id(), summary.approvedAttemptId());
        assertEquals("APPROVED", stored(attempt).status());
        assertEquals(1, committed.approvedOf(attempt.id()));
        assertEquals(1, committed.refundedOf(attempt.id()));
        // 환불된 시도에 실패 결과는 반대 결과라 409다(규칙 4)
        assertError(deliver(body(newEventId(), attempt, "FAILED")), 409, "MOCK_EVENT_CONFLICT");
    }

    // ---------- Y21 ----------

    /**
     * T23의 결제 몫. 규칙 7. 이벤트 기록까지 저장한 뒤 트랜잭션 안 구독자가 실패하면 부분 반영 없이
     * 롤백하고, 같은 이벤트 재전달은 DUPLICATE가 아니라 PROCESSED다(7절 D-2 가).
     */
    @Test
    void Y21_이벤트_기록_뒤_강제_실패로_롤백되면_기록과_시도_변경이_없고_재전달은_200_PROCESSED다() throws Exception {
        PaymentAttemptView attempt = deferredAttempt();
        String eventId = newEventId();
        Map<String, Object> approval = body(eventId, attempt, "APPROVED");
        policyFailure.armed.set(true);

        HttpResponse<String> failed = deliver(approval);

        assertEquals(500, failed.statusCode(), failed.body());
        assertFalse(policyFailure.armed.get());
        assertEquals("REQUESTED", stored(attempt).status());
        assertTrue(eventRepository.findByEventId(eventId).isEmpty());
        assertEquals(0, committed.approvedOf(attempt.id()));

        assertResult(deliver(approval), eventId, attempt.id(), "PROCESSED");
        assertEquals("APPROVED", stored(attempt).status());
        assertEquals(1, committed.approvedOf(attempt.id()));
    }

    // ---------- Y22 ----------

    /**
     * 규칙 2. 같은 이벤트 둘이 같은 순간 온다. 잠금이 규칙 2의 기록 조회보다 앞이라(7절 D-2) 둘째가
     * 첫째의 커밋을 본 뒤 DUPLICATE로 답한다. 기록은 그 eventId로 하나다.
     */
    @Test
    void Y22_잠금이_풀린_뒤_같은_이벤트_둘은_하나가_PROCESSED_하나가_DUPLICATE이고_PaymentApproved는_한_번이다()
            throws Exception {
        PaymentAttemptView attempt = deferredAttempt();
        String eventId = newEventId();
        String body = JSON.writeValueAsString(body(eventId, attempt, "APPROVED"));

        List<HttpResponse<String>> responses = race(attempt.bookingId(), body, body);

        assertProcessedAndDuplicate(responses, eventId, eventId, attempt.id());
        assertEquals(MockEventResult.Result.PROCESSED, eventRepository.findByEventId(eventId).orElseThrow().result());
        assertEquals("APPROVED", stored(attempt).status());
        assertEquals(1, committed.approvedOf(attempt.id()));
    }

    /** 규칙 3. 다른 eventId로 같은 거래 같은 결과 둘이 같은 순간 온다. 기록은 각자 하나씩이다 */
    @Test
    void Y22_잠금이_풀린_뒤_다른_eventId의_같은_결과_둘은_하나가_PROCESSED_하나가_DUPLICATE이고_기록은_각자_하나씩이다()
            throws Exception {
        PaymentAttemptView attempt = deferredAttempt();
        String eventA = newEventId();
        String eventB = newEventId();

        List<HttpResponse<String>> responses = race(attempt.bookingId(),
                JSON.writeValueAsString(body(eventA, attempt, "APPROVED")),
                JSON.writeValueAsString(body(eventB, attempt, "APPROVED")));

        assertProcessedAndDuplicate(responses, eventA, eventB, attempt.id());
        MockPaymentEvent recordA = eventRepository.findByEventId(eventA).orElseThrow();
        MockPaymentEvent recordB = eventRepository.findByEventId(eventB).orElseThrow();
        assertNotEquals(recordA.result(), recordB.result());
        assertEquals("APPROVED", stored(attempt).status());
        assertEquals(1, committed.approvedOf(attempt.id()));
    }

    /**
     * V14 방식. 테스트 트랜잭션이 Payment 행을 잠근 채 요청 둘을 보낸다. 잠금을 쥔 동안 어느 쪽도
     * 끝나면 잠금 없이 처리한 것이다. 풀린 뒤 둘의 응답을 돌려준다.
     */
    private List<HttpResponse<String>> race(String bookingId, String firstBody, String secondBody)
            throws Exception {
        ExecutorService racers = Executors.newFixedThreadPool(2);
        try {
            List<Future<HttpResponse<String>>> sent = new TransactionTemplate(transactionManager)
                    .execute(status -> {
                        paymentRepository.findByBookingIdForUpdate(bookingId).orElseThrow();
                        Future<HttpResponse<String>> a = racers.submit(() -> deliver(MOCK, firstBody));
                        Future<HttpResponse<String>> b = racers.submit(() -> deliver(MOCK, secondBody));
                        assertThrows(TimeoutException.class, () -> a.get(2, TimeUnit.SECONDS));
                        assertThrows(TimeoutException.class, () -> b.get(1, TimeUnit.SECONDS));
                        return List.of(a, b);
                    });
            return List.of(sent.get(0).get(30, TimeUnit.SECONDS), sent.get(1).get(30, TimeUnit.SECONDS));
        } finally {
            racers.shutdownNow();
        }
    }

    private static void assertProcessedAndDuplicate(List<HttpResponse<String>> responses,
                                                    String firstEventId, String secondEventId,
                                                    String attemptId) {
        JsonNode a = assertDoesNotThrow(() -> JSON.readTree(responses.get(0).body()));
        JsonNode b = assertDoesNotThrow(() -> JSON.readTree(responses.get(1).body()));
        assertEquals(200, responses.get(0).statusCode(), responses.get(0).body());
        assertEquals(200, responses.get(1).statusCode(), responses.get(1).body());
        assertEquals(firstEventId, a.get("eventId").asString());
        assertEquals(secondEventId, b.get("eventId").asString());
        assertEquals(attemptId, a.get("paymentAttemptId").asString());
        assertEquals(attemptId, b.get("paymentAttemptId").asString());
        List<String> results = List.of(a.get("result").asString(), b.get("result").asString());
        assertTrue(results.contains("PROCESSED") && results.contains("DUPLICATE"), results.toString());
        assertEquals(a.get("processedAt"), b.get("processedAt"));
    }

    // ---------- Y23 (dev 쪽) ----------

    /** T30. 기본 컨텍스트는 dev라 행위자 헤더와 /internal 경로가 산다. 밖은 DevProfileBoundaryTest */
    @Test
    void Y23_dev_컨텍스트에서는_HOST_전용_조회와_INTERNAL_01이_둘_다_산다() throws Exception {
        HttpResponse<String> hostOnly = send("GET", "/api/v1/host/properties", "host_001", null);
        assertEquals(200, hostOnly.statusCode(), hostOnly.body());

        PaymentAttemptView attempt = deferredAttempt();
        String eventId = newEventId();
        assertResult(deliver(body(eventId, attempt, "APPROVED")), eventId, attempt.id(), "PROCESSED");
    }
}
