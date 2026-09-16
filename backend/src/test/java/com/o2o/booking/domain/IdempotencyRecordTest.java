package com.o2o.booking.domain;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * K6의 범위 몫. 설계 근거: task-S9-booking 8-1절, 11 명세 멱등 규칙 1(범위는 행위자 + 메서드 +
 * 경로 + 키), 규칙 3(최초 응답 보관), 규칙 4(진행 중), 규칙 6(완료는 본 트랜잭션), 계약 7절 D-1.
 * body 해시가 키 순서와 무관한 것은 api의 RequestBookingRequestTest가 본다.
 */
class IdempotencyRecordTest {

    private static final Instant NOW = Instant.parse("2026-10-01T20:00:00Z");
    private static final Instant LATER = Instant.parse("2026-10-01T20:00:01Z");
    private static final IdempotencyKey KEY = IdempotencyKey.of("booking-key-0001");
    private static final IdempotencyScope SCOPE =
            new IdempotencyScope("guest_001", "POST", "/api/v1/bookings", KEY);

    @Test
    void K6_범위는_넷이_전부_같아야_같다() {
        assertEquals(SCOPE, new IdempotencyScope("guest_001", "POST", "/api/v1/bookings", KEY));

        // 넷 중 하나라도 다르면 다른 범위다. 같은 키를 다른 손님, 다른 메서드, 다른 경로에 써도
        // 서로 간섭하지 않는다(규칙 1). 2차의 결제 요청은 예약마다 경로가 달라 범위가 갈린다
        assertNotEquals(SCOPE, new IdempotencyScope("guest_002", "POST", "/api/v1/bookings", KEY));
        assertNotEquals(SCOPE, new IdempotencyScope("guest_001", "DELETE", "/api/v1/bookings", KEY));
        assertNotEquals(SCOPE, new IdempotencyScope("guest_001", "POST",
                "/api/v1/bookings/booking_1/payments", KEY));
        assertNotEquals(SCOPE, new IdempotencyScope("guest_001", "POST", "/api/v1/bookings",
                IdempotencyKey.of("booking-key-0002")));
    }

    @Test
    void K6_범위의_빈_값은_거절한다() {
        assertThrows(IllegalArgumentException.class,
                () -> new IdempotencyScope("", "POST", "/api/v1/bookings", KEY));
        assertThrows(NullPointerException.class,
                () -> new IdempotencyScope("guest_001", "POST", "/api/v1/bookings", null));
    }

    @Test
    void K6_키_형식은_8자_이상_128자_이하의_영숫자와_하이픈과_밑줄이다() {
        // 경계 양쪽(T2). 8자와 128자는 통과, 7자와 129자는 거절. 허용 밖 문자도 거절
        assertEquals("a".repeat(8), IdempotencyKey.of("a".repeat(8)).value());
        assertEquals("a".repeat(128), IdempotencyKey.of("a".repeat(128)).value());
        assertEquals("Key_01-x", IdempotencyKey.of("Key_01-x").value());
        assertThrows(InvalidIdempotencyKeyException.class, () -> IdempotencyKey.of("a".repeat(7)));
        assertThrows(InvalidIdempotencyKeyException.class, () -> IdempotencyKey.of("a".repeat(129)));
        assertThrows(InvalidIdempotencyKeyException.class, () -> IdempotencyKey.of("key with space"));
        assertThrows(InvalidIdempotencyKeyException.class, () -> IdempotencyKey.of("키한글키한글키한글"));
        assertThrows(InvalidIdempotencyKeyException.class, () -> IdempotencyKey.of(null));
    }

    @Test
    void K6_진행_중_기록은_완료가_아니고_완료하면_최초_응답을_그대로_갖는다() {
        IdempotencyRecord record = IdempotencyRecord.begin(SCOPE, "hash-a", NOW);

        assertFalse(record.isCompleted());
        assertEquals(IdempotencyRecord.State.IN_PROGRESS, record.state());
        assertNull(record.responseStatus());
        assertEquals(SCOPE, record.scope());
        assertTrue(record.id().startsWith("idem_"));

        record.complete(201, "/api/v1/bookings/booking_1", "{\"id\":\"booking_1\"}", LATER);

        assertTrue(record.isCompleted());
        assertEquals(201, record.responseStatus());
        assertEquals("/api/v1/bookings/booking_1", record.responseLocation());
        assertEquals("{\"id\":\"booking_1\"}", record.responseBody());
        assertEquals(LATER, record.completedAt());
        assertEquals(NOW, record.createdAt());
    }

    @Test
    void K6_완료된_기록은_다시_완료할_수_없다() {
        // 규칙 3. 최초 응답이 정답이다. 두 번째 완료가 그것을 덮어쓰면 재전송이 다른 답을 받는다
        IdempotencyRecord record = IdempotencyRecord.begin(SCOPE, "hash-a", NOW);
        record.complete(201, "/api/v1/bookings/booking_1", "{}", LATER);

        assertThrows(IllegalStateException.class,
                () -> record.complete(201, "/api/v1/bookings/booking_2", "{}", LATER));

        assertEquals("/api/v1/bookings/booking_1", record.responseLocation());
    }

    @Test
    void K6_body_대조는_해시_문자열이_같은지다() {
        IdempotencyRecord record = IdempotencyRecord.begin(SCOPE, "hash-a", NOW);

        assertTrue(record.sameBody("hash-a"));
        assertFalse(record.sameBody("hash-b"));
        assertThrows(IllegalArgumentException.class, () -> IdempotencyRecord.begin(SCOPE, " ", NOW));
    }
}
