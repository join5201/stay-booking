package com.o2o.booking.api;

import org.junit.jupiter.api.Test;

import com.o2o.booking.application.RequestBookingCommand;
import com.o2o.booking.domain.IdempotencyKey;
import com.o2o.booking.domain.UserId;

import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * K6의 body 해시 몫. 설계 근거: task-S9-booking 8-1절, 11 명세 멱등 규칙 2(같은 키에 다른 body는
 * 409. 대조는 JSON 키 순서와 무관한 정규형), T11, 계약 6절 body 대조 행.
 *
 * JSON을 실제 JsonMapper로 읽는 이유는 컨트롤러가 받는 것이 그 결과이기 때문이다. 문자열 원문을
 * 해시하면 키 순서와 공백이 다른 같은 body가 다른 body로 판정된다.
 */
class RequestBookingRequestTest {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private static final String IN_ORDER = """
            {"roomTypeId":"room_1","checkIn":"2026-10-10","checkOut":"2026-10-12",
             "guestCount":2,"expectedTotalAmount":200000,"currency":"KRW"}
            """;

    private static final String REORDERED = """
            {"currency":"KRW","expectedTotalAmount":200000,"guestCount":2,
             "checkOut":"2026-10-12","checkIn":"2026-10-10","roomTypeId":"room_1"}
            """;

    private static RequestBookingRequest read(String json) {
        return MAPPER.readValue(json, RequestBookingRequest.class);
    }

    @Test
    void K6_키_순서와_공백이_달라도_같은_body다() {
        assertEquals(read(IN_ORDER).fingerprint(), read(REORDERED).fingerprint());
        assertEquals(64, read(IN_ORDER).fingerprint().length());
    }

    @Test
    void K6_값_하나가_다르면_다른_body다() {
        // 통과의 짝. 인원 2와 3, 총액 200000과 200001은 다른 요청이다(T11)
        String guestChanged = IN_ORDER.replace("\"guestCount\":2", "\"guestCount\":3");
        String amountChanged = IN_ORDER.replace("200000", "200001");

        assertNotEquals(read(IN_ORDER).fingerprint(), read(guestChanged).fingerprint());
        assertNotEquals(read(IN_ORDER).fingerprint(), read(amountChanged).fingerprint());
    }

    @Test
    void 커맨드로_바꾸면_API_이름이_도메인_이름이_된다() {
        RequestBookingCommand command = read(IN_ORDER).toCommand(
                UserId.of("guest_001"), IdempotencyKey.of("booking-key-0001"));

        assertEquals("room_1", command.roomTypeId().value());
        assertEquals("2026-10-10", command.checkIn().toString());
        assertEquals("2026-10-12", command.checkOut().toString());
        assertEquals(2, command.userCount());
        assertEquals(200_000L, command.expectedTotalAmount());
        assertEquals("guest_001", command.userId().value());
    }

    @Test
    void 날짜_형식이_틀리면_커맨드로_바꾸지_못한다() {
        // 형식은 이 층의 몫이고 순서와 30박과 과거는 StayPeriod의 몫이다(06-4 1-4)
        RequestBookingRequest broken = read(IN_ORDER.replace("2026-10-12", "2026/10/12"));

        assertThrows(InvalidDateFormatException.class,
                () -> broken.toCommand(UserId.of("guest_001"), IdempotencyKey.of("booking-key-0001")));
    }
}
