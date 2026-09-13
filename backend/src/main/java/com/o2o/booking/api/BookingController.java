package com.o2o.booking.api;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.o2o.booking.application.BookingApplicationService;
import com.o2o.booking.application.BookingPaymentService;
import com.o2o.booking.application.IdempotentRequestExecutor;
import com.o2o.booking.application.IdempotentResult;
import com.o2o.booking.application.StoredResponse;
import com.o2o.booking.domain.Booking;
import com.o2o.booking.domain.BookingId;
import com.o2o.booking.domain.BookingStatus;
import com.o2o.booking.domain.IdempotencyKey;
import com.o2o.booking.domain.IdempotencyScope;
import com.o2o.booking.domain.UserId;
import com.o2o.payment.application.PaymentAttemptView;
import com.o2o.payment.application.PaymentSummaryView;
import com.o2o.shared.Actor;
import com.o2o.shared.ActorResolver;
import com.o2o.shared.ActorRole;
import com.o2o.shared.PageQuery;

import jakarta.validation.Valid;
import tools.jackson.databind.json.JsonMapper;

/**
 * 예약 API 다섯. 설계 근거: 11 예약과 결제 절의 BOOK-01, BOOK-02, BOOK-03, PAY-01, PAY-02와 공통
 * 헤더 표. 2차 계약 2절 PAY-01 표.
 *
 * BOOK-01과 PAY-01은 멱등 실행기로 감싼다. 이 컨트롤러가 앱 서비스를 부르고 응답 JSON을 만드는
 * 람다를 실행기에 넘기며, 실행기는 그 JSON을 멱등 기록에 저장했다가 재전송에 되돌린다. 재전송
 * 응답은 최초 시점의 스냅샷이다(11 멱등 처리 절 끝. 화면의 현재 상태는 GET으로 갱신한다). PAY-01의
 * 범위는 경로에 예약 ID가 들어가므로 다른 예약에 같은 키를 써도 다른 범위다(계약 2절 표 4행).
 *
 * 조회 셋(BOOK-02, BOOK-03, PAY-02)은 예약의 payment를 결제의 attemptsOf로 채운다. 목록은
 * 항목마다 한 번이다(2차 계약 6절 Booking 응답의 payment 채움 행). BOOK-01의 새 예약은 시도가
 * 있을 수 없어 빈 요약이다.
 *
 * 검사 순서는 계약 2절 표다. 행위자, 멱등키 형식, body 형식, 멱등 기록, 그다음 앱 서비스.
 * 단 body 형식은 프레임워크가 인자를 해석하며 먼저 보므로 행위자 없음과 body 오류가 겹치면
 * 400이 401보다 먼저 나간다. 재고와 카탈로그의 컨트롤러도 같다.
 *
 * serverNow는 shared의 UTC Clock에서 낸다. 프론트가 expiresAt과 serverNow로 남은 시간을
 * 계산한다(BOOK-03 처리 규칙).
 */
@RestController
public class BookingController {

    static final String BOOKINGS_PATH = "/api/v1/bookings";
    static final String PAYMENT_ATTEMPTS = "/payment-attempts";

    private final BookingApplicationService bookingApplicationService;
    private final BookingPaymentService bookingPaymentService;
    private final IdempotentRequestExecutor idempotentRequestExecutor;
    private final ActorResolver actorResolver;
    private final JsonMapper jsonMapper;
    private final Clock clock;

    public BookingController(BookingApplicationService bookingApplicationService,
                             BookingPaymentService bookingPaymentService,
                             IdempotentRequestExecutor idempotentRequestExecutor,
                             ActorResolver actorResolver, JsonMapper jsonMapper, Clock clock) {
        this.bookingApplicationService = bookingApplicationService;
        this.bookingPaymentService = bookingPaymentService;
        this.idempotentRequestExecutor = idempotentRequestExecutor;
        this.actorResolver = actorResolver;
        this.jsonMapper = jsonMapper;
        this.clock = clock;
    }

    /** BOOK-01 예약 요청. 201과 Location. 재전송이면 Idempotency-Replayed: true */
    @PostMapping(BOOKINGS_PATH)
    public ResponseEntity<String> request(
            @RequestHeader(value = "X-Dev-Actor-Id", required = false) String actorId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody RequestBookingRequest request) {
        Actor actor = actorResolver.require(actorId, ActorRole.GUEST);
        IdempotencyKey key = IdempotencyKey.of(idempotencyKey);
        IdempotencyScope scope = new IdempotencyScope(actor.id(), "POST", BOOKINGS_PATH, key);

        IdempotentResult result = idempotentRequestExecutor.execute(scope, request.fingerprint(),
                () -> {
                    Booking booking = bookingApplicationService.requestBooking(
                            request.toCommand(UserId.of(actor.id()), key));
                    BookingResponse body = BookingResponse.from(booking, PaymentSummaryView.empty(),
                            Instant.now(clock));
                    return new StoredResponse(HttpStatus.CREATED.value(),
                            BOOKINGS_PATH + "/" + booking.id().value(),
                            jsonMapper.writeValueAsString(body));
                });
        return toResponse(result);
    }

    /**
     * PAY-01 Mock 결제 요청. 202와 Location. 재전송이면 Idempotency-Replayed: true. body는 접수
     * 결과(REQUESTED)다. 결제 결과는 GET으로 본다(11 PAY-01 처리 규칙). body 없음은 빈 객체와
     * 같다(계약 2절 PAY-01 표 1행)
     */
    @PostMapping(BOOKINGS_PATH + "/{bookingId}" + PAYMENT_ATTEMPTS)
    public ResponseEntity<String> requestPayment(
            @RequestHeader(value = "X-Dev-Actor-Id", required = false) String actorId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @PathVariable String bookingId,
            @Valid @RequestBody(required = false) RequestPaymentRequest request) {
        RequestPaymentRequest body = request == null ? RequestPaymentRequest.empty() : request;
        Actor actor = actorResolver.require(actorId, ActorRole.GUEST);
        IdempotencyKey key = IdempotencyKey.of(idempotencyKey);
        String path = BOOKINGS_PATH + "/" + bookingId + PAYMENT_ATTEMPTS;
        IdempotencyScope scope = new IdempotencyScope(actor.id(), "POST", path, key);

        IdempotentResult result = idempotentRequestExecutor.execute(scope, body.fingerprint(),
                () -> {
                    PaymentAttemptView attempt = bookingPaymentService.requestPayment(
                            body.toCommand(UserId.of(actor.id()), BookingId.of(bookingId)));
                    return new StoredResponse(HttpStatus.ACCEPTED.value(), path,
                            jsonMapper.writeValueAsString(PaymentAttemptResponse.from(attempt)));
                });
        return toResponse(result);
    }

    /** PAY-02 결제 시도 목록. 소유자만 200이고 남의 예약은 404. attemptNumber 오름차순 */
    @GetMapping(BOOKINGS_PATH + "/{bookingId}" + PAYMENT_ATTEMPTS)
    public PaymentAttemptListResponse paymentAttempts(
            @RequestHeader(value = "X-Dev-Actor-Id", required = false) String actorId,
            @PathVariable String bookingId) {
        Actor actor = actorResolver.require(actorId, ActorRole.GUEST);
        PaymentSummaryView summary = bookingPaymentService.paymentAttempts(UserId.of(actor.id()),
                BookingId.of(bookingId));
        return PaymentAttemptListResponse.from(bookingId, summary);
    }

    /** 멱등 실행기의 결과를 HTTP로. 최초와 재전송이 같은 본문이고 재전송만 표시 헤더가 붙는다 */
    private static ResponseEntity<String> toResponse(IdempotentResult result) {
        StoredResponse stored = result.response();
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(stored.status())
                .contentType(MediaType.APPLICATION_JSON);
        if (stored.location() != null) {
            builder.location(URI.create(stored.location()));
        }
        if (result.replayed()) {
            builder.header("Idempotency-Replayed", "true");
        }
        return builder.body(stored.body());
    }

    /** BOOK-02 본인 예약 목록. status 생략이면 전체. 정렬은 리포지토리가 한다 */
    @GetMapping(BOOKINGS_PATH)
    public PageResponse<BookingResponse> list(
            @RequestHeader(value = "X-Dev-Actor-Id", required = false) String actorId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        Actor actor = actorResolver.require(actorId, ActorRole.GUEST);
        // 허용 밖 값은 IllegalArgumentException이고 공통 처리기가 400 INVALID_REQUEST로 낸다
        BookingStatus filter = status == null ? null : BookingStatus.valueOf(status);
        Instant serverNow = Instant.now(clock);
        return PageResponse.from(
                bookingApplicationService.findBookings(UserId.of(actor.id()), filter,
                        PageQuery.of(page, size)),
                (booking) -> BookingResponse.from(booking,
                        bookingPaymentService.paymentSummaryOf(booking.id()), serverNow));
    }

    /** BOOK-03 예약 상세. 소유자만 200이고 남의 예약은 404 */
    @GetMapping(BOOKINGS_PATH + "/{bookingId}")
    public BookingResponse get(
            @RequestHeader(value = "X-Dev-Actor-Id", required = false) String actorId,
            @PathVariable String bookingId) {
        Actor actor = actorResolver.require(actorId, ActorRole.GUEST);
        Booking booking = bookingApplicationService.getBooking(UserId.of(actor.id()),
                BookingId.of(bookingId));
        return BookingResponse.from(booking, bookingPaymentService.paymentSummaryOf(booking.id()),
                Instant.now(clock));
    }
}
