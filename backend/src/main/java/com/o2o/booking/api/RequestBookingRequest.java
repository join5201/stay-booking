package com.o2o.booking.api;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import com.o2o.booking.application.RequestBookingCommand;
import com.o2o.booking.domain.IdempotencyKey;
import com.o2o.booking.domain.UserId;
import com.o2o.shared.RoomTypeId;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * BOOK-01 요청 본문. 설계 근거: 11 BOOK-01 요청 표. 여섯 필드 전부 필수다. 미정의 필드는
 * 전역 설정(fail-on-unknown-properties)이 400으로 막는다.
 *
 * 필드 이름은 API 이름(guestCount)이고 커맨드로 바꿀 때 도메인 이름(userCount)이 된다.
 * 날짜의 순서와 30박과 과거는 StayPeriod가 본다. 여기는 형식만이다(06-4 1-4).
 */
public record RequestBookingRequest(
        @NotBlank @Size(max = 64) String roomTypeId,
        @NotBlank String checkIn,
        @NotBlank String checkOut,
        @NotNull @Min(1) @Max(100) Integer guestCount,
        @NotNull @Min(1) @Max(30_000_000_000L) Long expectedTotalAmount,
        @NotBlank @Pattern(regexp = "KRW") String currency) {

    public RequestBookingCommand toCommand(UserId userId, IdempotencyKey idempotencyKey) {
        return new RequestBookingCommand(userId, RoomTypeId.of(roomTypeId),
                ApiFormat.parseDate("checkIn", checkIn), ApiFormat.parseDate("checkOut", checkOut),
                guestCount, expectedTotalAmount, currency, idempotencyKey);
    }

    /**
     * 멱등 규칙 2의 body 대조 값. JSON 키 순서와 무관해야 하므로 원문이 아니라 필드 값을 정한
     * 순서로 이어 붙인 정규형의 sha256이다. 값이 같으면 키 순서나 공백이 달라도 같은 body다.
     * 형식 검증을 통과한 뒤에 만들므로 거절된 body는 지문이 생기지 않는다.
     */
    public String fingerprint() {
        String canonical = String.join("\n", "roomTypeId=" + roomTypeId, "checkIn=" + checkIn,
                "checkOut=" + checkOut, "guestCount=" + guestCount,
                "expectedTotalAmount=" + expectedTotalAmount, "currency=" + currency);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256이 없다", e);
        }
    }
}
